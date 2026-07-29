package com.weeeedddd.orv.guild;

import com.weeeedddd.orv.data.ModAttachments;
import com.weeeedddd.orv.economy.CoinService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GuildService {
    public static final int MAX_GUILD_NAME_LENGTH =
            GuildStorage.MAX_GUILD_NAME_LENGTH;
    public static final int MAX_INVITE_NOTE_LENGTH = 160;

    // Membership lives in GuildStorage so it survives a restart. Pending
    // invites are deliberately transient: they expire with the session.
    private static final Map<UUID, PendingInvite> PENDING_INVITES = new HashMap<>();

    private GuildService() {
    }

    /**
     * Tests whether {@code player} may found a guild right now. Pass
     * {@code null} for {@code requestedName} to ask about eligibility alone,
     * which is what the client does to decide whether to grey the button.
     */
    public static GuildCreationCheck checkCreation(
            ServerPlayer player,
            String requestedName
    ) {
        GuildStorage storage = GuildStorage.get(player.getServer());
        return GuildCreationCheck.evaluate(
                storage.isInGuild(player.getUUID()),
                ModAttachments.getStrengthLevel(player),
                CoinService.getCoins(player),
                requestedName == null
                        ? null
                        : normalizeGuildName(requestedName)
        );
    }

    /**
     * Founds a guild once every requirement is met, charging the configured
     * cost. Returns the check so the caller can report the exact reason on
     * failure; nothing is mutated unless the check passes.
     */
    public static GuildCreationCheck createGuild(
            ServerPlayer leader,
            String requestedName,
            String emblem
    ) {
        GuildCreationCheck check = checkCreation(leader, requestedName);
        if (!check.allowed()) {
            leader.sendSystemMessage(Component.literal(check.describe()));
            return check;
        }

        // Charge before creating: if the debit fails the guild is not made.
        if (!CoinService.removeCoins(leader, check.requiredCoins())) {
            leader.sendSystemMessage(Component.literal(
                    "Coin transaction failed; the guild was not created."
            ));
            return new GuildCreationCheck(
                    GuildCreationCheck.Status.NOT_ENOUGH_COINS,
                    check.requiredLevel(),
                    check.requiredCoins(),
                    check.playerLevel(),
                    CoinService.getCoins(leader)
            );
        }

        GuildStorage storage = GuildStorage.get(leader.getServer());
        GuildStorage.Guild guild = storage.create(
                leader.getUUID(),
                leader.getGameProfile().getName(),
                normalizeGuildName(requestedName),
                emblem
        );

        leader.sendSystemMessage(Component.literal(
                "Guild \"" + guild.name() + "\" founded for "
                        + check.requiredCoins() + " coins."
        ));
        return check;
    }

    public static GuildSnapshot snapshotFor(ServerPlayer viewer) {
        MinecraftServer server = viewer.getServer();
        GuildStorage storage = GuildStorage.get(server);
        GuildStorage.Guild guild = storage.guildOf(viewer.getUUID())
                .orElse(null);

        List<GuildSnapshot.OnlinePlayer> onlinePlayers =
                buildOnlinePlayerList(server, viewer);
        GuildCreationCheck creation = checkCreation(viewer, null);

        if (guild == null) {
            return new GuildSnapshot(
                    "NO GUILD",
                    viewer.getUUID(),
                    GuildRole.NONE,
                    List.of(),
                    onlinePlayers,
                    "",
                    creation
            );
        }

        GuildStorage.Member viewerMember =
                guild.members().get(viewer.getUUID());
        GuildRole viewerRole = viewerMember == null
                ? GuildRole.NONE
                : viewerMember.role();

        List<GuildSnapshot.Member> members = new ArrayList<>();
        for (Map.Entry<UUID, GuildStorage.Member> entry
                : guild.members().entrySet()) {
            UUID playerId = entry.getKey();
            GuildStorage.Member member = entry.getValue();
            ServerPlayer onlinePlayer =
                    server.getPlayerList().getPlayer(playerId);

            if (onlinePlayer != null
                    && !onlinePlayer.getGameProfile().getName()
                            .equals(member.lastKnownName())) {
                member = new GuildStorage.Member(
                        onlinePlayer.getGameProfile().getName(),
                        member.role()
                );
                storage.putMember(guild.id(), playerId, member);
            }

            members.add(new GuildSnapshot.Member(
                    playerId,
                    member.lastKnownName(),
                    member.role(),
                    onlinePlayer != null
            ));
        }

        members.sort(
                Comparator.comparingInt(
                                (GuildSnapshot.Member member) ->
                                        member.role().authorityLevel()
                        )
                        .reversed()
                        .thenComparing(
                                GuildSnapshot.Member::playerName,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        return new GuildSnapshot(
                guild.name(),
                viewer.getUUID(),
                viewerRole,
                members,
                onlinePlayers,
                guild.emblem(),
                creation
        );
    }

    public static boolean sendInvite(
            ServerPlayer sender,
            UUID targetPlayerId,
            String targetPlayerName,
            String rawNote
    ) {
        GuildStorage storage = GuildStorage.get(sender.getServer());
        GuildStorage.Guild guild = storage.guildOf(sender.getUUID())
                .orElse(null);

        if (guild == null) {
            sender.sendSystemMessage(Component.literal(
                    "You must belong to a guild before sending invitations."
            ));
            return false;
        }

        GuildStorage.Member senderMember =
                guild.members().get(sender.getUUID());
        if (senderMember == null || !senderMember.role().canInvite()) {
            sender.sendSystemMessage(Component.literal(
                    "Your guild role cannot send invitations."
            ));
            return false;
        }

        if (sender.getUUID().equals(targetPlayerId)) {
            return false;
        }

        ServerPlayer target = sender.getServer()
                .getPlayerList()
                .getPlayer(targetPlayerId);

        if (target == null
                || !target.getGameProfile().getName().equals(targetPlayerName)) {
            sender.sendSystemMessage(Component.literal(
                    "The selected player is no longer online."
            ));
            return false;
        }

        if (storage.isInGuild(targetPlayerId)) {
            sender.sendSystemMessage(Component.literal(
                    "That player already belongs to a guild."
            ));
            return false;
        }

        String note = normalizeNote(rawNote);
        PENDING_INVITES.put(
                targetPlayerId,
                new PendingInvite(guild.id(), sender.getUUID(), note)
        );

        sender.sendSystemMessage(Component.literal(
                "Guild invitation sent to " + targetPlayerName + "."
        ));
        target.sendSystemMessage(Component.literal(
                sender.getGameProfile().getName()
                        + " invited you to join "
                        + guild.name()
                        + "."
        ));

        if (!note.isBlank()) {
            target.sendSystemMessage(Component.literal("Note: " + note));
        }

        return true;
    }

    public static boolean applyRoleAction(
            ServerPlayer actor,
            UUID targetPlayerId,
            GuildRoleAction action
    ) {
        GuildStorage storage = GuildStorage.get(actor.getServer());
        GuildStorage.Guild guild = storage.guildOf(actor.getUUID())
                .orElse(null);

        if (guild == null || actor.getUUID().equals(targetPlayerId)) {
            return false;
        }

        GuildStorage.Member actorMember =
                guild.members().get(actor.getUUID());
        GuildStorage.Member targetMember =
                guild.members().get(targetPlayerId);
        if (actorMember == null || targetMember == null) {
            return false;
        }

        GuildRole actorRole = actorMember.role();
        GuildRole targetRole = targetMember.role();

        boolean changed = switch (action) {
            case PROMOTE -> promote(
                    storage, guild, targetPlayerId, actorRole, targetMember);
            case DEMOTE -> demote(
                    storage, guild, targetPlayerId, actorRole, targetMember);
            case KICK -> kick(
                    storage, guild, targetPlayerId, actorRole, targetRole);
            case TRANSFER_LEADERSHIP -> transferLeadership(
                    storage,
                    guild,
                    actor.getUUID(),
                    targetPlayerId,
                    actorMember,
                    targetMember
            );
        };

        if (changed) {
            actor.sendSystemMessage(Component.literal(
                    "Guild hierarchy updated."
            ));
        } else {
            actor.sendSystemMessage(Component.literal(
                    "You are not allowed to perform that guild action."
            ));
        }

        return changed;
    }

    public static List<ServerPlayer> onlineGuildMembers(
            MinecraftServer server,
            UUID memberId
    ) {
        GuildStorage.Guild guild = GuildStorage.get(server)
                .guildOf(memberId)
                .orElse(null);
        if (guild == null) {
            return List.of();
        }

        List<ServerPlayer> onlineMembers = new ArrayList<>();
        for (UUID playerId : guild.members().keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                onlineMembers.add(player);
            }
        }
        return onlineMembers;
    }

    private static List<GuildSnapshot.OnlinePlayer> buildOnlinePlayerList(
            MinecraftServer server,
            ServerPlayer viewer
    ) {
        return server.getPlayerList()
                .getPlayers()
                .stream()
                .filter(player -> !player.getUUID().equals(viewer.getUUID()))
                .map(player -> new GuildSnapshot.OnlinePlayer(
                        player.getUUID(),
                        player.getGameProfile().getName(),
                        !GuildStorage.get(server).isInGuild(player.getUUID())
                                && !PENDING_INVITES.containsKey(player.getUUID())
                ))
                .sorted(Comparator.comparing(
                        GuildSnapshot.OnlinePlayer::playerName,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();
    }

    private static boolean promote(
            GuildStorage storage,
            GuildStorage.Guild guild,
            UUID targetId,
            GuildRole actorRole,
            GuildStorage.Member target
    ) {
        if (!actorRole.canPromote(target.role())) {
            return false;
        }
        storage.putMember(guild.id(), targetId, new GuildStorage.Member(
                target.lastKnownName(),
                GuildRole.VICE_LEADER
        ));
        return true;
    }

    private static boolean demote(
            GuildStorage storage,
            GuildStorage.Guild guild,
            UUID targetId,
            GuildRole actorRole,
            GuildStorage.Member target
    ) {
        if (!actorRole.canDemote(target.role())) {
            return false;
        }
        storage.putMember(guild.id(), targetId, new GuildStorage.Member(
                target.lastKnownName(),
                GuildRole.MEMBER
        ));
        return true;
    }

    private static boolean kick(
            GuildStorage storage,
            GuildStorage.Guild guild,
            UUID targetId,
            GuildRole actorRole,
            GuildRole targetRole
    ) {
        if (!actorRole.canKick(targetRole)) {
            return false;
        }
        storage.removeMember(guild.id(), targetId);
        return true;
    }

    private static boolean transferLeadership(
            GuildStorage storage,
            GuildStorage.Guild guild,
            UUID actorId,
            UUID targetId,
            GuildStorage.Member actor,
            GuildStorage.Member target
    ) {
        if (!actor.role().canTransferLeadership(target.role())) {
            return false;
        }

        storage.putMember(guild.id(), actorId, new GuildStorage.Member(
                actor.lastKnownName(),
                GuildRole.VICE_LEADER
        ));
        storage.putMember(guild.id(), targetId, new GuildStorage.Member(
                target.lastKnownName(),
                GuildRole.LEADER
        ));
        return true;
    }

    private static String normalizeGuildName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        return name.length() <= MAX_GUILD_NAME_LENGTH
                ? name
                : name.substring(0, MAX_GUILD_NAME_LENGTH);
    }

    private static String normalizeNote(String rawNote) {
        String note = rawNote == null ? "" : rawNote.trim();
        return note.length() <= MAX_INVITE_NOTE_LENGTH
                ? note
                : note.substring(0, MAX_INVITE_NOTE_LENGTH);
    }

    private record PendingInvite(
            UUID guildId,
            UUID inviterId,
            String note
    ) {
    }
}
