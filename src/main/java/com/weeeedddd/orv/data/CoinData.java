package com.weeeedddd.orv.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class CoinData implements ICoinData {
    public static final Codec<CoinData> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.LONG
                                    .fieldOf("coins")
                                    .forGetter(CoinData::getCoins)
                    ).apply(instance, CoinData::new)
            );

    private long coins;

    public CoinData() {
        this(0L);
    }

    public CoinData(long coins) {
        setCoins(coins);
    }

    @Override
    public long getCoins() {
        return coins;
    }

    @Override
    public long addCoins(long amount) {
        validateTransactionAmount(amount);
        long newBalance = Math.addExact(coins, amount);
        coins = newBalance;
        return newBalance;
    }

    @Override
    public boolean removeCoins(long amount) {
        validateTransactionAmount(amount);
        if (!hasEnough(amount)) {
            return false;
        }
        coins -= amount;
        return true;
    }

    @Override
    public boolean hasEnough(long amount) {
        validateTransactionAmount(amount);
        return coins >= amount;
    }

    @Override
    public void setCoins(long amount) {
        validateTransactionAmount(amount);
        coins = amount;
    }

    private static void validateTransactionAmount(long amount) {
        if (amount < 0L) {
            throw new IllegalArgumentException(
                    "coin amount must not be negative"
            );
        }
    }
}
