package com.weeeedddd.orv.character;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StrengthServiceTest {

    @Test
    void addsLevelsWithoutLosingTheCurrentValue() {
        assertEquals(15, StrengthService.calculateAddedLevel(10, 5));
    }

    @Test
    void rejectsStrengthLevelOverflow() {
        assertThrows(
                ArithmeticException.class,
                () -> StrengthService.calculateAddedLevel(
                        Integer.MAX_VALUE,
                        1
                )
        );
    }

    @Test
    void rejectsNegativeLevelAmounts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> StrengthService.calculateAddedLevel(10, -1)
        );
    }
}
