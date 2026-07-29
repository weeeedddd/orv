package com.weeeedddd.orv.client.character;

import com.weeeedddd.orv.network.SyncCharacterProfilePayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Publishes character sheet updates to the client cache on the main thread.
 */
public final class ClientCharacterPayloadHandler {

    private ClientCharacterPayloadHandler() {
    }

    public static void handle(
            SyncCharacterProfilePayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> CharacterClientCache.accept(payload));
    }
}
