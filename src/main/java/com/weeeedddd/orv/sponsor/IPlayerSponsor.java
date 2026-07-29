package com.weeeedddd.orv.sponsor;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

/**
 * Immutable, persistent sponsor state owned by one server player.
 */
public interface IPlayerSponsor {
    String constellationName();

    Set<ResourceLocation> activeStigmas();

    long probability();

    Map<ResourceLocation, Long> cooldowns();

    boolean hasActiveStigma(ResourceLocation stigmaId);

    long cooldownRemaining(
            ResourceLocation stigmaId,
            long currentGameTick
    );

    IPlayerSponsor withConstellationName(String constellationName);

    IPlayerSponsor withActiveStigma(ResourceLocation stigmaId);

    IPlayerSponsor withoutActiveStigma(ResourceLocation stigmaId);

    IPlayerSponsor withProbability(long probability);

    IPlayerSponsor withCooldownUntil(
            ResourceLocation stigmaId,
            long readyAtGameTick
    );
}
