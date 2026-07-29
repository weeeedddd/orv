package com.weeeedddd.orv.stigma;

import com.weeeedddd.orv.OrvMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ModStigmaEffects {
    public static final ResourceLocation HEAL =
            ResourceLocation.fromNamespaceAndPath(
                    OrvMod.MOD_ID,
                    "heal"
            );

    private static final StigmaEffectRegistry<ServerPlayer> EFFECTS =
            createRegistry();

    private ModStigmaEffects() {
    }

    public static StigmaEffectRegistry<ServerPlayer> registry() {
        return EFFECTS;
    }

    public static void register(
            ResourceLocation effectId,
            StigmaEffectLogic<ServerPlayer> effect
    ) {
        EFFECTS.register(effectId, effect);
    }

    private static StigmaEffectRegistry<ServerPlayer>
            createRegistry() {
        StigmaEffectRegistry<ServerPlayer> registry =
                new StigmaEffectRegistry<>();
        registry.register(
                HEAL,
                context -> context.player().heal(4.0F)
        );
        return registry;
    }
}
