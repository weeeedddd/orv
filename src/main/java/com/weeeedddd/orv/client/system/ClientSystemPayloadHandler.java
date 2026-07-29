package com.weeeedddd.orv.client.system;

import com.weeeedddd.orv.network.SyncSystemDataPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Publishes network updates to the HUD cache on the client main thread.
 */
public final class ClientSystemPayloadHandler {
    private ClientSystemPayloadHandler() {
    }

    public static void handle(
            SyncSystemDataPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> SystemDataClientCache.accept(payload)
        );
    }
}
