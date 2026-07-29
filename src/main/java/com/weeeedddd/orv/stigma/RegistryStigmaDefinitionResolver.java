package com.weeeedddd.orv.stigma;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class RegistryStigmaDefinitionResolver
        implements StigmaDefinitionResolver<ServerPlayer> {
    @Override
    public Optional<StigmaDefinition> find(
            ServerPlayer player,
            ResourceLocation stigmaId
    ) {
        Registry<StigmaDefinition> registry = player.registryAccess()
                .registryOrThrow(
                        ModStigmaRegistries.STIGMA_REGISTRY_KEY
                );
        return Optional.ofNullable(registry.get(stigmaId));
    }
}
