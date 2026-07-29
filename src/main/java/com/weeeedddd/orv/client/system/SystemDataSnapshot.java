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
        String constellationName
) {
    public SystemDataSnapshot {
        Objects.requireNonNull(playerId, "playerId");
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
    }
}
