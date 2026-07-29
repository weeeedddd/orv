package com.weeeedddd.orv.stigma;

import com.weeeedddd.orv.sponsor.IPlayerSponsor;

import java.util.Objects;

public record StigmaPlayerState(
        IPlayerSponsor sponsor,
        long energy
) {
    public StigmaPlayerState {
        Objects.requireNonNull(sponsor, "sponsor");
        if (energy < 0L) {
            throw new IllegalArgumentException(
                    "energy must not be negative"
            );
        }
    }
}
