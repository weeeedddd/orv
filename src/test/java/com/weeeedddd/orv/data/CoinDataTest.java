package com.weeeedddd.orv.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoinDataTest {
    @Test
    void exposesTheCoinDataContract() {
        ICoinData data = new CoinData(25L);

        assertEquals(25L, data.getCoins());
        assertTrue(data.hasEnough(25L));
        assertFalse(data.hasEnough(26L));
    }

    @Test
    void addsCoinsAndReturnsTheNewBalance() {
        ICoinData data = new CoinData(25L);

        long balance = data.addCoins(17L);

        assertEquals(42L, balance);
        assertEquals(42L, data.getCoins());
    }

    @Test
    void removesCoinsAtomicallyWhenTheBalanceIsSufficient() {
        ICoinData data = new CoinData(25L);

        boolean removed = data.removeCoins(10L);

        assertTrue(removed);
        assertEquals(15L, data.getCoins());
    }

    @Test
    void leavesTheBalanceUnchangedWhenFundsAreInsufficient() {
        ICoinData data = new CoinData(25L);

        boolean removed = data.removeCoins(26L);

        assertFalse(removed);
        assertEquals(25L, data.getCoins());
    }

    @Test
    void supportsSettingAnAdministrativeBalance() {
        ICoinData data = new CoinData(25L);

        data.setCoins(100L);

        assertEquals(100L, data.getCoins());
    }

    @Test
    void rejectsNegativeTransactionAmountsWithoutChangingTheBalance() {
        ICoinData data = new CoinData(25L);

        assertThrows(
                IllegalArgumentException.class,
                () -> data.addCoins(-1L)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> data.removeCoins(-1L)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> data.hasEnough(-1L)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> data.setCoins(-1L)
        );
        assertEquals(25L, data.getCoins());
    }

    @Test
    void rejectsOverflowWithoutChangingTheBalance() {
        ICoinData data = new CoinData(Long.MAX_VALUE);

        assertThrows(
                ArithmeticException.class,
                () -> data.addCoins(1L)
        );
        assertEquals(Long.MAX_VALUE, data.getCoins());
    }
}
