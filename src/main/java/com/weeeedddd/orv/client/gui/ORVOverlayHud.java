package com.weeeedddd.orv.client.gui;

import com.weeeedddd.orv.OrvMod;
import com.weeeedddd.orv.client.system.SystemDataClientCache;
import com.weeeedddd.orv.client.system.SystemDataSnapshot;
import com.weeeedddd.orv.data.ModAttachments;
import com.weeeedddd.orv.data.PlayerData;
import net.minecraft.Util;
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
    private static final int PANEL_UNDERLAY = 0x550B0E14;
    private static final int ACCENT_CYAN = 0xFF00E5FF;
    private static final int COINS_GOLD = 0xFFFFD700;
    private static final int ENERGY_BLUE = 0xFF55FFFF;
    private static final int STRENGTH_RED = 0xFFFF5555;
    private static final int CONSTELLATION_PURPLE = 0xFFAA55FF;

    private static final int PANEL_X = 12;
    private static final int PANEL_Y = 12;
    private static final int MIN_PANEL_WIDTH = 248;
    private static final int PANEL_HEIGHT = 110;

    /** Offset of the parallax plane sitting behind the main panel. */
    private static final int UNDERLAY_OFFSET = 4;
    private static final int SEAL_INSET = 8;
    private static final int HEADER_TOP = 6;
    private static final int DIVIDER_Y = 34;
    private static final int ROW_TOP = 42;
    private static final int ROW_STEP = 15;
    private static final int ICON_X = 12;
    private static final int TEXT_X = 34;
    private static final int MOTE_COUNT = 14;

    private static final int[] MOTE_COLORS = {
            ACCENT_CYAN, COINS_GOLD, CONSTELLATION_PURPLE
    };

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
                + (systemData == null ? 0L : systemData.coins());
        String energyText = "Energy: "
                + (systemData == null ? 0L : systemData.energy());
        String strengthText = "Strength: Lv. " + data.strengthLevel();
        boolean hasConstellation = systemData != null
                && !systemData.constellationName().isBlank();
        String constellationName = hasConstellation
                ? systemData.constellationName()
                : "None";
        String constellationText = "Constellation: " + constellationName;

        int rowsWidth = font.width(coinsText);
        rowsWidth = Math.max(rowsWidth, font.width(energyText));
        rowsWidth = Math.max(rowsWidth, font.width(strengthText));
        rowsWidth = Math.max(rowsWidth, font.width(constellationText));

        // The header must fit a seal on each side of the title.
        int headerWidth = SEAL_INSET * 2 + StatusAtlas.SEAL_SIZE * 2
                + 16 + font.width(title);
        int panelWidth = Math.max(
                MIN_PANEL_WIDTH,
                // The right margin keeps long values clear of the frame's
                // corner rosettes.
                Math.max(headerWidth, TEXT_X + rowsWidth + 18)
        );

        renderPanel(guiGraphics, panelWidth);
        renderHeader(guiGraphics, font, title, panelWidth);
        renderRows(
                guiGraphics,
                font,
                coinsText,
                energyText,
                strengthText,
                constellationText,
                hasConstellation
        );
        renderMotes(guiGraphics, panelWidth);
    }

    /** Layered translucent planes with the filigree border on top. */
    private static void renderPanel(
            GuiGraphics guiGraphics,
            int panelWidth
    ) {
        // Parallax plane peeking out at the bottom right.
        guiGraphics.fill(
                PANEL_X + UNDERLAY_OFFSET,
                PANEL_Y + UNDERLAY_OFFSET,
                PANEL_X + panelWidth + UNDERLAY_OFFSET,
                PANEL_Y + PANEL_HEIGHT + UNDERLAY_OFFSET,
                PANEL_UNDERLAY
        );
        guiGraphics.fill(
                PANEL_X,
                PANEL_Y,
                PANEL_X + panelWidth,
                PANEL_Y + PANEL_HEIGHT,
                PANEL_BACKGROUND
        );

        // Scenario-path labyrinth, kept faint so it never fights the text.
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 0.18F);
        StatusAtlas.labyrinth(
                guiGraphics,
                PANEL_X + 6,
                PANEL_Y + DIVIDER_Y + 4,
                panelWidth - 12,
                PANEL_HEIGHT - DIVIDER_Y - 10
        );
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        StatusAtlas.frame(
                guiGraphics,
                PANEL_X,
                PANEL_Y,
                panelWidth,
                PANEL_HEIGHT
        );
    }

    private static void renderHeader(
            GuiGraphics guiGraphics,
            Font font,
            String title,
            int panelWidth
    ) {
        int sealY = PANEL_Y + HEADER_TOP;
        StatusAtlas.seal(guiGraphics, PANEL_X + SEAL_INSET, sealY);
        StatusAtlas.seal(
                guiGraphics,
                PANEL_X + panelWidth - SEAL_INSET - StatusAtlas.SEAL_SIZE,
                sealY
        );

        guiGraphics.drawString(
                font,
                title,
                PANEL_X + (panelWidth - font.width(title)) / 2,
                sealY + (StatusAtlas.SEAL_SIZE - font.lineHeight) / 2,
                ACCENT_CYAN,
                false
        );

        int dividerY = PANEL_Y + DIVIDER_Y;
        guiGraphics.fill(
                PANEL_X + 10,
                dividerY,
                PANEL_X + panelWidth - 10,
                dividerY + 1,
                ACCENT_CYAN
        );
        // Horn-and-eye motifs mark the junctions at each end of the rule.
        StatusAtlas.horn(guiGraphics, PANEL_X + 3, dividerY - 4);
        StatusAtlas.horn(
                guiGraphics,
                PANEL_X + panelWidth - 3 - StatusAtlas.HORN_SIZE,
                dividerY - 4
        );
    }

    private static void renderRows(
            GuiGraphics guiGraphics,
            Font font,
            String coinsText,
            String energyText,
            String strengthText,
            String constellationText,
            boolean hasConstellation
    ) {
        row(guiGraphics, font, 0, coinsText, COINS_GOLD,
                StatusAtlas.ICON_COIN_U);
        row(guiGraphics, font, 1, energyText, ENERGY_BLUE,
                StatusAtlas.ICON_ENERGY_U);
        row(guiGraphics, font, 2, strengthText, STRENGTH_RED,
                StatusAtlas.ICON_SWORD_U);

        int starY = rowY(3) - 4;
        StatusAtlas.star(
                guiGraphics,
                PANEL_X + ICON_X,
                starY,
                hasConstellation
        );
        guiGraphics.drawString(
                font,
                constellationText,
                PANEL_X + TEXT_X,
                rowY(3),
                CONSTELLATION_PURPLE,
                true
        );
    }

    private static void row(
            GuiGraphics guiGraphics,
            Font font,
            int index,
            String text,
            int color,
            int iconU
    ) {
        int y = rowY(index);
        // Icons are 16px tall against 9px text, so lift them to share a
        // centre line with the label.
        StatusAtlas.icon(guiGraphics, iconU, PANEL_X + ICON_X, y - 4);
        guiGraphics.drawString(
                font,
                text,
                PANEL_X + TEXT_X,
                y,
                color,
                true
        );
    }

    private static int rowY(int index) {
        return PANEL_Y + ROW_TOP + index * ROW_STEP;
    }

    /** Cyan, gold and violet motes drifting along the panel edges. */
    private static void renderMotes(
            GuiGraphics guiGraphics,
            int panelWidth
    ) {
        long millis = Util.getMillis();

        for (int index = 0; index < MOTE_COUNT; index++) {
            double speed = 0.07 + (index % 4) * 0.015;
            double phase = ((millis / 1000.0) * speed + index * 0.41) % 1.0;

            int alpha = (int) (Math.sin(phase * Math.PI) * 120.0);
            if (alpha <= 6) {
                continue;
            }

            boolean rightSide = index % 2 == 1;
            double wobble = Math.sin(millis / 900.0 + index * 1.7) * 3.0;
            int x = rightSide
                    ? PANEL_X + panelWidth - 5 + (int) wobble
                    : PANEL_X + 4 + (int) wobble;
            // Motes rise along the panel edge over their lifetime.
            int y = PANEL_Y + PANEL_HEIGHT - 6
                    - (int) (phase * (PANEL_HEIGHT - 12));

            int size = 1 + (index % 2);
            int color = MOTE_COLORS[index % MOTE_COLORS.length];
            guiGraphics.fill(
                    x,
                    y,
                    x + size,
                    y + size,
                    (alpha << 24) | (color & 0x00FFFFFF)
            );
        }
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
