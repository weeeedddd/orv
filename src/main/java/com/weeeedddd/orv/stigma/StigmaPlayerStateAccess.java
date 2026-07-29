package com.weeeedddd.orv.stigma;

public interface StigmaPlayerStateAccess<P> {
    StigmaPlayerState load(P player);

    void commit(
            P player,
            StigmaPlayerState updatedState
    );
}
