package com.weeeedddd.orv.stigma;

import com.weeeedddd.orv.OrvMod;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

public final class ModStigmaRegistries {
    public static final ResourceKey<Registry<StigmaDefinition>>
            STIGMA_REGISTRY_KEY = ResourceKey.createRegistryKey(
                    ResourceLocation.fromNamespaceAndPath(
                            OrvMod.MOD_ID,
                            "stigma"
                    )
            );

    private ModStigmaRegistries() {
    }

    public static void register(
            DataPackRegistryEvent.NewRegistry event
    ) {
        event.dataPackRegistry(
                STIGMA_REGISTRY_KEY,
                StigmaDefinition.CODEC
        );
    }
}
