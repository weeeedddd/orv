package com.weeeedddd.orv.guild;

import java.util.List;
import java.util.UUID;

public record GuildSnapshot(
        String guildName,
        UUID viewerId,
        GuildRole viewerRole,
        List<Member> members,
        List<OnlinePlayer> onlinePlayers,
        String emblem,
        GuildCreationCheck creation
) {
    public GuildSnapshot {
        members = List.copyOf(members);
        onlinePlayers = List.copyOf(onlinePlayers);
        emblem = emblem == null ? "" : emblem;
    }

    public record Member(
            UUID playerId,
            String playerName,
            GuildRole role,
            boolean online
    ) {
    }

    public record OnlinePlayer(
            UUID playerId,
            String playerName,
            boolean available
    ) {
    }
}
