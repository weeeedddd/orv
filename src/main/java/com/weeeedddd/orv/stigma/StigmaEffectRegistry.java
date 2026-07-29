package com.weeeedddd.orv.stigma;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Code-side registry addressed by data-driven effect logic identifiers.
 */
public final class StigmaEffectRegistry<P> {
    private final ConcurrentMap<ResourceLocation, StigmaEffectLogic<P>>
            effects = new ConcurrentHashMap<>();

    public void register(
            ResourceLocation effectId,
            StigmaEffectLogic<P> effect
    ) {
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(effect, "effect");

        StigmaEffectLogic<P> previous = effects.putIfAbsent(
                effectId,
                effect
        );
        if (previous != null) {
            throw new IllegalStateException(
                    "Effect logic already registered: " + effectId
            );
        }
    }

    public Optional<StigmaEffectLogic<P>> find(
            ResourceLocation effectId
    ) {
        return Optional.ofNullable(
                effects.get(
                        Objects.requireNonNull(effectId, "effectId")
                )
        );
    }

    public Map<ResourceLocation, StigmaEffectLogic<P>> snapshot() {
        return Map.copyOf(effects);
    }
}
