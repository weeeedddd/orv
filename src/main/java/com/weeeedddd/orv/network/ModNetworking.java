package com.weeeedddd.orv.network;

import com.weeeedddd.orv.OrvMod;
import com.weeeedddd.orv.client.guild.GuildClientState;
import com.weeeedddd.orv.client.system.ClientSystemPayloadHandler;
import com.weeeedddd.orv.data.ModAttachments;
import com.weeeedddd.orv.data.PlayerData;
import com.weeeedddd.orv.economy.CoinService;
import com.weeeedddd.orv.guild.GuildService;
import com.weeeedddd.orv.network.GuildPayloads.GuildRoleActionPayload;
import com.weeeedddd.orv.network.GuildPayloads.GuildScreenDataPayload;
import com.weeeedddd.orv.network.GuildPayloads.RequestGuildDataPayload;
import com.weeeedddd.orv.network.GuildPayloads.SendGuildInvitePayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private static final String NETWORK_VERSION = "2";

    private ModNetworking() {
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(
                SyncSystemDataPayload.TYPE,
                SyncSystemDataPayload.STREAM_CODEC,
                ClientSystemPayloadHandler::handle
        );
        registrar.playToClient(
                SyncPlayerStatsPayload.TYPE,
                SyncPlayerStatsPayload.STREAM_CODEC,
                ModNetworking::handleSyncPlayerStats
        );
        registrar.playToServer(
                RequestGuildDataPayload.TYPE,
                RequestGuildDataPayload.STREAM_CODEC,
                ModNetworking::handleGuildDataRequest
        );
        registrar.playToClient(
                GuildScreenDataPayload.TYPE,
                GuildScreenDataPayload.STREAM_CODEC,
                ModNetworking::handleGuildScreenData
        );
        registrar.playToServer(
                SendGuildInvitePayload.TYPE,
                SendGuildInvitePayload.STREAM_CODEC,
                ModNetworking::handleGuildInvite
        );
        registrar.playToServer(
                GuildRoleActionPayload.TYPE,
                GuildRoleActionPayload.STREAM_CODEC,
                ModNetworking::handleGuildRoleAction
        );
    }

    public static void syncPlayerData(ServerPlayer player) {
        syncSystemData(player);
        syncPlayerStats(player);
    }

    public static void syncSystemData(ServerPlayer player) {
        PlayerData data = ModAttachments.get(player);
        PacketDistributor.sendToPlayer(
                player,
                new SyncSystemDataPayload(
                        player.getUUID(),
                        CoinService.getCoins(player),
                        data.energy(),
                        data.maxEnergy(),
                        data.channelId(),
                        ModAttachments.getSponsor(player)
                                .constellationName()
                )
        );
    }

    public static void syncPlayerStats(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
                player,
                new SyncPlayerStatsPayload(
                        ModAttachments.getStrengthLevel(player)
                )
        );
    }

    private static void handleSyncPlayerStats(
            SyncPlayerStatsPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            context.player().setData(
                    ModAttachments.PLAYER_DATA,
                    ModAttachments.get(context.player())
                            .withStrengthLevel(
                                    payload.strengthLevel()
                            )
            );
        });
    }

    public static void sendGuildSnapshot(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
                player,
                new GuildScreenDataPayload(
                        GuildService.snapshotFor(player)
                )
        );
    }

    private static void handleGuildDataRequest(
            RequestGuildDataPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                sendGuildSnapshot(player);
            }
        });
    }

    private static void handleGuildScreenData(
            GuildScreenDataPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() ->
                GuildClientState.accept(payload.snapshot())
        );
    }

    private static void handleGuildInvite(
            SendGuildInvitePayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }

            GuildService.sendInvite(
                    sender,
                    payload.targetPlayerId(),
                    payload.targetPlayerName(),
                    payload.note()
            );
            sendGuildSnapshot(sender);
        });
    }

    private static void handleGuildRoleAction(
            GuildRoleActionPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer actor)) {
                return;
            }

            ServerPlayer target = actor.getServer()
                    .getPlayerList()
                    .getPlayer(payload.targetPlayerId());

            boolean changed = GuildService.applyRoleAction(
                    actor,
                    payload.targetPlayerId(),
                    payload.action()
            );

            if (!changed) {
                sendGuildSnapshot(actor);
                return;
            }

            for (ServerPlayer member : GuildService.onlineGuildMembers(
                    actor.getServer(),
                    actor.getUUID()
            )) {
                sendGuildSnapshot(member);
            }

            if (target != null) {
                sendGuildSnapshot(target);
            }
        });
    }

    public record SyncPlayerStatsPayload(int strengthLevel)
            implements CustomPacketPayload {
        public static final Type<SyncPlayerStatsPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        OrvMod.MOD_ID,
                        "sync_player_stats"
                )
        );

        public static final StreamCodec<ByteBuf, SyncPlayerStatsPayload>
                STREAM_CODEC = StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        SyncPlayerStatsPayload::strengthLevel,
                        SyncPlayerStatsPayload::new
                );

        public SyncPlayerStatsPayload {
            if (strengthLevel < 0) {
                throw new IllegalArgumentException(
                        "strengthLevel must not be negative"
                );
            }
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
