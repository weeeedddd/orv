package com.weeeedddd.orv.economy;

import com.weeeedddd.orv.data.ICoinData;
import com.weeeedddd.orv.data.ModAttachments;
import com.weeeedddd.orv.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Application service for all server-side coin transactions.
 */
public final class CoinService {
    private CoinService() {
    }

    public static long getCoins(Player player) {
        return coinData(player).getCoins();
    }

    public static boolean hasEnough(Player player, long amount) {
        return coinData(player).hasEnough(amount);
    }

    public static long addCoins(ServerPlayer player, long amount) {
        long balance = coinData(player).addCoins(amount);
        sync(player);
        return balance;
    }

    public static boolean removeCoins(ServerPlayer player, long amount) {
        boolean removed = coinData(player).removeCoins(amount);
        if (removed) {
            sync(player);
        }
        return removed;
    }

    public static long setCoins(ServerPlayer player, long amount) {
        ICoinData data = coinData(player);
        data.setCoins(amount);
        sync(player);
        return data.getCoins();
    }

    private static ICoinData coinData(Player player) {
        return ModAttachments.getCoinData(player);
    }

    private static void sync(ServerPlayer player) {
        ModNetworking.syncSystemData(player);
    }
}
