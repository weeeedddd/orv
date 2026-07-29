package com.weeeedddd.orv.sponsor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record PlayerSponsor(
        String constellationName,
        Set<ResourceLocation> activeStigmas,
        long probability,
        Map<ResourceLocation, Long> cooldowns
) implements IPlayerSponsor {
    public static final int MAX_CONSTELLATION_NAME_LENGTH = 64;
    public static final PlayerSponsor DEFAULT = new PlayerSponsor(
            "",
            Set.of(),
            0L,
            Map.of()
    );

    private static final Codec<Set<ResourceLocation>>
            ACTIVE_STIGMAS_CODEC = ResourceLocation.CODEC.listOf()
            .xmap(
                    Set::copyOf,
                    stigmas -> stigmas.stream()
                            .sorted(Comparator.comparing(
                                    ResourceLocation::toString
                            ))
                            .toList()
            );

    private static final Codec<Map<ResourceLocation, Long>>
            COOLDOWNS_CODEC = Codec.unboundedMap(
                    ResourceLocation.CODEC,
                    Codec.LONG
            );

    public static final Codec<PlayerSponsor> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.optionalFieldOf(
                            "constellation",
                            ""
                    ).forGetter(PlayerSponsor::constellationName),
                    ACTIVE_STIGMAS_CODEC.optionalFieldOf(
                            "active_stigmas",
                            Set.of()
                    ).forGetter(PlayerSponsor::activeStigmas),
                    Codec.LONG.optionalFieldOf(
                            "probability",
                            0L
                    ).forGetter(PlayerSponsor::probability),
                    COOLDOWNS_CODEC.optionalFieldOf(
                            "cooldowns",
                            Map.of()
                    ).forGetter(PlayerSponsor::cooldowns)
            ).apply(instance, PlayerSponsor::new));

    public PlayerSponsor {
        Objects.requireNonNull(
                constellationName,
                "constellationName"
        );
        Objects.requireNonNull(activeStigmas, "activeStigmas");
        Objects.requireNonNull(cooldowns, "cooldowns");

        constellationName = constellationName.trim();
        if (constellationName.length()
                > MAX_CONSTELLATION_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "constellationName exceeds "
                            + MAX_CONSTELLATION_NAME_LENGTH
                            + " characters"
            );
        }
        if (probability < 0L) {
            throw new IllegalArgumentException(
                    "probability must not be negative"
            );
        }

        activeStigmas = Set.copyOf(activeStigmas);
        Map<ResourceLocation, Long> validatedCooldowns =
                new HashMap<>();
        for (Map.Entry<ResourceLocation, Long> entry
                : cooldowns.entrySet()) {
            ResourceLocation stigmaId = Objects.requireNonNull(
                    entry.getKey(),
                    "cooldown stigmaId"
            );
            Long readyAtGameTick = Objects.requireNonNull(
                    entry.getValue(),
                    "cooldown readyAtGameTick"
            );
            if (readyAtGameTick < 0L) {
                throw new IllegalArgumentException(
                        "cooldown tick must not be negative"
                );
            }
            validatedCooldowns.put(stigmaId, readyAtGameTick);
        }
        cooldowns = Map.copyOf(validatedCooldowns);
    }

    public static PlayerSponsor copyOf(IPlayerSponsor sponsor) {
        Objects.requireNonNull(sponsor, "sponsor");
        if (sponsor instanceof PlayerSponsor playerSponsor) {
            return playerSponsor;
        }
        return new PlayerSponsor(
                sponsor.constellationName(),
                sponsor.activeStigmas(),
                sponsor.probability(),
                sponsor.cooldowns()
        );
    }

    @Override
    public boolean hasActiveStigma(ResourceLocation stigmaId) {
        return activeStigmas.contains(
                Objects.requireNonNull(stigmaId, "stigmaId")
        );
    }

    @Override
    public long cooldownRemaining(
            ResourceLocation stigmaId,
            long currentGameTick
    ) {
        Objects.requireNonNull(stigmaId, "stigmaId");
        if (currentGameTick < 0L) {
            throw new IllegalArgumentException(
                    "currentGameTick must not be negative"
            );
        }

        Long readyAtGameTick = cooldowns.get(stigmaId);
        if (readyAtGameTick == null
                || readyAtGameTick <= currentGameTick) {
            return 0L;
        }
        return readyAtGameTick - currentGameTick;
    }

    @Override
    public PlayerSponsor withConstellationName(
            String newConstellationName
    ) {
        return new PlayerSponsor(
                newConstellationName,
                activeStigmas,
                probability,
                cooldowns
        );
    }

    @Override
    public PlayerSponsor withActiveStigma(
            ResourceLocation stigmaId
    ) {
        Objects.requireNonNull(stigmaId, "stigmaId");
        Set<ResourceLocation> updated = new HashSet<>(
                activeStigmas
        );
        updated.add(stigmaId);
        return new PlayerSponsor(
                constellationName,
                updated,
                probability,
                cooldowns
        );
    }

    @Override
    public PlayerSponsor withoutActiveStigma(
            ResourceLocation stigmaId
    ) {
        Objects.requireNonNull(stigmaId, "stigmaId");
        Set<ResourceLocation> updated = new HashSet<>(
                activeStigmas
        );
        updated.remove(stigmaId);
        return new PlayerSponsor(
                constellationName,
                updated,
                probability,
                cooldowns
        );
    }

    @Override
    public PlayerSponsor withProbability(long newProbability) {
        return new PlayerSponsor(
                constellationName,
                activeStigmas,
                newProbability,
                cooldowns
        );
    }

    @Override
    public PlayerSponsor withCooldownUntil(
            ResourceLocation stigmaId,
            long readyAtGameTick
    ) {
        Objects.requireNonNull(stigmaId, "stigmaId");
        if (readyAtGameTick < 0L) {
            throw new IllegalArgumentException(
                    "readyAtGameTick must not be negative"
            );
        }
        Map<ResourceLocation, Long> updated =
                new HashMap<>(cooldowns);
        updated.put(stigmaId, readyAtGameTick);
        return new PlayerSponsor(
                constellationName,
                activeStigmas,
                probability,
                updated
        );
    }
}
