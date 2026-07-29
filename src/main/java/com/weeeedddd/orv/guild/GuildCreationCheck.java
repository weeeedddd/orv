package com.weeeedddd.orv.guild;

/**
 * The outcome of testing whether a player may found a guild.
 *
 * <p>The same record is evaluated server-side before a guild is created and
 * shipped to the client so the button can be greyed out with a reason,
 * rather than the client guessing at the rules.
 *
 * @param status         why creation is or is not allowed
 * @param requiredLevel  level threshold currently in force
 * @param requiredCoins  coin cost currently in force
 * @param playerLevel    the player's level at the time of the check
 * @param playerCoins    the player's balance at the time of the check
 */
public record GuildCreationCheck(
        Status status,
        int requiredLevel,
        long requiredCoins,
        int playerLevel,
        long playerCoins
) {
    public enum Status {
        /** All requirements met. */
        ALLOWED,
        /** The player already belongs to a guild. */
        ALREADY_IN_GUILD,
        /** Below the configured level threshold. */
        LEVEL_TOO_LOW,
        /** Cannot afford the founding cost. */
        NOT_ENOUGH_COINS,
        /** The requested name was blank or otherwise unusable. */
        INVALID_NAME
    }

    public boolean allowed() {
        return status == Status.ALLOWED;
    }

    /**
     * Runs the requirement test. Name validation is skipped when
     * {@code requestedName} is {@code null}, which is how the client asks
     * "could I create a guild at all?" before typing one.
     */
    public static GuildCreationCheck evaluate(
            boolean alreadyInGuild,
            int playerLevel,
            long playerCoins,
            String requestedName
    ) {
        int requiredLevel = GuildConfig.minimumLevel();
        long requiredCoins = GuildConfig.creationCost();

        Status status;
        if (alreadyInGuild) {
            status = Status.ALREADY_IN_GUILD;
        } else if (playerLevel < requiredLevel) {
            status = Status.LEVEL_TOO_LOW;
        } else if (playerCoins < requiredCoins) {
            status = Status.NOT_ENOUGH_COINS;
        } else if (requestedName != null && requestedName.isBlank()) {
            status = Status.INVALID_NAME;
        } else {
            status = Status.ALLOWED;
        }

        return new GuildCreationCheck(
                status,
                requiredLevel,
                requiredCoins,
                playerLevel,
                playerCoins
        );
    }

    /** Short line shown on the greyed-out button and in chat. */
    public String describe() {
        return switch (status) {
            case ALLOWED -> "Found a new guild for "
                    + requiredCoins + " coins.";
            case ALREADY_IN_GUILD -> "You already belong to a guild.";
            case LEVEL_TOO_LOW -> "Requires Lv. " + requiredLevel
                    + " and " + requiredCoins + " coins"
                    + " (you are Lv. " + playerLevel + ").";
            case NOT_ENOUGH_COINS -> "Requires Lv. " + requiredLevel
                    + " and " + requiredCoins + " coins"
                    + " (you have " + playerCoins + ").";
            case INVALID_NAME -> "Enter a guild name first.";
        };
    }
}
