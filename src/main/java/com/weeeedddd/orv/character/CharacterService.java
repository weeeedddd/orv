package com.weeeedddd.orv.character;

import com.weeeedddd.orv.data.ModAttachments;
import com.weeeedddd.orv.network.SyncCharacterProfilePayload;
import com.weeeedddd.orv.sponsor.IPlayerSponsor;
import com.weeeedddd.orv.stigma.RegistryStigmaDefinitionResolver;
import com.weeeedddd.orv.stigma.StigmaDefinition;
import com.weeeedddd.orv.stigma.StigmaDefinitionResolver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Server-side API for the character sheet. Every mutation goes through here
 * so the client is resynced immediately afterwards.
 */
public final class CharacterService {

    private static final StigmaDefinitionResolver<ServerPlayer> STIGMA_RESOLVER =
            new RegistryStigmaDefinitionResolver();

    private CharacterService() {
    }

    public static CharacterProfile get(ServerPlayer player) {
        return ModAttachments.getCharacterProfile(player);
    }

    public static void setAge(ServerPlayer player, int age) {
        update(player, get(player).withAge(age));
    }

    public static void grantSkill(
            ServerPlayer player,
            String name,
            int level,
            boolean stolen
    ) {
        update(
                player,
                get(player).withSkill(
                        new CharacterProfile.Skill(name, level, stolen)
                )
        );
    }

    public static void removeSkill(ServerPlayer player, String name) {
        update(player, get(player).withoutSkill(name));
    }

    public static void grantAttribute(
            ServerPlayer player,
            String name,
            CharacterProfile.Rarity rarity
    ) {
        update(
                player,
                get(player).withAttribute(
                        new CharacterProfile.Attribute(name, rarity)
                )
        );
    }

    public static void removeAttribute(ServerPlayer player, String name) {
        update(player, get(player).withoutAttribute(name));
    }

    private static void update(ServerPlayer player, CharacterProfile profile) {
        ModAttachments.setCharacterProfile(player, profile);
        sync(player);
    }

    /** Sends the full sheet, with stigmas resolved to display rows. */
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
                player,
                new SyncCharacterProfilePayload(
                        player.getUUID(),
                        player.getGameProfile().getName(),
                        ModAttachments.getStrengthLevel(player),
                        get(player),
                        resolveStigmas(player)
                )
        );
    }

    /**
     * Turns the sponsor's stigma ids into display rows. Stigmas carry no
     * level of their own, so they render at level 1; ones whose definition
     * is missing from the registry are skipped rather than shown as raw ids.
     */
    private static List<CharacterProfile.Skill> resolveStigmas(
            ServerPlayer player
    ) {
        IPlayerSponsor sponsor = ModAttachments.getSponsor(player);
        List<CharacterProfile.Skill> rows = new ArrayList<>();

        for (ResourceLocation stigmaId : sponsor.activeStigmas()) {
            Optional<StigmaDefinition> definition =
                    STIGMA_RESOLVER.find(player, stigmaId);
            definition.ifPresent(stigma -> rows.add(
                    new CharacterProfile.Skill(stigma.name(), 1, false)
            ));
        }

        rows.sort(Comparator.comparing(
                CharacterProfile.Skill::name,
                String.CASE_INSENSITIVE_ORDER
        ));
        return rows;
    }
}
