package com.weeeedddd.orv.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PlayerData(int strengthLevel, long energy) {
    public static final PlayerData DEFAULT = new PlayerData(0, 0L);

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("strength_level")
                            .forGetter(PlayerData::strengthLevel),
                    Codec.LONG.optionalFieldOf("energy", 0L)
                            .forGetter(PlayerData::energy)
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
    }

    public PlayerData(int strengthLevel) {
        this(strengthLevel, 0L);
    }

    public PlayerData withStrengthLevel(int newStrengthLevel) {
        return new PlayerData(newStrengthLevel, energy);
    }

    public PlayerData withEnergy(long newEnergy) {
        return new PlayerData(strengthLevel, newEnergy);
    }
}
