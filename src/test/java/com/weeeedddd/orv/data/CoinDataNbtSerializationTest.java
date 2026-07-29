package com.weeeedddd.orv.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CoinDataNbtSerializationTest {
    @ParameterizedTest
    @ValueSource(longs = {
            0L,
            1L,
            1_000_000L,
            Long.MAX_VALUE
    })
    void preservesCoinBalancesAcrossAnNbtRoundTrip(long coins) {
        CoinData original = new CoinData(coins);

        Tag encoded = CoinData.CODEC
                .encodeStart(NbtOps.INSTANCE, original)
                .getOrThrow();
        CoinData restored = CoinData.CODEC
                .parse(NbtOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(coins, restored.getCoins());
    }

    @Test
    void serializesTheCurrentBalanceAfterTransactions() {
        CoinData data = new CoinData(10L);
        data.addCoins(5L);
        data.removeCoins(3L);

        Tag encoded = CoinData.CODEC
                .encodeStart(NbtOps.INSTANCE, data)
                .getOrThrow();
        CompoundTag tag = assertInstanceOf(
                CompoundTag.class,
                encoded
        );

        assertEquals(12L, tag.getLong("coins"));
    }
}
