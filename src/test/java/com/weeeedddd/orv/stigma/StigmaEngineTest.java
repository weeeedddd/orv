package com.weeeedddd.orv.stigma;

import com.weeeedddd.orv.OrvMod;
import com.weeeedddd.orv.sponsor.IPlayerSponsor;
import com.weeeedddd.orv.sponsor.PlayerSponsor;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
class StigmaEngineTest {
    private static final ResourceLocation STIGMA_ID =
            ResourceLocation.fromNamespaceAndPath(
                    OrvMod.MOD_ID,
                    "recovery"
            );
    private static final ResourceLocation EFFECT_ID =
            ResourceLocation.fromNamespaceAndPath(
                    OrvMod.MOD_ID,
                    "heal"
            );
    private static final long NOW = 1_000L;

    private Object player;
    private StigmaDefinition definition;
    private StigmaEffectRegistry<Object> effects;
    private InMemoryStateAccess states;
    private AtomicInteger effectCalls;
    private AtomicReference<StigmaExecutionContext<Object>>
            effectContext;

    @BeforeEach
    void setUp() {
        player = new Object();
        definition = new StigmaDefinition(
                "Recovery",
                30L,
                200,
                EFFECT_ID
        );
        effects = new StigmaEffectRegistry<>();
        states = new InMemoryStateAccess(new StigmaPlayerState(
                new PlayerSponsor(
                        "Demon-like Judge of Fire",
                        Set.of(STIGMA_ID),
                        777L,
                        Map.of()
                ),
                100L
        ));
        effectCalls = new AtomicInteger();
        effectContext = new AtomicReference<>();
        effects.register(EFFECT_ID, context -> {
            effectCalls.incrementAndGet();
            effectContext.set(context);
        });
    }

    @Test
    void executesAnOwnedStigmaAndCommitsManaAndCooldown() {
        StigmaEngine<Object> engine = engineWith(
                (target, stigmaId) -> Optional.of(definition),
                NOW
        );

        StigmaExecutionResult result =
                engine.execute(player, STIGMA_ID);

        assertEquals(StigmaExecutionStatus.SUCCESS, result.status());
        assertEquals(0L, result.remainingCooldownTicks());
        assertEquals(1, effectCalls.get());
        assertSame(player, effectContext.get().player());
        assertEquals(STIGMA_ID, effectContext.get().stigmaId());
        assertEquals(777L, effectContext.get().probability());
        assertEquals(70L, states.state.energy());
        assertEquals(
                200L,
                states.state.sponsor()
                        .cooldownRemaining(STIGMA_ID, NOW)
        );
        assertEquals(1, states.commitCount);
    }

    @Test
    void rejectsAStigmaThatIsNotActiveForThePlayer() {
        states.state = new StigmaPlayerState(
                PlayerSponsor.DEFAULT,
                100L
        );
        StigmaEngine<Object> engine = engineWith(
                (target, stigmaId) -> Optional.of(definition),
                NOW
        );

        StigmaExecutionResult result =
                engine.execute(player, STIGMA_ID);

        assertEquals(
                StigmaExecutionStatus.STIGMA_NOT_ACTIVE,
                result.status()
        );
        assertUnchanged();
    }

    @Test
    void rejectsAnUnknownStigmaDefinition() {
        StigmaEngine<Object> engine = engineWith(
                (target, stigmaId) -> Optional.empty(),
                NOW
        );

        StigmaExecutionResult result =
                engine.execute(player, STIGMA_ID);

        assertEquals(
                StigmaExecutionStatus.STIGMA_NOT_FOUND,
                result.status()
        );
        assertUnchanged();
    }

    @Test
    void rejectsAnUnregisteredEffectLogic() {
        StigmaDefinition missingEffect = new StigmaDefinition(
                "Missing",
                0L,
                0,
                ResourceLocation.fromNamespaceAndPath(
                        OrvMod.MOD_ID,
                        "missing"
                )
        );
        StigmaEngine<Object> engine = engineWith(
                (target, stigmaId) -> Optional.of(missingEffect),
                NOW
        );

        StigmaExecutionResult result =
                engine.execute(player, STIGMA_ID);

        assertEquals(
                StigmaExecutionStatus.EFFECT_NOT_REGISTERED,
                result.status()
        );
        assertUnchanged();
    }

