package com.weeeedddd.orv.network;

import com.weeeedddd.orv.OrvMod;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SyncSystemDataPayloadTest {
    @Test
    void usesTheSystemSyncPayloadId() {
        assertEquals(
                ResourceLocation.fromNamespaceAndPath(
                        OrvMod.MOD_ID,
                        "system_sync"
                ),
                SyncSystemDataPayload.TYPE.id()
        );
    }

    @Test
    void roundTripsAllSystemFields() {
        SyncSystemDataPayload original = new SyncSystemDataPayload(
                UUID.fromString("4f6f5bb8-f871-45ae-881c-efc155f9b514"),
                12_345L,
                678L,
                "Demon-like Judge of Fire"
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(
                Unpooled.buffer()
        );

        try {
            SyncSystemDataPayload.STREAM_CODEC.encode(buffer, original);
            SyncSystemDataPayload decoded =
                    SyncSystemDataPayload.STREAM_CODEC.decode(buffer);

            assertEquals(original, decoded);
        } finally {
            buffer.release();
        }
    }

    @Test
    void supportsUnicodeConstellationNames() {
        SyncSystemDataPayload original = new SyncSystemDataPayload(
                UUID.randomUUID(),
                1L,
                2L,
                "深淵の黒炎竜"
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(
                Unpooled.buffer()
        );

        try {
            SyncSystemDataPayload.STREAM_CODEC.encode(buffer, original);
            SyncSystemDataPayload decoded =
                    SyncSystemDataPayload.STREAM_CODEC.decode(buffer);

            assertEquals(original.constellationName(),
                    decoded.constellationName());
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsInvalidSystemValues() {
        UUID playerId = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> new SyncSystemDataPayload(
                        playerId,
                        -1L,
                        0L,
                        ""
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SyncSystemDataPayload(
                        playerId,
                        0L,
                        -1L,
                        ""
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SyncSystemDataPayload(
                        playerId,
                        0L,
                        0L,
                        "x".repeat(
                                SyncSystemDataPayload
                                        .MAX_CONSTELLATION_NAME_LENGTH
                                        + 1
                        )
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new SyncSystemDataPayload(
                        null,
                        0L,
                        0L,
                        ""
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new SyncSystemDataPayload(
                        playerId,
                        0L,
                        0L,
                        null
                )
        );
    }
}
