package com.weeeedddd.orv.stigma;

import java.util.Objects;

public record StigmaExecutionResult(
        StigmaExecutionStatus status,
        long remainingCooldownTicks
) {
    public StigmaExecutionResult {
        Objects.requireNonNull(status, "status");
        if (remainingCooldownTicks < 0L) {
            throw new IllegalArgumentException(
                    "remainingCooldownTicks must not be negative"
            );
        }
        if (status != StigmaExecutionStatus.ON_COOLDOWN
                && remainingCooldownTicks != 0L) {
            throw new IllegalArgumentException(
                    "only cooldown results may contain remaining ticks"
            );
        }
    }

    public static StigmaExecutionResult of(
            StigmaExecutionStatus status
    ) {
        return new StigmaExecutionResult(status, 0L);
    }

    public static StigmaExecutionResult onCooldown(
            long remainingCooldownTicks
    ) {
        return new StigmaExecutionResult(
                StigmaExecutionStatus.ON_COOLDOWN,
                remainingCooldownTicks
        );
    }
}
