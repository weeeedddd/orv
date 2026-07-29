package com.weeeedddd.orv.client.system;

import com.weeeedddd.orv.network.SyncSystemDataPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemDataClientCacheTest {
    @AfterEach
    void clearCache() {
        SystemDataClientCache.clear();
    }

    @Test
    void publishesAnImmutableSnapshotForTheHud() {
        UUID playerId = UUID.randomUUID();
        SyncSystemDataPayload payload = new SyncSystemDataPayload(
                playerId,
                500L,
                80L,
                "Prisoner of the Golden Headband"
        );

        SystemDataClientCache.accept(payload);

        SystemDataSnapshot snapshot = SystemDataClientCache
                .find(playerId)
                .orElseThrow();
        assertEquals(playerId, snapshot.playerId());
        assertEquals(500L, snapshot.coins());
        assertEquals(80L, snapshot.energy());
        assertEquals(
                "Prisoner of the Golden Headband",
                snapshot.constellationName()
        );
    }

    @Test
    void replacesAllFieldsAsOneAtomicSnapshot() {
        UUID playerId = UUID.randomUUID();
        SystemDataClientCache.accept(new SyncSystemDataPayload(
                playerId,
                10L,
                20L,
                "Old"
        ));

        SystemDataClientCache.accept(new SyncSystemDataPayload(
                playerId,
                30L,
                40L,
                "New"
        ));

        assertEquals(
                new SystemDataSnapshot(
                        playerId,
                        30L,
                        40L,
                        "New"
                ),
                SystemDataClientCache.find(playerId).orElseThrow()
        );
    }

    @Test
    void acceptsUpdatesFromManyThreads() throws Exception {
        int playerCount = 100;
        List<Callable<Void>> updates = new ArrayList<>();

        for (int index = 0; index < playerCount; index++) {
            int value = index;
            updates.add(() -> {
                UUID playerId = new UUID(0L, value + 1L);
                SystemDataClientCache.accept(
                        new SyncSystemDataPayload(
                                playerId,
                                value,
                                value * 2L,
                                "Constellation " + value
                        )
                );
                return null;
            });
        }

        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Void>> futures = executor.invokeAll(updates);
            for (Future<Void> future : futures) {
                future.get();
            }
        }

        assertEquals(playerCount, SystemDataClientCache.size());
        assertTrue(SystemDataClientCache.find(
                new UUID(0L, playerCount)
        ).isPresent());
    }

    @Test
    void exposesAnUnmodifiableCacheView() {
        UUID playerId = UUID.randomUUID();
        SystemDataClientCache.accept(new SyncSystemDataPayload(
                playerId,
                1L,
                2L,
                ""
        ));
        Map<UUID, SystemDataSnapshot> snapshot =
                SystemDataClientCache.snapshot();

        assertThrows(
                UnsupportedOperationException.class,
                snapshot::clear
        );
    }
}
