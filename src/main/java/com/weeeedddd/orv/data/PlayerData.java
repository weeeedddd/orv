package com.weeeedddd.orv.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-player system values owned by the server.
 *
 * @param strengthLevel combat level shown on the HUD
 * @param energy        current energy
 * @param maxEnergy     energy ceiling the HUD renders as {@code energy / max}
 * @param channelId     broadcast channel tag, empty while the player is not
 *                      carried by a channel
 */
public record PlayerData(
        int strengthLevel,
        long energy,
        long maxEnergy,
        String channelId
) {
    public static final long DEFAULT_MAX_ENERGY = 100L;
    public static final int MAX_CHANNEL_ID_LENGTH = 32;

    public static final PlayerData DEFAULT =
            new PlayerData(0, 0L, DEFAULT_MAX_ENERGY, "");

    // maxEnergy and channelId are optional so worlds saved before they
    // existed still load.
    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.fieldOf("strength_level")
                            .forGetter(PlayerData::strengthLevel),
                    Codec.LONG.optionalFieldOf("energy", 0L)
                            .forGetter(PlayerData::energy),
                    Codec.LONG.optionalFieldOf(
                                    "max_energy",
                                    DEFAULT_MAX_ENERGY
                            )
                            .forGetter(PlayerData::maxEnergy),
                    Codec.STRING.optionalFieldOf("channel_id", "")
                            .forGetter(PlayerData::channelId)
            ).apply(instance, PlayerData::new)
    );

    public PlayerData {
        if (strengthLevel < 0) {
            throw new IllegalArgumentException(
                    "strengthLevel must not be negative"
            );
        }
        if (energy < 0L) {
            throw new IllegalArgumentException(
                    "energy must not be negative"
            );
        }
        if (maxEnergy < 1L) {
            throw new IllegalArgumentException(
                    "maxEnergy must be at least 1"
            );
        }
        if (channelId == null) {
            throw new IllegalArgumentException(
                    "channelId must not be null"
            );
        }
        if (channelId.length() > MAX_CHANNEL_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "channelId exceeds " + MAX_CHANNEL_ID_LENGTH
                            + " characters"
            );
        }
    }

    public PlayerData(int strengthLevel) {
        this(strengthLevel, 0L, DEFAULT_MAX_ENERGY, "");
    }

    public PlayerData(int strengthLevel, long energy) {
        this(strengthLevel, energy, DEFAULT_MAX_ENERGY, "");
    }

    public PlayerData withStrengthLevel(int newStrengthLevel) {
        return new PlayerData(newStrengthLevel, energy, maxEnergy, channelId);
    }

    public PlayerData withEnergy(long newEnergy) {
        return new PlayerData(strengthLevel, newEnergy, maxEnergy, channelId);
    }

    public PlayerData withMaxEnergy(long newMaxEnergy) {
        return new PlayerData(strengthLevel, energy, newMaxEnergy, channelId);
    }

    public PlayerData withChannelId(String newChannelId) {
        return new PlayerData(strengthLevel, energy, maxEnergy, newChannelId);
    }
}
