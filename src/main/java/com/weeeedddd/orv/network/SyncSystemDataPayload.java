package com.weeeedddd.orv.network;

import com.weeeedddd.orv.OrvMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

/**
 * Server-authoritative system values rendered by the local player's HUD.
 */
public record SyncSystemDataPayload(
        UUID playerId,
        long coins,
        long energy,
        String constellationName
) implements CustomPacketPayload {
    public static final int MAX_CONSTELLATION_NAME_LENGTH = 64;

    public static final Type<SyncSystemDataPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    OrvMod.MOD_ID,
                    "system_sync"
            )
    );

    public static final StreamCodec<FriendlyByteBuf, SyncSystemDataPayload>
            STREAM_CODEC = new StreamCodec<>() {
                @Override
                public SyncSystemDataPayload decode(FriendlyByteBuf buffer) {
                    return new SyncSystemDataPayload(
                            buffer.readUUID(),
                            buffer.readVarLong(),
                            buffer.readVarLong(),
                            buffer.readUtf(
                                    MAX_CONSTELLATION_NAME_LENGTH
                            )
                    );
                }

                @Override
                public void encode(
                        FriendlyByteBuf buffer,
                        SyncSystemDataPayload payload
                ) {
                    buffer.writeUUID(payload.playerId());
                    buffer.writeVarLong(payload.coins());
                    buffer.writeVarLong(payload.energy());
                    buffer.writeUtf(
                            payload.constellationName(),
                            MAX_CONSTELLATION_NAME_LENGTH
                    );
                }
            };

    public SyncSystemDataPayload {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(
                constellationName,
                "constellationName"
        );

        if (coins < 0L) {
            throw new IllegalArgumentException(
                    "coins must not be negative"
            );
        }
        if (energy < 0L) {
            throw new IllegalArgumentException(
                    "energy must not be negative"
            );
        }
        if (constellationName.length()
                > MAX_CONSTELLATION_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "constellationName exceeds "
                            + MAX_CONSTELLATION_NAME_LENGTH
                            + " characters"
            );
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
