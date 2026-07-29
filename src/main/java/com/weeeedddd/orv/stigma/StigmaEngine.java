package com.weeeedddd.orv.stigma;

import com.weeeedddd.orv.sponsor.IPlayerSponsor;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.function.ToLongFunction;

/**
 * Server-side API for validated, transactional stigma execution.
 */
public final class StigmaEngine<P> {
    private final StigmaDefinitionResolver<P> definitions;
    private final StigmaEffectRegistry<P> effects;
    private final StigmaPlayerStateAccess<P> states;
    private final ToLongFunction<P> gameClock;

    public StigmaEngine(
            StigmaDefinitionResolver<P> definitions,
            StigmaEffectRegistry<P> effects,
            StigmaPlayerStateAccess<P> states,
            ToLongFunction<P> gameClock
    ) {
        this.definitions = Objects.requireNonNull(
                definitions,
                "definitions"
        );
        this.effects = Objects.requireNonNull(effects, "effects");
        this.states = Objects.requireNonNull(states, "states");
        this.gameClock = Objects.requireNonNull(
                gameClock,
                "gameClock"
        );
    }

    public StigmaExecutionResult execute(
            P player,
            ResourceLocation stigmaId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(stigmaId, "stigmaId");

        Optional<StigmaDefinition> resolvedDefinition =
                Objects.requireNonNull(
                        definitions.find(player, stigmaId),
                        "resolved definition"
                );
        if (resolvedDefinition.isEmpty()) {
            return StigmaExecutionResult.of(
                    StigmaExecutionStatus.STIGMA_NOT_FOUND
            );
        }
        StigmaDefinition definition = resolvedDefinition.orElseThrow();

        StigmaPlayerState currentState = Objects.requireNonNull(
                states.load(player),
                "player state"
        );
        IPlayerSponsor sponsor = currentState.sponsor();
        if (!sponsor.hasActiveStigma(stigmaId)) {
            return StigmaExecutionResult.of(
                    StigmaExecutionStatus.STIGMA_NOT_ACTIVE
            );
        }

        Optional<StigmaEffectLogic<P>> resolvedEffect =
                effects.find(definition.effectLogic());
        if (resolvedEffect.isEmpty()) {
            return StigmaExecutionResult.of(
                    StigmaExecutionStatus.EFFECT_NOT_REGISTERED
            );
        }

        long currentGameTick = gameClock.applyAsLong(player);
        if (currentGameTick < 0L) {
            return StigmaExecutionResult.of(
                    StigmaExecutionStatus.EXECUTION_FAILED
            );
        }
        long remainingCooldown = sponsor.cooldownRemaining(
                stigmaId,
                currentGameTick
        );
        if (remainingCooldown > 0L) {
            return StigmaExecutionResult.onCooldown(
                    remainingCooldown
            );
        }
        if (currentState.energy() < definition.manaCost()) {
            return StigmaExecutionResult.of(
                    StigmaExecutionStatus.INSUFFICIENT_MANA
            );
        }

        long readyAtGameTick;
        try {
            readyAtGameTick = Math.addExact(
                    currentGameTick,
                    definition.cooldownTicks()
            );
        } catch (ArithmeticException exception) {
            return StigmaExecutionResult.of(
                    StigmaExecutionStatus.EXECUTION_FAILED
            );
        }

        IPlayerSponsor updatedSponsor = sponsor.withCooldownUntil(
                stigmaId,
                readyAtGameTick
        );
        StigmaPlayerState updatedState = new StigmaPlayerState(
                updatedSponsor,
                currentState.energy() - definition.manaCost()
        );
        StigmaExecutionContext<P> context =
                new StigmaExecutionContext<>(
                        player,
                        stigmaId,
                        definition,
                        sponsor.probability()
                );

        try {
            resolvedEffect.orElseThrow().apply(context);
        } catch (RuntimeException exception) {
            return StigmaExecutionResult.of(
                    StigmaExecutionStatus.EXECUTION_FAILED
            );
        }

        states.commit(player, updatedState);
        return StigmaExecutionResult.of(
                StigmaExecutionStatus.SUCCESS
        );
    }
}
