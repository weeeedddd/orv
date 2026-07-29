package com.weeeedddd.orv.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerDataChannelTest {
    @Test
    void preservesMaxEnergyAndChannelAcrossAnNbtRoundTrip() {
        PlayerData original = new PlayerData(3, 30L, 250L, "#BIHYUNG-412");

        Tag encoded = PlayerData.CODEC
                .encodeStart(NbtOps.INSTANCE, original)
                .getOrThrow();
        PlayerData restored = PlayerData.CODEC
                .parse(NbtOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, restored);
    }

    @Test
    void readsPlayerDataSavedBeforeMaxEnergyAndChannelExisted() {
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putInt("strength_level", 5);
        legacyTag.putLong("energy", 12L);

        PlayerData restored = PlayerData.CODEC
                .parse(NbtOps.INSTANCE, legacyTag)
                .getOrThrow();

        assertEquals(5, restored.strengthLevel());
        assertEquals(12L, restored.energy());
        assertEquals(PlayerData.DEFAULT_MAX_ENERGY, restored.maxEnergy());
        assertEquals("", restored.channelId());
    }

    @Test
    void rejectsAMaxEnergyBelowOne() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerData(0, 0L, 0L, "")
        );
    }

    @Test
    void rejectsAnOverlongChannelId() {
        String tooLong = "x".repeat(PlayerData.MAX_CHANNEL_ID_LENGTH + 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerData(0, 0L, 100L, tooLong)
        );
    }

    @Test
    void withersKeepTheRemainingFields() {
        PlayerData original = new PlayerData(2, 40L, 200L, "#KIM-999");

        assertEquals(
                new PlayerData(2, 40L, 200L, "#YOO-001"),
                original.withChannelId("#YOO-001")
        );
        assertEquals(
                new PlayerData(2, 40L, 300L, "#KIM-999"),
                original.withMaxEnergy(300L)
        );
        assertEquals(
                new PlayerData(2, 55L, 200L, "#KIM-999"),
                original.withEnergy(55L)
        );
        assertEquals(
                new PlayerData(9, 40L, 200L, "#KIM-999"),
                original.withStrengthLevel(9)
        );
    }
}
