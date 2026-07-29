package com.weeeedddd.orv.client.system;

import com.weeeedddd.orv.OrvMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = OrvMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSystemEvents {
    private ClientSystemEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        SystemDataClientCache.clear();
    }
}
