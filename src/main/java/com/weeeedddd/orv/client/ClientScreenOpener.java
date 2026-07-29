package com.weeeedddd.orv.client;

import com.weeeedddd.orv.client.gui.CharacterInfoScreen;
import com.weeeedddd.orv.client.gui.GuildScreen;
import com.weeeedddd.orv.network.OpenScreenPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Opens the requested screen on the client main thread. */
public final class ClientScreenOpener {

    private ClientScreenOpener() {
    }

    public static void handle(
            OpenScreenPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null) {
                return;
            }
            switch (payload.target()) {
                case STATUS -> minecraft.setScreen(new CharacterInfoScreen());
                case GUILD -> minecraft.setScreen(new GuildScreen());
            }
        });
    }
}
