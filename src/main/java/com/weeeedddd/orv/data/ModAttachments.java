package com.weeeedddd.orv.data;

import com.weeeedddd.orv.OrvMod;
import com.weeeedddd.orv.character.CharacterProfile;
import com.weeeedddd.orv.economy.CoinService;
import com.weeeedddd.orv.network.ModNetworking;
import com.weeeedddd.orv.sponsor.IPlayerSponsor;
import com.weeeedddd.orv.sponsor.PlayerSponsor;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, OrvMod.MOD_ID);

    private static final Codec<ICoinData> COIN_DATA_CODEC =
            CoinData.CODEC.xmap(
                    data -> data,
                    data -> new CoinData(data.getCoins())
            );

    private static final Codec<IPlayerSponsor> PLAYER_SPONSOR_CODEC =
            PlayerSponsor.CODEC.xmap(
                    data -> data,
                    PlayerSponsor::copyOf
            );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ICoinData>> COIN_DATA =
            ATTACHMENT_TYPES.register(
                    "coin_data",
                    () -> AttachmentType.<ICoinData>builder(
                                    () -> new CoinData()
                            )
                            .serialize(COIN_DATA_CODEC)
                            .copyOnDeath()
                            .build()
            );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerData>> PLAYER_DATA =
            ATTACHMENT_TYPES.register(
                    "player_data",
                    () -> AttachmentType.builder(() -> PlayerData.DEFAULT)
                            .serialize(PlayerData.CODEC)
                            .copyOnDeath()
                            .build()
            );

    public static final DeferredHolder<
            AttachmentType<?>,
            AttachmentType<IPlayerSponsor>
            > PLAYER_SPONSOR = ATTACHMENT_TYPES.register(
                    "player_sponsor",
                    () -> AttachmentType.<IPlayerSponsor>builder(
                                    () -> PlayerSponsor.DEFAULT
                            )
                            .serialize(PLAYER_SPONSOR_CODEC)
                            .copyOnDeath()
                            .build()
            );

    public static final DeferredHolder<
            AttachmentType<?>,
            AttachmentType<CharacterProfile>
            > CHARACTER_PROFILE = ATTACHMENT_TYPES.register(
                    "character_profile",
                    () -> AttachmentType.builder(
                                    () -> CharacterProfile.DEFAULT
                            )
                            .serialize(CharacterProfile.CODEC)
                            .copyOnDeath()
                            .build()
            );

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static PlayerData get(Player player) {
        return player.getData(PLAYER_DATA);
    }

    public static ICoinData getCoinData(Player player) {
        return player.getData(COIN_DATA);
    }

    public static long getCoins(Player player) {
        return getCoinData(player).getCoins();
    }

    public static int getStrengthLevel(Player player) {
        return get(player).strengthLevel();
    }

    public static long getEnergy(Player player) {
        return get(player).energy();
    }

    public static long getMaxEnergy(Player player) {
        return get(player).maxEnergy();
    }

    public static String getChannelId(Player player) {
        return get(player).channelId();
    }

    public static IPlayerSponsor getSponsor(Player player) {
        return player.getData(PLAYER_SPONSOR);
    }

    public static CharacterProfile getCharacterProfile(Player player) {
        return player.getData(CHARACTER_PROFILE);
    }

    public static void setCharacterProfile(
            ServerPlayer player,
            CharacterProfile profile
    ) {
        player.setData(CHARACTER_PROFILE, profile);
    }

    @Deprecated(forRemoval = false)
    public static void setCoins(ServerPlayer player, long coins) {
        CoinService.setCoins(player, coins);
    }

    @Deprecated(forRemoval = false)
    public static void addCoins(ServerPlayer player, long amount) {
        CoinService.addCoins(player, amount);
    }

    public static void setStrengthLevel(ServerPlayer player, int strengthLevel) {
        player.setData(
                PLAYER_DATA,
                get(player).withStrengthLevel(strengthLevel)
        );
        ModNetworking.syncPlayerStats(player);
    }

    public static void setEnergy(ServerPlayer player, long energy) {
        player.setData(
                PLAYER_DATA,
                get(player).withEnergy(energy)
        );
        ModNetworking.syncSystemData(player);
    }

    public static void setMaxEnergy(ServerPlayer player, long maxEnergy) {
        player.setData(
                PLAYER_DATA,
                get(player).withMaxEnergy(maxEnergy)
        );
        ModNetworking.syncSystemData(player);
    }

    public static void setChannelId(ServerPlayer player, String channelId) {
        player.setData(
                PLAYER_DATA,
                get(player).withChannelId(channelId)
        );
        ModNetworking.syncSystemData(player);
    }
}