    @Test
    void rejectsInsufficientManaWithoutCallingTheEffect() {
        states.state = new StigmaPlayerState(
                states.state.sponsor(),
                29L
        );
        StigmaEngine<Object> engine = engineWith(
                (target, stigmaId) -> Optional.of(definition),
                NOW
        );

        StigmaExecutionResult result =
                engine.execute(player, STIGMA_ID);

        assertEquals(
                StigmaExecutionStatus.INSUFFICIENT_MANA,
                result.status()
        );
        assertEquals(29L, states.state.energy());
        assertUnchanged();
    }

    @Test
    void reportsTheRemainingCooldownWithoutMutatingState() {
        states.state = new StigmaPlayerState(
                states.state.sponsor()
                        .withCooldownUntil(STIGMA_ID, NOW + 75L),
                100L
        );
        StigmaEngine<Object> engine = engineWith(
                (target, stigmaId) -> Optional.of(definition),
                NOW
        );

        StigmaExecutionResult result =
                engine.execute(player, STIGMA_ID);

        assertEquals(
                StigmaExecutionStatus.ON_COOLDOWN,
                result.status()
        );
        assertEquals(75L, result.remainingCooldownTicks());
        assertUnchanged();
    }

    @Test
    void rollsBackResourceStateWhenEffectLogicFails() {
        effects = new StigmaEffectRegistry<>();
        effects.register(EFFECT_ID, context -> {
            effectCalls.incrementAndGet();
            throw new IllegalStateException("effect failed");
        });
        StigmaPlayerState original = states.state;
        StigmaEngine<Object> engine = engineWith(
                (target, stigmaId) -> Optional.of(definition),
                NOW
        );

        StigmaExecutionResult result =
                engine.execute(player, STIGMA_ID);

        assertEquals(
                StigmaExecutionStatus.EXECUTION_FAILED,
                result.status()
        );
        assertEquals(1, effectCalls.get());
        assertEquals(original, states.state);
        assertEquals(0, states.commitCount);
    }

    @Test
    void rejectsCooldownOverflowBeforeCallingTheEffect() {
        StigmaEngine<Object> engine = engineWith(
                (target, stigmaId) -> Optional.of(definition),
                Long.MAX_VALUE
        );

        StigmaExecutionResult result =
                engine.execute(player, STIGMA_ID);

        assertEquals(
                StigmaExecutionStatus.EXECUTION_FAILED,
                result.status()
        );
        assertUnchanged();
    }

    @Test
    void allowsZeroCostAndZeroCooldown() {
        StigmaDefinition free = new StigmaDefinition(
                "Free",
                0L,
                0,
                EFFECT_ID
        );
        states.state = new StigmaPlayerState(
                states.state.sponsor(),
                0L
        );
        StigmaEngine<Object> engine = engineWith(
                (target, stigmaId) -> Optional.of(free),
                NOW
        );

        StigmaExecutionResult result =
                engine.execute(player, STIGMA_ID);

        assertEquals(StigmaExecutionStatus.SUCCESS, result.status());
        assertEquals(0L, states.state.energy());
        assertEquals(1, effectCalls.get());
    }

    @Test
    void effectRegistryRejectsDuplicateLogicIds() {
        assertThrows(
                IllegalStateException.class,
                () -> effects.register(EFFECT_ID, context -> {
                })
        );
    }

    private StigmaEngine<Object> engineWith(
            StigmaDefinitionResolver<Object> definitions,
            long currentTick
    ) {
        return new StigmaEngine<>(
                definitions,
                effects,
                states,
                target -> currentTick
        );
    }

    private void assertUnchanged() {
        assertEquals(0, effectCalls.get());
        assertEquals(0, states.commitCount);
    }

    private static final class InMemoryStateAccess
            implements StigmaPlayerStateAccess<Object> {
        private StigmaPlayerState state;
        private int commitCount;

        private InMemoryStateAccess(StigmaPlayerState initialState) {
            state = initialState;
        }

        @Override
        public StigmaPlayerState load(Object player) {
            return state;
        }

        @Override
        public void commit(
                Object player,
                StigmaPlayerState updatedState
        ) {
            state = updatedState;
            commitCount++;
        }
    }
}
