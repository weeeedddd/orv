package com.weeeedddd.orv.sponsor;

import com.weeeedddd.orv.data.ModAttachments;
import com.weeeedddd.orv.network.ModNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Server-authoritative application service for sponsor state.
 */
public final class SponsorService {
    private SponsorService() {
    }

    public static IPlayerSponsor get(Player player) {
        return ModAttachments.getSponsor(player);
    }

    public static IPlayerSponsor selectConstellation(
            ServerPlayer player,
            String constellationName
    ) {
        IPlayerSponsor updated = update(
                player,
                sponsor -> sponsor.withConstellationName(
                        constellationName
                )
        );
        ModNetworking.syncSystemData(player);
        return updated;
    }

    public static IPlayerSponsor activateStigma(
            ServerPlayer player,
            ResourceLocation stigmaId
    ) {
        Objects.requireNonNull(stigmaId, "stigmaId");
        return update(
                player,
                sponsor -> sponsor.withActiveStigma(stigmaId)
        );
    }

    public static IPlayerSponsor deactivateStigma(
            ServerPlayer player,
            ResourceLocation stigmaId
    ) {
        Objects.requireNonNull(stigmaId, "stigmaId");
        return update(
                player,
                sponsor -> sponsor.withoutActiveStigma(stigmaId)
        );
    }

    public static IPlayerSponsor setProbability(
            ServerPlayer player,
            long probability
    ) {
        return update(
                player,
                sponsor -> sponsor.withProbability(probability)
        );
    }

    private static IPlayerSponsor update(
            ServerPlayer player,
            UnaryOperator<IPlayerSponsor> mutation
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(mutation, "mutation");

        IPlayerSponsor updated = Objects.requireNonNull(
                mutation.apply(get(player)),
                "updated sponsor"
        );
        player.setData(
                ModAttachments.PLAYER_SPONSOR,
                updated
        );
        return updated;
    }
}
