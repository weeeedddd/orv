package com.weeeedddd.orv.network;

import com.weeeedddd.orv.OrvMod;
import com.weeeedddd.orv.guild.GuildStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Serverbound request to found a guild. The server re-runs the full
 * requirement check; the client's greyed-out button is a courtesy, not the
 * authority.
 */
public record CreateGuildPayload(String guildName, String emblem)
        implements CustomPacketPayload {

    public static final Type<CreateGuildPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    OrvMod.MOD_ID,
                    "create_guild"
            )
    );

    public CreateGuildPayload {
        Objects.requireNonNull(guildName, "guildName");
        Objects.requireNonNull(emblem, "emblem");
    }

    public static final StreamCodec<FriendlyByteBuf, CreateGuildPayload>
            STREAM_CODEC = new StreamCodec<>() {
                @Override
                public CreateGuildPayload decode(FriendlyByteBuf buffer) {
                    return new CreateGuildPayload(
                            buffer.readUtf(
                                    GuildStorage.MAX_GUILD_NAME_LENGTH
                            ),
                            buffer.readUtf(GuildStorage.MAX_EMBLEM_LENGTH)
                    );
                }

                @Override
                public void encode(
                        FriendlyByteBuf buffer,
                        CreateGuildPayload payload
                ) {
                    buffer.writeUtf(
                            payload.guildName(),
                            GuildStorage.MAX_GUILD_NAME_LENGTH
                    );
                    buffer.writeUtf(
                            payload.emblem(),
                            GuildStorage.MAX_EMBLEM_LENGTH
                    );
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
