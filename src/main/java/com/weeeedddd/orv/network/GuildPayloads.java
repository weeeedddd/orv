package com.weeeedddd.orv.network;

import com.weeeedddd.orv.OrvMod;
import com.weeeedddd.orv.guild.GuildCreationCheck;
import com.weeeedddd.orv.guild.GuildRole;
import com.weeeedddd.orv.guild.GuildRoleAction;
import com.weeeedddd.orv.guild.GuildSnapshot;
import com.weeeedddd.orv.guild.GuildStorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GuildPayloads {
    private static final int MAX_GUILD_NAME_LENGTH = 48;
    private static final int MAX_PLAYER_NAME_LENGTH = 16;
    private static final int MAX_INVITE_NOTE_LENGTH = 160;
    private static final int MAX_MEMBERS = 256;
    private static final int MAX_ONLINE_PLAYERS = 1024;

    private GuildPayloads() {
    }

    public record RequestGuildDataPayload() implements CustomPacketPayload {
        public static final Type<RequestGuildDataPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        OrvMod.MOD_ID,
                        "request_guild_data"
                )
        );

        public static final StreamCodec<ByteBuf, RequestGuildDataPayload>
                STREAM_CODEC = StreamCodec.unit(
                        new RequestGuildDataPayload()
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record GuildScreenDataPayload(GuildSnapshot snapshot)
            implements CustomPacketPayload {
        public static final Type<GuildScreenDataPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        OrvMod.MOD_ID,
                        "guild_screen_data"
                )
        );

        public static final StreamCodec<
                RegistryFriendlyByteBuf,
                GuildScreenDataPayload
                > STREAM_CODEC = new StreamCodec<>() {
                    @Override
                    public GuildScreenDataPayload decode(
                            RegistryFriendlyByteBuf buffer
                    ) {
                        return new GuildScreenDataPayload(
                                readSnapshot(buffer)
                        );
                    }

                    @Override
                    public void encode(
                            RegistryFriendlyByteBuf buffer,
                            GuildScreenDataPayload payload
                    ) {
                        writeSnapshot(buffer, payload.snapshot());
                    }
                };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SendGuildInvitePayload(
            UUID targetPlayerId,
            String targetPlayerName,
            String note
    ) implements CustomPacketPayload {
        public static final Type<SendGuildInvitePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        OrvMod.MOD_ID,
                        "send_guild_invite"
                )
        );

        public static final StreamCodec<
                RegistryFriendlyByteBuf,
                SendGuildInvitePayload
                > STREAM_CODEC = new StreamCodec<>() {
                    @Override
                    public SendGuildInvitePayload decode(
                            RegistryFriendlyByteBuf buffer
                    ) {
                        return new SendGuildInvitePayload(
                                buffer.readUUID(),
                                buffer.readUtf(MAX_PLAYER_NAME_LENGTH),
                                buffer.readUtf(MAX_INVITE_NOTE_LENGTH)
                        );
                    }

                    @Override
                    public void encode(
                            RegistryFriendlyByteBuf buffer,
                            SendGuildInvitePayload payload
                    ) {
                        buffer.writeUUID(payload.targetPlayerId());
                        buffer.writeUtf(
                                payload.targetPlayerName(),
                                MAX_PLAYER_NAME_LENGTH
                        );
                        buffer.writeUtf(
                                payload.note(),
                                MAX_INVITE_NOTE_LENGTH
                        );
                    }
                };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record GuildRoleActionPayload(
            UUID targetPlayerId,
            GuildRoleAction action
    ) implements CustomPacketPayload {
        public static final Type<GuildRoleActionPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        OrvMod.MOD_ID,
                        "guild_role_action"
                )
        );

        public static final StreamCodec<
                RegistryFriendlyByteBuf,
                GuildRoleActionPayload
                > STREAM_CODEC = new StreamCodec<>() {
                    @Override
                    public GuildRoleActionPayload decode(
                            RegistryFriendlyByteBuf buffer
                    ) {
                        return new GuildRoleActionPayload(
                                buffer.readUUID(),
                                buffer.readEnum(GuildRoleAction.class)
                        );
                    }

                    @Override
                    public void encode(
                            RegistryFriendlyByteBuf buffer,
                            GuildRoleActionPayload payload
                    ) {
                        buffer.writeUUID(payload.targetPlayerId());
                        buffer.writeEnum(payload.action());
                    }
                };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static GuildSnapshot readSnapshot(
            RegistryFriendlyByteBuf buffer
    ) {
        String guildName = buffer.readUtf(MAX_GUILD_NAME_LENGTH);
        UUID viewerId = buffer.readUUID();
        GuildRole viewerRole = buffer.readEnum(GuildRole.class);

        int memberCount = readBoundedCount(buffer, MAX_MEMBERS);
        List<GuildSnapshot.Member> members =
                new ArrayList<>(memberCount);

        for (int index = 0; index < memberCount; index++) {
            members.add(new GuildSnapshot.Member(
                    buffer.readUUID(),
                    buffer.readUtf(MAX_PLAYER_NAME_LENGTH),
                    buffer.readEnum(GuildRole.class),
                    buffer.readBoolean()
            ));
        }

        int onlineCount = readBoundedCount(
                buffer,
                MAX_ONLINE_PLAYERS
        );
        List<GuildSnapshot.OnlinePlayer> onlinePlayers =
                new ArrayList<>(onlineCount);

        for (int index = 0; index < onlineCount; index++) {
            onlinePlayers.add(new GuildSnapshot.OnlinePlayer(
                    buffer.readUUID(),
                    buffer.readUtf(MAX_PLAYER_NAME_LENGTH),
                    buffer.readBoolean()
            ));
        }

        String emblem = buffer.readUtf(GuildStorage.MAX_EMBLEM_LENGTH);
        GuildCreationCheck creation = new GuildCreationCheck(
                buffer.readEnum(GuildCreationCheck.Status.class),
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readVarInt(),
                buffer.readVarLong()
        );

        return new GuildSnapshot(
                guildName,
                viewerId,
                viewerRole,
                members,
                onlinePlayers,
                emblem,
                creation
        );
    }

    private static void writeSnapshot(
            RegistryFriendlyByteBuf buffer,
            GuildSnapshot snapshot
    ) {
        buffer.writeUtf(snapshot.guildName(), MAX_GUILD_NAME_LENGTH);
        buffer.writeUUID(snapshot.viewerId());
        buffer.writeEnum(snapshot.viewerRole());

        writeBoundedCount(buffer, snapshot.members().size(), MAX_MEMBERS);
        for (GuildSnapshot.Member member : snapshot.members()) {
            buffer.writeUUID(member.playerId());
            buffer.writeUtf(
                    member.playerName(),
                    MAX_PLAYER_NAME_LENGTH
            );
            buffer.writeEnum(member.role());
            buffer.writeBoolean(member.online());
        }

        writeBoundedCount(
                buffer,
                snapshot.onlinePlayers().size(),
                MAX_ONLINE_PLAYERS
        );
        for (GuildSnapshot.OnlinePlayer player :
                snapshot.onlinePlayers()) {
            buffer.writeUUID(player.playerId());
            buffer.writeUtf(
                    player.playerName(),
                    MAX_PLAYER_NAME_LENGTH
            );
            buffer.writeBoolean(player.available());
        }

        buffer.writeUtf(snapshot.emblem(), GuildStorage.MAX_EMBLEM_LENGTH);
        GuildCreationCheck creation = snapshot.creation();
        buffer.writeEnum(creation.status());
        buffer.writeVarInt(creation.requiredLevel());
        buffer.writeVarLong(creation.requiredCoins());
        buffer.writeVarInt(creation.playerLevel());
        buffer.writeVarLong(creation.playerCoins());
    }

    private static int readBoundedCount(
            RegistryFriendlyByteBuf buffer,
            int maximum
    ) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException(
                    "Guild payload list size is out of bounds: " + count
            );
        }
        return count;
    }

    private static void writeBoundedCount(
            RegistryFriendlyByteBuf buffer,
            int count,
            int maximum
    ) {
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException(
                    "Guild payload list size is out of bounds: " + count
            );
        }
        buffer.writeVarInt(count);
    }
}
