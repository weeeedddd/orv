package com.weeeedddd.orv.stigma;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Data-driven description of a stigma and its server effect binding.
 */
public record StigmaDefinition(
        String name,
        long manaCost,
        int cooldownTicks,
        ResourceLocation effectLogic
) {
    public static final int MAX_NAME_LENGTH = 64;

    public static final Codec<StigmaDefinition> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("name")
                            .forGetter(StigmaDefinition::name),
                    Codec.LONG.fieldOf("mana_cost")
                            .forGetter(StigmaDefinition::manaCost),
                    Codec.INT.fieldOf("cooldown_ticks")
                            .forGetter(
                                    StigmaDefinition::cooldownTicks
                            ),
                    ResourceLocation.CODEC.fieldOf("effect_logic")
                            .forGetter(
                                    StigmaDefinition::effectLogic
                            )
            ).apply(instance, StigmaDefinition::new));

    public StigmaDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(effectLogic, "effectLogic");

        name = name.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException(
                    "name must not be blank"
            );
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "name exceeds " + MAX_NAME_LENGTH + " characters"
            );
        }
        if (manaCost < 0L) {
            throw new IllegalArgumentException(
                    "manaCost must not be negative"
            );
        }
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException(
                    "cooldownTicks must not be negative"
            );
        }
    }
}
