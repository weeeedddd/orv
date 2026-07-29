package com.weeeedddd.orv.client.system;

import com.weeeedddd.orv.network.SyncSystemDataPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ClientSystemPayloadHandlerTest {
    @AfterEach
    void clearCache() {
        SystemDataClientCache.clear();
    }

    @Test
    void publishesThePayloadOnTheClientsMainThread() {
        IPayloadContext context = mock(IPayloadContext.class);
        UUID playerId = UUID.randomUUID();
        SyncSystemDataPayload payload = new SyncSystemDataPayload(
                playerId,
                77L,
                21L,
                100L,
                "#BIHYUNG-412",
                "Secretive Plotter"
        );

        ClientSystemPayloadHandler.handle(payload, context);

        assertTrue(SystemDataClientCache.find(playerId).isEmpty());
        ArgumentCaptor<Runnable> work =
                ArgumentCaptor.forClass(Runnable.class);
        verify(context).enqueueWork(work.capture());

        work.getValue().run();

        assertEquals(
                new SystemDataSnapshot(
                        playerId,
                        77L,
                        21L,
                        100L,
                        "#BIHYUNG-412",
                        "Secretive Plotter"
                ),
                SystemDataClientCache.find(playerId).orElseThrow()
        );
    }
}
