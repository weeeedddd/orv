package com.weeeedddd.orv.stigma;

import com.weeeedddd.orv.data.ModAttachments;
import com.weeeedddd.orv.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class AttachmentStigmaPlayerStateAccess
        implements StigmaPlayerStateAccess<ServerPlayer> {
    @Override
    public StigmaPlayerState load(ServerPlayer player) {
        return new StigmaPlayerState(
                ModAttachments.getSponsor(player),
                ModAttachments.getEnergy(player)
        );
    }

    @Override
    public void commit(
            ServerPlayer player,
            StigmaPlayerState updatedState
    ) {
        player.setData(
                ModAttachments.PLAYER_SPONSOR,
                updatedState.sponsor()
        );
        player.setData(
                ModAttachments.PLAYER_DATA,
                ModAttachments.get(player)
                        .withEnergy(updatedState.energy())
        );
        ModNetworking.syncSystemData(player);
    }
}
