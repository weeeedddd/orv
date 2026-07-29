package com.weeeedddd.orv.stigma;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record StigmaExecutionContext<P>(
        P player,
        ResourceLocation stigmaId,
        StigmaDefinition definition,
        long probability
) {
    public StigmaExecutionContext {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(stigmaId, "stigmaId");
        Objects.requireNonNull(definition, "definition");
        if (probability < 0L) {
            throw new IllegalArgumentException(
                    "probability must not be negative"
            );
        }
    }
}
