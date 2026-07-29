package com.weeeedddd.orv.guild;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuildCreationCheckTest {

    private static final int LEVEL = GuildConfig.DEFAULT_MINIMUM_LEVEL;
    private static final long COST = GuildConfig.DEFAULT_CREATION_COST;

    @Test
    void allowsCreationWhenEveryRequirementIsMet() {
        GuildCreationCheck check = GuildCreationCheck.evaluate(
                false,
                LEVEL,
                COST,
                "Golden Dawn"
        );

        assertTrue(check.allowed());
        assertEquals(GuildCreationCheck.Status.ALLOWED, check.status());
    }

    @Test
    void reportsTheConfiguredRequirementsBackToTheCaller() {
        GuildCreationCheck check = GuildCreationCheck.evaluate(
                false,
                0,
                0L,
                null
        );

        assertEquals(LEVEL, check.requiredLevel());
        assertEquals(COST, check.requiredCoins());
    }

    @Test
    void rejectsAPlayerWhoAlreadyBelongsToAGuild() {
        GuildCreationCheck check = GuildCreationCheck.evaluate(
                true,
                LEVEL * 10,
                COST * 10,
                "Second Guild"
        );

        assertFalse(check.allowed());
        assertEquals(
                GuildCreationCheck.Status.ALREADY_IN_GUILD,
                check.status()
        );
    }

    @Test
    void rejectsAPlayerBelowTheLevelThreshold() {
        GuildCreationCheck check = GuildCreationCheck.evaluate(
                false,
                LEVEL - 1,
                COST,
                "Too Early"
        );

        assertEquals(
                GuildCreationCheck.Status.LEVEL_TOO_LOW,
                check.status()
        );
    }

    @Test
    void rejectsAPlayerWhoCannotAffordTheCost() {
        GuildCreationCheck check = GuildCreationCheck.evaluate(
                false,
                LEVEL,
                COST - 1,
                "Too Poor"
        );

        assertEquals(
                GuildCreationCheck.Status.NOT_ENOUGH_COINS,
                check.status()
        );
    }

    @Test
    void rejectsABlankName() {
        GuildCreationCheck check = GuildCreationCheck.evaluate(
                false,
                LEVEL,
                COST,
                "   "
        );

        assertEquals(GuildCreationCheck.Status.INVALID_NAME, check.status());
    }

    @Test
    void treatsANullNameAsAnEligibilityProbe() {
        // The client asks with a null name to decide whether to grey the
        // button out, before the player has typed anything.
        GuildCreationCheck check = GuildCreationCheck.evaluate(
                false,
                LEVEL,
                COST,
                null
        );

        assertTrue(check.allowed());
    }

    @Test
    void checksLevelBeforeCoins() {
        GuildCreationCheck check = GuildCreationCheck.evaluate(
                false,
                LEVEL - 1,
                0L,
                "Neither"
        );

        assertEquals(
                GuildCreationCheck.Status.LEVEL_TOO_LOW,
                check.status()
        );
    }

    @Test
    void namesTheMissingRequirementInTheDescription() {
        String description = GuildCreationCheck.evaluate(
                false,
                LEVEL - 5,
                COST,
                "Blocked"
        ).describe();

        assertTrue(description.contains(String.valueOf(LEVEL)));
        assertTrue(description.contains(String.valueOf(COST)));
    }
}
