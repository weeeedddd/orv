package com.weeeedddd.orv.stigma;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInStigmaResourceTest {
    @Test
    void recoveryDefinitionMatchesARegisteredEffectLogic()
            throws Exception {
        String path =
                "data/orv/orv/stigma/recovery.json";

        try (InputStream stream = getClass()
                .getClassLoader()
                .getResourceAsStream(path)) {
            assertNotNull(stream, () -> "Missing resource: " + path);
            JsonElement json = JsonParser.parseReader(
                    new InputStreamReader(
                            stream,
                            StandardCharsets.UTF_8
                    )
            );
            StigmaDefinition definition = StigmaDefinition.CODEC
                    .parse(JsonOps.INSTANCE, json)
                    .getOrThrow();

            assertEquals("Recovery", definition.name());
            assertEquals(
                    ModStigmaEffects.HEAL,
                    definition.effectLogic()
            );
            assertTrue(
                    ModStigmaEffects.registry()
                            .find(definition.effectLogic())
                            .isPresent()
            );
        }
    }
}
