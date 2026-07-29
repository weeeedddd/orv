package com.weeeedddd.orv.client.character;

import com.weeeedddd.orv.network.SyncCharacterProfilePayload;

/**
 * Holds the last character sheet the server sent for the local player.
 *
 * <p>Guarded by the class monitor because the payload handler publishes from
 * the client main thread while the screen reads during rendering.
 */
public final class CharacterClientCache {

    private static SyncCharacterProfilePayload snapshot;

    private CharacterClientCache() {
    }

    public static synchronized void accept(
            SyncCharacterProfilePayload payload
    ) {
        snapshot = payload;
    }

    /** The current sheet, or {@code null} before the first sync arrives. */
    public static synchronized SyncCharacterProfilePayload snapshot() {
        return snapshot;
    }

    public static synchronized void clear() {
        snapshot = null;
    }
}
