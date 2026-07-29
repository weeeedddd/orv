package com.weeeedddd.orv.client.guild;

import com.weeeedddd.orv.guild.GuildCreationCheck;
import com.weeeedddd.orv.guild.GuildRole;
import com.weeeedddd.orv.guild.GuildSnapshot;

import java.util.List;
import java.util.UUID;

public final class GuildClientState {
    private static GuildSnapshot snapshot = new GuildSnapshot(
            "LOADING...",
            new UUID(0L, 0L),
            GuildRole.NONE,
            List.of(),
            List.of(),
            "",
            // Placeholder until the first snapshot arrives; the server
            // always overwrites this with a real evaluation.
            new GuildCreationCheck(
                    GuildCreationCheck.Status.ALREADY_IN_GUILD,
                    0,
                    0L,
                    0,
                    0L
            )
    );
    private static long revision;

    private GuildClientState() {
    }

    public static synchronized void accept(GuildSnapshot newSnapshot) {
        snapshot = newSnapshot;
        revision++;
    }

    public static synchronized GuildSnapshot snapshot() {
        return snapshot;
    }

    public static synchronized long revision() {
        return revision;
    }
}
