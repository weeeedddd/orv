package com.weeeedddd.orv;

import com.weeeedddd.orv.command.OrvCommands;
import com.weeeedddd.orv.data.ModAttachments;
import com.weeeedddd.orv.data.PlayerDataEvents;
import com.weeeedddd.orv.network.ModNetworking;
import com.weeeedddd.orv.stigma.ModStigmaRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(OrvMod.MOD_ID)
public final class OrvMod {
    public static final String MOD_ID = "orv";

    public OrvMod(IEventBus modEventBus) {
        ModAttachments.register(modEventBus);
        modEventBus.addListener(ModNetworking::registerPayloadHandlers);
        modEventBus.addListener(ModStigmaRegistries::register);

        NeoForge.EVENT_BUS.addListener(PlayerDataEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(PlayerDataEvents::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(PlayerDataEvents::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(OrvCommands::onRegisterCommands);
    }
}
