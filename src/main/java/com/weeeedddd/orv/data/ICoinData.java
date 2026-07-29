package com.weeeedddd.orv.data;

/**
 * Server-authoritative coin state attached to a player.
 *
 * <p>Mutation methods reject negative amounts. Implementations must apply
 * changes atomically so a failed operation cannot partially change the
 * balance.</p>
 */
public interface ICoinData {
    long getCoins();

    long addCoins(long amount);

    boolean removeCoins(long amount);

    boolean hasEnough(long amount);

    void setCoins(long amount);
}
