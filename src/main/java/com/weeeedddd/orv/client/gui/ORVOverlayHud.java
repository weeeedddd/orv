package com.weeeedddd.orv.client.gui;

import com.weeeedddd.orv.OrvMod;
import com.weeeedddd.orv.client.system.SystemDataClientCache;
import com.weeeedddd.orv.client.system.SystemDataSnapshot;
import com.weeeedddd.orv.data.ModAttachments;
import com.weeeedddd.orv.data.PlayerData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = OrvMod.MOD_ID, value = Dist.CLIENT)
public final class ORVOverlayHud {
    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(OrvMod.MOD_ID, "status_hud");

    private static final int PANEL_BACKGROUND = 0xCC0B0E14;
    private static final int ACCENT_CYAN = 0xFF00E5FF;
    private static final int COINS_GOLD = 0xFFFFD700;
    private static final int ENERGY_BLUE = 0xFF55FFFF;
    private static final int STRENGTH_RED = 0xFFFF5555;
    private static final int CONSTELLATION_PURPLE = 0xFFAA55FF;

    private static final int PANEL_X = 12;
    private static final int PANEL_Y = 12;
    private static final int MIN_PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 72;

    private ORVOverlayHud() {
    }

    public static void render(
            GuiGraphics guiGraphics,
            DeltaTracker deltaTracker
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        PlayerData data = ModAttachments.get(minecraft.player);
        SystemDataSnapshot systemData = SystemDataClientCache
                .find(minecraft.player.getUUID())
                .orElse(null);
        Font font = minecraft.font;

        String title = "[ SYSTEM STATUS ]";
        String coinsText = "Coins: "
                + (systemData == null ? 0L : systemData.coins())
                + " \u26C3";
        String energyText = "Energy: "
                + (systemData == null ? 0L : systemData.energy());
        String strengthText = "Strength: Lv. " + data.strengthLevel();
        String constellationName = systemData == null
                || systemData.constellationName().isBlank()
                ? "None"
                : systemData.constellationName();
        String constellationText =
                "Constellation: " + constellationName;

        int contentWidth = font.width(title);
        contentWidth = Math.max(contentWidth, font.width(coinsText));
        contentWidth = Math.max(contentWidth, font.width(energyText));
        contentWidth = Math.max(contentWidth, font.width(strengthText));
        contentWidth = Math.max(
                contentWidth,
                font.width(constellationText)
        );
        int panelWidth = Math.max(MIN_PANEL_WIDTH, contentWidth + 16);

        guiGraphics.fill(
                PANEL_X,
                PANEL_Y,
                PANEL_X + panelWidth,
                PANEL_Y + PANEL_HEIGHT,
                PANEL_BACKGROUND
        );
        guiGraphics.renderOutline(
                PANEL_X,
                PANEL_Y,
                panelWidth,
                PANEL_HEIGHT,
                ACCENT_CYAN
        );
        guiGraphics.fill(
                PANEL_X + 2,
                PANEL_Y + 14,
                PANEL_X + panelWidth - 2,
                PANEL_Y + 15,
                ACCENT_CYAN
        );

        guiGraphics.drawString(
                font,
                title,
                PANEL_X + 6,
                PANEL_Y + 4,
                ACCENT_CYAN,
                false
        );
        guiGraphics.drawString(
                font,
                coinsText,
                PANEL_X + 8,
                PANEL_Y + 20,
                COINS_GOLD,
                true
        );
        guiGraphics.drawString(
                font,
                energyText,
                PANEL_X + 8,
                PANEL_Y + 32,
                ENERGY_BLUE,
                true
        );
        guiGraphics.drawString(
                font,
                strengthText,
                PANEL_X + 8,
                PANEL_Y + 44,
                STRENGTH_RED,
                true
        );
        guiGraphics.drawString(
                font,
                constellationText,
                PANEL_X + 8,
                PANEL_Y + 56,
                CONSTELLATION_PURPLE,
                true
        );
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.CHAT,
                LAYER_ID,
                ORVOverlayHud::render
        );
    }
}
