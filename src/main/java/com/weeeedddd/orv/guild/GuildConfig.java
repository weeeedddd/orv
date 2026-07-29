package com.weeeedddd.orv.guild;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side tuning for guild creation.
 *
 * <p>Lives in the server config so an operator can change the barrier to
 * entry without touching code. The defaults are the agreed values: level 10
 * and 15,000 coins.
 */
public final class GuildConfig {

    public static final int DEFAULT_MINIMUM_LEVEL = 10;
    public static final long DEFAULT_CREATION_COST = 15_000L;

    private static final ModConfigSpec.Builder BUILDER =
            new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue MINIMUM_LEVEL = BUILDER
            .comment("Strength level required to found a guild.")
            .defineInRange(
                    "guild.minimumLevel",
                    DEFAULT_MINIMUM_LEVEL,
                    0,
                    Integer.MAX_VALUE
            );

    private static final ModConfigSpec.LongValue CREATION_COST = BUILDER
            .comment("Coins deducted when a guild is founded.")
            .defineInRange(
                    "guild.creationCost",
                    DEFAULT_CREATION_COST,
                    0L,
                    Long.MAX_VALUE
            );

    public static final ModConfigSpec SPEC = BUILDER.build();

    private GuildConfig() {
    }

    /**
     * Reading a config value before the file is loaded throws, which would
     * take the server down over a cosmetic lookup. Falling back to the
     * default keeps the requirement check answerable at any time.
     */
    public static int minimumLevel() {
        try {
            return MINIMUM_LEVEL.get();
        } catch (IllegalStateException notLoadedYet) {
            return DEFAULT_MINIMUM_LEVEL;
        }
    }

    public static long creationCost() {
        try {
            return CREATION_COST.get();
        } catch (IllegalStateException notLoadedYet) {
            return DEFAULT_CREATION_COST;
        }
    }
}
