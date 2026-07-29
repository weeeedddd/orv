package com.weeeedddd.orv.data;

import com.weeeedddd.orv.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PlayerDataEvents {
    private PlayerDataEvents() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event);
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event);
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event);
    }

    private static void sync(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetworking.syncPlayerData(player);
        }
    }
}
