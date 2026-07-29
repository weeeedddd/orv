package com.weeeedddd.orv.guild;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.UUIDUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent guild storage, attached to the overworld's data storage so it
 * survives restarts and is shared by every dimension.
 *
 * <p>Two indexes are kept: guilds by id, and a reverse index from player to
 * guild so membership lookups stay O(1). The reverse index is derived, so it
 * is rebuilt on load rather than serialised.
 */
public final class GuildStorage extends SavedData {

    private static final String FILE_ID = "orv_guilds";
    public static final int MAX_GUILD_NAME_LENGTH = 48;
    public static final int MAX_EMBLEM_LENGTH = 32;
    public static final String DEFAULT_EMBLEM = "star";

    private final Map<UUID, Guild> guilds = new LinkedHashMap<>();
    private final Map<UUID, UUID> guildByPlayer = new HashMap<>();

    /**
     * A stored guild.
     *
     * @param id      stable identifier
     * @param name    display name
     * @param emblem  emblem key, resolved to a sprite on the client
     * @param members membership with each member's rank
     */
    public record Guild(
            UUID id,
            String name,
            String emblem,
            Map<UUID, Member> members
    ) {
        public Guild {
            members = Map.copyOf(members);
        }

        public static final Codec<Guild> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        UUIDUtil.STRING_CODEC.fieldOf("id")
                                .forGetter(Guild::id),
                        Codec.STRING.fieldOf("name").forGetter(Guild::name),
                        Codec.STRING.optionalFieldOf("emblem", DEFAULT_EMBLEM)
                                .forGetter(Guild::emblem),
                        Codec.unboundedMap(
                                UUIDUtil.STRING_CODEC,
                                Member.CODEC
                        ).fieldOf("members").forGetter(Guild::members)
                ).apply(instance, Guild::new));

        public Guild withMembers(Map<UUID, Member> newMembers) {
            return new Guild(id, name, emblem, newMembers);
        }
    }

    /**
     * One membership row.
     *
     * @param lastKnownName cached name, refreshed whenever the player is seen
     * @param role          rank within the guild
     */
    public record Member(String lastKnownName, GuildRole role) {
        public static final Codec<Member> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("name")
                                .forGetter(Member::lastKnownName),
                        Codec.STRING.fieldOf("role").forGetter(
                                member -> member.role().name()
                        )
                ).apply(instance, (name, role) -> new Member(
                        name,
                        parseRole(role)
                )));

        private static GuildRole parseRole(String raw) {
            try {
                return GuildRole.valueOf(raw);
            } catch (IllegalArgumentException unknown) {
                return GuildRole.MEMBER;
            }
        }
    }

    private static final Codec<List<Guild>> GUILD_LIST_CODEC =
            Guild.CODEC.listOf();

    // ---------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------

    public static GuildStorage get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(factory(), FILE_ID);
    }

    private static SavedData.Factory<GuildStorage> factory() {
        return new SavedData.Factory<>(
                GuildStorage::new,
                GuildStorage::load,
                DataFixTypes.LEVEL
        );
    }

    private static GuildStorage load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        GuildStorage storage = new GuildStorage();
        GUILD_LIST_CODEC
                .parse(NbtOps.INSTANCE, tag.get("guilds"))
                .resultOrPartial(error -> {
                })
                .ifPresent(loaded -> {
                    for (Guild guild : loaded) {
                        storage.guilds.put(guild.id(), guild);
                        for (UUID memberId : guild.members().keySet()) {
                            storage.guildByPlayer.put(memberId, guild.id());
                        }
                    }
                });
        return storage;
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        GUILD_LIST_CODEC
                .encodeStart(NbtOps.INSTANCE, List.copyOf(guilds.values()))
                .resultOrPartial(error -> {
                })
                .ifPresent(encoded -> tag.put("guilds", (Tag) encoded));
        return tag;
    }

    // ---------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------

    public Optional<Guild> guildOf(UUID playerId) {
        UUID guildId = guildByPlayer.get(playerId);
        return guildId == null
                ? Optional.empty()
                : Optional.ofNullable(guilds.get(guildId));
    }

    public boolean isInGuild(UUID playerId) {
        return guildByPlayer.containsKey(playerId);
    }

    public Optional<GuildRole> roleOf(UUID playerId) {
        return guildOf(playerId)
                .map(guild -> guild.members().get(playerId))
                .map(Member::role);
    }

    // ---------------------------------------------------------------
    // Mutations
    // ---------------------------------------------------------------

    /** Creates a guild led by {@code leaderId}. Callers check requirements. */
    public Guild create(
            UUID leaderId,
            String leaderName,
            String name,
            String emblem
    ) {
        UUID guildId = UUID.randomUUID();
        Guild guild = new Guild(
                guildId,
                clamp(name, MAX_GUILD_NAME_LENGTH),
                clamp(emblem, MAX_EMBLEM_LENGTH),
                Map.of(leaderId, new Member(leaderName, GuildRole.LEADER))
        );

        guilds.put(guildId, guild);
        guildByPlayer.put(leaderId, guildId);
        setDirty();
        return guild;
    }

    public void putMember(UUID guildId, UUID playerId, Member member) {
        Guild guild = guilds.get(guildId);
        if (guild == null) {
            return;
        }
        Map<UUID, Member> members = new LinkedHashMap<>(guild.members());
        members.put(playerId, member);
        guilds.put(guildId, guild.withMembers(members));
        guildByPlayer.put(playerId, guildId);
        setDirty();
    }

    public void removeMember(UUID guildId, UUID playerId) {
        Guild guild = guilds.get(guildId);
        if (guild == null) {
            return;
        }
        Map<UUID, Member> members = new LinkedHashMap<>(guild.members());
        members.remove(playerId);
        guildByPlayer.remove(playerId);

        // A guild with nobody left is removed rather than kept as a husk.
        if (members.isEmpty()) {
            guilds.remove(guildId);
        } else {
            guilds.put(guildId, guild.withMembers(members));
        }
        setDirty();
    }

    private static String clamp(String raw, int maxLength) {
        String value = raw == null ? "" : raw.trim();
        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }
}
