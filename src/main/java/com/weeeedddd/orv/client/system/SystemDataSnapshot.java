package com.weeeedddd.orv.client.system;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable client-side view of the server's system data.
 */
public record SystemDataSnapshot(
        UUID playerId,
        long coins,
        long energy,
        long maxEnergy,
        String channelId,
        String constellationName
) {
    public SystemDataSnapshot {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(
                constellationName,
                "constellationName"
        );

        if (coins < 0L) {
            throw new IllegalArgumentException(
                    "coins must not be negative"
            );
        }
        if (energy < 0L) {
            throw new IllegalArgumentException(
                    "energy must not be negative"
            );
        }
        if (maxEnergy < 1L) {
            throw new IllegalArgumentException(
                    "maxEnergy must be at least 1"
            );
        }
    }
}
