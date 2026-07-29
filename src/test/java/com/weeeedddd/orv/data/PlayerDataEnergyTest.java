package com.weeeedddd.orv.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerDataEnergyTest {
    @Test
    void preservesEnergyAcrossAnNbtRoundTrip() {
        PlayerData original = new PlayerData(4, 250L);

        Tag encoded = PlayerData.CODEC
                .encodeStart(NbtOps.INSTANCE, original)
                .getOrThrow();
        PlayerData restored = PlayerData.CODEC
                .parse(NbtOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, restored);
    }

    @Test
    void readsLegacyPlayerDataWithZeroEnergy() {
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putInt("strength_level", 8);

        PlayerData restored = PlayerData.CODEC
                .parse(NbtOps.INSTANCE, legacyTag)
                .getOrThrow();

        assertEquals(new PlayerData(8, 0L), restored);
    }

    @Test
    void changesEnergyWithoutLosingStrength() {
        PlayerData original = new PlayerData(7, 10L);

        assertEquals(
                new PlayerData(7, 80L),
                original.withEnergy(80L)
        );
        assertEquals(
                new PlayerData(11, 10L),
                original.withStrengthLevel(11)
        );
    }

    @Test
    void rejectsNegativeEnergy() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerData(0, -1L)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerData(0, 0L).withEnergy(-1L)
        );
    }
}
