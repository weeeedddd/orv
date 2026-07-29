package com.weeeedddd.orv.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.weeeedddd.orv.OrvMod;
import com.weeeedddd.orv.client.gui.CharacterInfoScreen;
import com.weeeedddd.orv.client.gui.GuildScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = OrvMod.MOD_ID, value = Dist.CLIENT)
public final class ModKeybindings {
    private static final KeyMapping OPEN_GUILD = new KeyMapping(
            "key.orv.open_guild",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.orv"
    );

    private static final KeyMapping OPEN_CHARACTER = new KeyMapping(
            "key.orv.open_character",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.orv"
    );

    private ModKeybindings() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GUILD);
        event.register(OPEN_CHARACTER);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        while (OPEN_GUILD.consumeClick()) {
            if (minecraft.player != null
                    && minecraft.level != null
                    && !(minecraft.screen instanceof GuildScreen)) {
                minecraft.setScreen(new GuildScreen());
            }
        }

        while (OPEN_CHARACTER.consumeClick()) {
            if (minecraft.player != null
                    && minecraft.level != null
                    && !(minecraft.screen instanceof CharacterInfoScreen)) {
                minecraft.setScreen(new CharacterInfoScreen());
            }
        }
    }
}
