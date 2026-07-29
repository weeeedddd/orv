package com.weeeedddd.orv.sponsor;

import com.weeeedddd.orv.OrvMod;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSponsorTest {
    private static final ResourceLocation RECOVERY =
            ResourceLocation.fromNamespaceAndPath(
                    OrvMod.MOD_ID,
                    "recovery"
            );

    @Test
    void exposesAnEmptyDefaultSponsorState() {
        IPlayerSponsor sponsor = PlayerSponsor.DEFAULT;

        assertEquals("", sponsor.constellationName());
        assertEquals(Set.of(), sponsor.activeStigmas());
        assertEquals(0L, sponsor.probability());
        assertEquals(Map.of(), sponsor.cooldowns());
        assertFalse(sponsor.hasActiveStigma(RECOVERY));
    }

    @Test
    void createsImmutableUpdatedSponsorSnapshots() {
        IPlayerSponsor original = PlayerSponsor.DEFAULT;

        IPlayerSponsor updated = original
                .withConstellationName("Secretive Plotter")
                .withActiveStigma(RECOVERY)
                .withProbability(900L)
                .withCooldownUntil(RECOVERY, 1_200L);

        assertEquals("", original.constellationName());
        assertFalse(original.hasActiveStigma(RECOVERY));
        assertEquals("Secretive Plotter",
                updated.constellationName());
        assertTrue(updated.hasActiveStigma(RECOVERY));
        assertEquals(900L, updated.probability());
        assertEquals(200L,
                updated.cooldownRemaining(RECOVERY, 1_000L));
        assertEquals(0L,
                updated.cooldownRemaining(RECOVERY, 1_200L));
        assertThrows(
                UnsupportedOperationException.class,
                () -> updated.activeStigmas().clear()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> updated.cooldowns().clear()
        );
    }

    @Test
    void removesAnActiveStigmaWithoutChangingOtherState() {
        ResourceLocation second =
                ResourceLocation.fromNamespaceAndPath(
                        OrvMod.MOD_ID,
                        "second"
                );
        IPlayerSponsor original = new PlayerSponsor(
                "Abyssal Black Flame Dragon",
                Set.of(RECOVERY, second),
                50L,
                Map.of()
        );

        IPlayerSponsor updated =
                original.withoutActiveStigma(RECOVERY);

        assertFalse(updated.hasActiveStigma(RECOVERY));
        assertTrue(updated.hasActiveStigma(second));
        assertEquals(original.constellationName(),
                updated.constellationName());
        assertEquals(original.probability(),
                updated.probability());
    }

    @Test
    void persistsEverySponsorFieldThroughNbt() {
        PlayerSponsor original = new PlayerSponsor(
                "Demon-like Judge of Fire",
                Set.of(RECOVERY),
                4_500L,
                Map.of(RECOVERY, 9_999L)
        );

        Tag encoded = PlayerSponsor.CODEC
                .encodeStart(NbtOps.INSTANCE, original)
                .getOrThrow();
        PlayerSponsor restored = PlayerSponsor.CODEC
                .parse(NbtOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, restored);
    }

    @Test
    void decodesLegacyEmptySponsorData() {
        PlayerSponsor restored = PlayerSponsor.CODEC
                .parse(
                        NbtOps.INSTANCE,
                        new net.minecraft.nbt.CompoundTag()
                )
                .getOrThrow();

        assertEquals(PlayerSponsor.DEFAULT, restored);
    }

    @Test
    void rejectsInvalidSponsorState() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerSponsor(
                        "x".repeat(
                                PlayerSponsor
                                        .MAX_CONSTELLATION_NAME_LENGTH
                                        + 1
                        ),
                        Set.of(),
                        0L,
                        Map.of()
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerSponsor(
                        "",
                        Set.of(),
                        -1L,
                        Map.of()
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerSponsor(
                        "",
                        Set.of(),
                        0L,
                        Map.of(RECOVERY, -1L)
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PlayerSponsor.DEFAULT
                        .cooldownRemaining(RECOVERY, -1L)
        );
    }
}
