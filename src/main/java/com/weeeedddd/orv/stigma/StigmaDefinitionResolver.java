package com.weeeedddd.orv.stigma;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

@FunctionalInterface
public interface StigmaDefinitionResolver<P> {
    Optional<StigmaDefinition> find(
            P player,
            ResourceLocation stigmaId
    );
}
