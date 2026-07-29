package com.weeeedddd.orv.stigma;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.weeeedddd.orv.OrvMod;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StigmaDefinitionTest {
    @Test
    void decodesADataDrivenDefinition() {
        JsonObject json = JsonParser.parseString("""
                {
                  "name": "Recovery",
                  "mana_cost": 20,
                  "cooldown_ticks": 100,
                  "effect_logic": "orv:heal"
                }
                """).getAsJsonObject();

        StigmaDefinition definition = StigmaDefinition.CODEC
                .parse(JsonOps.INSTANCE, json)
                .getOrThrow();

        assertEquals("Recovery", definition.name());
        assertEquals(20L, definition.manaCost());
        assertEquals(100, definition.cooldownTicks());
        assertEquals(
                ResourceLocation.fromNamespaceAndPath(
                        OrvMod.MOD_ID,
                        "heal"
                ),
                definition.effectLogic()
        );
    }

    @Test
    void roundTripsThroughTheDefinitionCodec() {
        StigmaDefinition original = new StigmaDefinition(
                "Demon King Transformation",
                250L,
                1_200,
                ResourceLocation.fromNamespaceAndPath(
                        OrvMod.MOD_ID,
                        "demon_king_transformation"
                )
        );

        Object encoded = StigmaDefinition.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        StigmaDefinition restored = StigmaDefinition.CODEC
                .parse(JsonOps.INSTANCE, (com.google.gson.JsonElement) encoded)
                .getOrThrow();

        assertEquals(original, restored);
    }

    @Test
    void rejectsInvalidDefinitionValues() {
        ResourceLocation effectLogic =
                ResourceLocation.fromNamespaceAndPath(
                        OrvMod.MOD_ID,
                        "heal"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> new StigmaDefinition(
                        "",
                        0L,
                        0,
                        effectLogic
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new StigmaDefinition(
                        "x".repeat(
                                StigmaDefinition.MAX_NAME_LENGTH + 1
                        ),
                        0L,
                        0,
                        effectLogic
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new StigmaDefinition(
                        "Recovery",
                        -1L,
                        0,
                        effectLogic
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new StigmaDefinition(
                        "Recovery",
                        0L,
                        -1,
                        effectLogic
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new StigmaDefinition(
                        "Recovery",
                        0L,
                        0,
                        null
                )
        );
    }

    @Test
    void usesTheStigmaDatapackRegistryId() {
        assertEquals(
                ResourceLocation.fromNamespaceAndPath(
                        OrvMod.MOD_ID,
                        "stigma"
                ),
                ModStigmaRegistries.STIGMA_REGISTRY_KEY.location()
        );
    }
}
