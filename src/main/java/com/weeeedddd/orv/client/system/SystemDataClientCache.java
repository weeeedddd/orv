package com.weeeedddd.orv.client.system;

import com.weeeedddd.orv.network.SyncSystemDataPayload;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe cache of complete system snapshots for client rendering.
 */
public final class SystemDataClientCache {
    private static final ConcurrentMap<UUID, SystemDataSnapshot> SNAPSHOTS =
            new ConcurrentHashMap<>();

    private SystemDataClientCache() {
    }

    public static void accept(SyncSystemDataPayload payload) {
        SystemDataSnapshot snapshot = new SystemDataSnapshot(
                payload.playerId(),
                payload.coins(),
                payload.energy(),
                payload.maxEnergy(),
                payload.channelId(),
                payload.constellationName()
        );
        SNAPSHOTS.put(snapshot.playerId(), snapshot);
    }

    public static Optional<SystemDataSnapshot> find(UUID playerId) {
        return Optional.ofNullable(SNAPSHOTS.get(playerId));
    }

    public static Map<UUID, SystemDataSnapshot> snapshot() {
        return Map.copyOf(SNAPSHOTS);
    }

    public static int size() {
        return SNAPSHOTS.size();
    }

    public static void clear() {
        SNAPSHOTS.clear();
    }
}
