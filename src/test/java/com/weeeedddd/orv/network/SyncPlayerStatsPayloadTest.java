package com.weeeedddd.orv.network;

import com.weeeedddd.orv.OrvMod;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SyncPlayerStatsPayloadTest {
    @Test
    void usesTheDedicatedPlayerStatsId() {
        assertEquals(
                ResourceLocation.fromNamespaceAndPath(
                        OrvMod.MOD_ID,
                        "sync_player_stats"
                ),
                ModNetworking.SyncPlayerStatsPayload.TYPE.id()
        );
    }

    @Test
    void roundTripsStrength() {
        ModNetworking.SyncPlayerStatsPayload original =
                new ModNetworking.SyncPlayerStatsPayload(42);
        ByteBuf buffer = Unpooled.buffer();

        try {
            ModNetworking.SyncPlayerStatsPayload.STREAM_CODEC
                    .encode(buffer, original);
            ModNetworking.SyncPlayerStatsPayload decoded =
                    ModNetworking.SyncPlayerStatsPayload.STREAM_CODEC
                            .decode(buffer);

            assertEquals(original, decoded);
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsNegativeStrength() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ModNetworking.SyncPlayerStatsPayload(-1)
        );
    }
}
