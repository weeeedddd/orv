package com.weeeedddd.orv.guild;

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
    public static final int MAX_GUILD_NAME_LENGTH = 48;
    public static final int MAX_INVITE_NOTE_LENGTH = 160;

    private static final Map<UUID, GuildRecord> GUILDS = new HashMap<>();
    private static final Map<UUID, UUID> GUILD_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, PendingInvite> PENDING_INVITES = new HashMap<>();

    private GuildService() {
    }

    /**
     * Entry point for the future guild-creation command or menu.
     * Storage is intentionally in-memory until Guild SavedData is implemented.
     */
    public static boolean createGuild(ServerPlayer leader, String requestedName) {
        if (GUILD_BY_PLAYER.containsKey(leader.getUUID())) {
            return false;
        }

        String guildName = normalizeGuildName(requestedName);
        if (guildName.isBlank()) {
            return false;
        }

        UUID guildId = UUID.randomUUID();
        GuildRecord guild = new GuildRecord(guildId, guildName);
        guild.members.put(
                leader.getUUID(),
                new MemberRecord(
                        leader.getGameProfile().getName(),
                        GuildRole.LEADER
                )
        );

        GUILDS.put(guildId, guild);
        GUILD_BY_PLAYER.put(leader.getUUID(), guildId);
        return true;
    }

    public static GuildSnapshot snapshotFor(ServerPlayer viewer) {
        MinecraftServer server = viewer.getServer();
        UUID guildId = GUILD_BY_PLAYER.get(viewer.getUUID());
        GuildRecord guild = guildId == null ? null : GUILDS.get(guildId);

        List<GuildSnapshot.OnlinePlayer> onlinePlayers =
                buildOnlinePlayerList(server, viewer);

        if (guild == null) {
            return new GuildSnapshot(
                    "NO GUILD",
                    viewer.getUUID(),
                    GuildRole.NONE,
                    List.of(),
                    onlinePlayers
            );
        }

        MemberRecord viewerMember = guild.members.get(viewer.getUUID());
        GuildRole viewerRole = viewerMember == null
                ? GuildRole.NONE
                : viewerMember.role();

        List<GuildSnapshot.Member> members = new ArrayList<>();
        for (Map.Entry<UUID, MemberRecord> entry : guild.members.entrySet()) {
            UUID playerId = entry.getKey();
            MemberRecord member = entry.getValue();
            ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(playerId);

            if (onlinePlayer != null) {
                member = new MemberRecord(
                        onlinePlayer.getGameProfile().getName(),
                        member.role()
                );
                guild.members.put(playerId, member);
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
                guild.name,
                viewer.getUUID(),
                viewerRole,
                members,
                onlinePlayers
        );
    }

    public static boolean sendInvite(
            ServerPlayer sender,
            UUID targetPlayerId,
            String targetPlayerName,
            String rawNote
    ) {
        UUID guildId = GUILD_BY_PLAYER.get(sender.getUUID());
        GuildRecord guild = guildId == null ? null : GUILDS.get(guildId);

        if (guild == null) {
            sender.sendSystemMessage(Component.literal(
                    "You must belong to a guild before sending invitations."
            ));
            return false;
        }

        MemberRecord senderMember = guild.members.get(sender.getUUID());
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

        if (GUILD_BY_PLAYER.containsKey(targetPlayerId)) {
            sender.sendSystemMessage(Component.literal(
                    "That player already belongs to a guild."
            ));
            return false;
        }

        String note = normalizeNote(rawNote);
        PENDING_INVITES.put(
                targetPlayerId,
                new PendingInvite(guildId, sender.getUUID(), note)
        );

        sender.sendSystemMessage(Component.literal(
                "Guild invitation sent to " + targetPlayerName + "."
        ));
        target.sendSystemMessage(Component.literal(
                sender.getGameProfile().getName()
                        + " invited you to join "
                        + guild.name
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
        UUID guildId = GUILD_BY_PLAYER.get(actor.getUUID());
        GuildRecord guild = guildId == null ? null : GUILDS.get(guildId);

        if (guild == null || actor.getUUID().equals(targetPlayerId)) {
            return false;
        }

        MemberRecord actorMember = guild.members.get(actor.getUUID());
        MemberRecord targetMember = guild.members.get(targetPlayerId);
        if (actorMember == null || targetMember == null) {
            return false;
        }

        GuildRole actorRole = actorMember.role();
        GuildRole targetRole = targetMember.role();

        boolean changed = switch (action) {
            case PROMOTE -> promote(guild, targetPlayerId, actorRole, targetMember);
            case DEMOTE -> demote(guild, targetPlayerId, actorRole, targetMember);
            case KICK -> kick(guild, targetPlayerId, actorRole, targetRole);
            case TRANSFER_LEADERSHIP -> transferLeadership(
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
        UUID guildId = GUILD_BY_PLAYER.get(memberId);
        GuildRecord guild = guildId == null ? null : GUILDS.get(guildId);
        if (guild == null) {
            return List.of();
        }

        List<ServerPlayer> onlineMembers = new ArrayList<>();
        for (UUID playerId : guild.members.keySet()) {
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
                        !GUILD_BY_PLAYER.containsKey(player.getUUID())
                                && !PENDING_INVITES.containsKey(player.getUUID())
                ))
                .sorted(Comparator.comparing(
                        GuildSnapshot.OnlinePlayer::playerName,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();
    }

    private static boolean promote(
            GuildRecord guild,
            UUID targetId,
            GuildRole actorRole,
            MemberRecord target
    ) {
        if (!actorRole.canPromote(target.role())) {
            return false;
        }
        guild.members.put(
                targetId,
                new MemberRecord(target.lastKnownName(), GuildRole.VICE_LEADER)
        );
        return true;
    }

    private static boolean demote(
            GuildRecord guild,
            UUID targetId,
            GuildRole actorRole,
            MemberRecord target
    ) {
        if (!actorRole.canDemote(target.role())) {
            return false;
        }
        guild.members.put(
                targetId,
                new MemberRecord(target.lastKnownName(), GuildRole.MEMBER)
        );
        return true;
    }

    private static boolean kick(
            GuildRecord guild,
            UUID targetId,
            GuildRole actorRole,
            GuildRole targetRole
    ) {
        if (!actorRole.canKick(targetRole)) {
            return false;
        }
        guild.members.remove(targetId);
        GUILD_BY_PLAYER.remove(targetId);
        return true;
    }

    private static boolean transferLeadership(
            GuildRecord guild,
            UUID actorId,
            UUID targetId,
            MemberRecord actor,
            MemberRecord target
    ) {
        if (!actor.role().canTransferLeadership(target.role())) {
            return false;
        }

        guild.members.put(
                actorId,
                new MemberRecord(actor.lastKnownName(), GuildRole.VICE_LEADER)
        );
        guild.members.put(
                targetId,
                new MemberRecord(target.lastKnownName(), GuildRole.LEADER)
        );
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

    private static final class GuildRecord {
        private final UUID id;
        private final String name;
        private final Map<UUID, MemberRecord> members = new HashMap<>();

        private GuildRecord(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private record MemberRecord(String lastKnownName, GuildRole role) {
    }

    private record PendingInvite(
            UUID guildId,
            UUID inviterId,
            String note
    ) {
    }
}
