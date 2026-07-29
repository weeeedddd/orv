package com.weeeedddd.orv.stigma;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Production entry point backed by datapack registries and attachments.
 */
public final class ServerStigmaEngine {
    private static final StigmaEngine<ServerPlayer> ENGINE =
            new StigmaEngine<>(
                    new RegistryStigmaDefinitionResolver(),
                    ModStigmaEffects.registry(),
                    new AttachmentStigmaPlayerStateAccess(),
                    player -> player.level().getGameTime()
            );

    private ServerStigmaEngine() {
    }

    public static StigmaExecutionResult execute(
            ServerPlayer player,
            ResourceLocation stigmaId
    ) {
        return ENGINE.execute(player, stigmaId);
    }

    public static StigmaEngine<ServerPlayer> api() {
        return ENGINE;
    }
}
