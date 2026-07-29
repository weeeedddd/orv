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

import java.util.ArrayList;
import java.util.List;

/**
 * The ORV system bar: an ornate strip across the top of the screen carrying
 * the channel tag, coins, strength, energy and the current constellation.
 *
 * <p>Every value is read from the server-authoritative
 * {@link SystemDataClientCache} snapshot, never from client-side state.
 */
@EventBusSubscriber(modid = OrvMod.MOD_ID, value = Dist.CLIENT)
public final class ORVOverlayHud {
    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(OrvMod.MOD_ID, "status_hud");

    private static final int BAR_FILL = 0xEE0A0C12;
    private static final int CHANNEL_TEXT = 0xFFE9EEF3;
    private static final int COINS_GOLD = 0xFFFFD700;
    private static final int STRENGTH_RED = 0xFFFF5555;
    private static final int ENERGY_BLUE = 0xFF55FFFF;
    private static final int CONSTELLATION_PURPLE = 0xFFAA55FF;
    private static final int CONSTELLATION_VALUE = 0xFFC9A0FF;
    private static final int PLATE_TEXT = 0xFFE8D8B0;
    private static final int GLOW_CYAN = 0xFF00E5FF;

    private static final int BAR_TOP = 8;
    private static final int BAR_HEIGHT = 34;
    private static final int BAR_MARGIN = 16;
    private static final int BAR_MAX_WIDTH = 1120;
    /** Smallest bar that still leaves room for both brackets and a gap. */
    private static final int BAR_MIN_WIDTH = 260;

    private static final int SEGMENT_GAP = 16;
    /** Columns are pushed this close together before any text is cut. */
    private static final int SEGMENT_GAP_MIN = 8;
    private static final int ICON_GAP = 4;
    /** Floor for the constellation column before other columns give way. */
    private static final int CONSTELLATION_MIN_WIDTH = 70;

    private static final String PLATE_LABEL =
            "[SYSTEM STATUS - INCORPORATED]";
    private static final String NO_CHANNEL = "OFFLINE";
    private static final String NO_CONSTELLATION = "[Searching Star Stream]";

    private static final int MOTE_COUNT = 18;
    private static final int[] MOTE_COLORS = {
            GLOW_CYAN, COINS_GOLD, CONSTELLATION_PURPLE
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

        Font font = minecraft.font;
        PlayerData data = ModAttachments.get(minecraft.player);
        SystemDataSnapshot systemData = SystemDataClientCache
                .find(minecraft.player.getUUID())
                .orElse(null);

        int barWidth = Math.max(
                BAR_MIN_WIDTH,
                Math.min(BAR_MAX_WIDTH, guiGraphics.guiWidth() - BAR_MARGIN * 2)
        );
        int barX = (guiGraphics.guiWidth() - barWidth) / 2;

        renderBar(guiGraphics, barX, barWidth);
        renderSegments(
                guiGraphics,
                font,
                barX,
                barWidth,
                buildSegments(font, data, systemData)
        );
        renderPlate(guiGraphics, font, barX, barWidth);
        renderMotes(guiGraphics, barX, barWidth);
    }

    // ---------------------------------------------------------------
    // Content
    // ---------------------------------------------------------------

    /**
     * Builds the readout columns from the synced snapshot. Falls back to the
     * local attachment only for strength, which rides its own payload.
     */
    private static List<Segment> buildSegments(
            Font font,
            PlayerData data,
            SystemDataSnapshot systemData
    ) {
        long coins = systemData == null ? 0L : systemData.coins();
        long energy = systemData == null ? 0L : systemData.energy();
        long maxEnergy = systemData == null
                ? PlayerData.DEFAULT_MAX_ENERGY
                : systemData.maxEnergy();
        String channelId = systemData == null || systemData.channelId().isBlank()
                ? NO_CHANNEL
                : systemData.channelId();
        boolean hasConstellation = systemData != null
                && !systemData.constellationName().isBlank();

        List<Segment> segments = new ArrayList<>();
        segments.add(new Segment(
                Segment.NO_ICON,
                false,
                "[Kanal: " + channelId + "]",
                null,
                CHANNEL_TEXT,
                CHANNEL_TEXT
        ));
        segments.add(new Segment(
                StatusAtlas.ICON_COIN_U,
                false,
                "Coins: " + coins,
                null,
                COINS_GOLD,
                COINS_GOLD
        ));
        segments.add(new Segment(
                StatusAtlas.ICON_SWORD_U,
                false,
                "Strength: Lv. " + data.strengthLevel(),
                null,
                STRENGTH_RED,
                STRENGTH_RED
        ));
        segments.add(new Segment(
                StatusAtlas.ICON_ENERGY_U,
                false,
                "Energy: " + energy + " / " + maxEnergy,
                null,
                ENERGY_BLUE,
                ENERGY_BLUE
        ));
        segments.add(new Segment(
                Segment.STAR_ICON,
                hasConstellation,
                "Constellation:",
                hasConstellation
                        ? "[" + systemData.constellationName() + "]"
                        : NO_CONSTELLATION,
                CONSTELLATION_PURPLE,
                CONSTELLATION_VALUE
        ));
        return segments;
    }

    /**
     * Lays the columns out left to right. When they do not fit, the
     * constellation column is squeezed first and the channel column second,
     * since those two carry the longest free-form text.
     */
    private static void renderSegments(
            GuiGraphics guiGraphics,
            Font font,
            int barX,
            int barWidth,
            List<Segment> segments
    ) {
        int available = barWidth - 2 * StatusAtlas.BRACKET_WIDTH - 8;
        int gaps = segments.size() - 1;
        int[] widths = new int[segments.size()];
        int content = 0;
        for (int index = 0; index < segments.size(); index++) {
            widths[index] = segments.get(index).width(font);
            content += widths[index];
        }

        // Tighten the gaps before sacrificing any characters.
        int gap = SEGMENT_GAP;
        if (content + gap * gaps > available && gaps > 0) {
            gap = Math.max(
                    SEGMENT_GAP_MIN,
                    Math.min(SEGMENT_GAP, (available - content) / gaps)
            );
        }

        int overflow = content + gap * gaps - available;
        if (overflow > 0) {
            int last = segments.size() - 1;
            int shrink = Math.min(
                    overflow,
                    Math.max(0, widths[last] - CONSTELLATION_MIN_WIDTH)
            );
            widths[last] -= shrink;
            overflow -= shrink;
        }
        if (overflow > 0) {
            int shrink = Math.min(overflow, Math.max(0, widths[0] - 40));
            widths[0] -= shrink;
        }

        int x = barX + StatusAtlas.BRACKET_WIDTH + 4;
        for (int index = 0; index < segments.size(); index++) {
            segments.get(index).render(guiGraphics, font, x, widths[index]);
            x += widths[index] + gap;
        }
    }

    /**
     * One readout column: an optional icon plus one or two lines of text.
     *
     * @param iconU      atlas column of the icon, or a sentinel
     * @param lit        selects the filled star when the icon is the sigil
     * @param label      first line, always drawn
     * @param value      optional second line, stacked under the label
     * @param labelColor colour of the first line
     * @param valueColor colour of the second line
     */
    private record Segment(
            int iconU,
            boolean lit,
            String label,
            String value,
            int labelColor,
            int valueColor
    ) {
        static final int NO_ICON = -1;
        static final int STAR_ICON = -2;

        int width(Font font) {
            int text = Math.max(
                    font.width(label),
                    value == null ? 0 : font.width(value)
            );
            return text + (iconU == NO_ICON
                    ? 0
                    : StatusAtlas.ICON_SIZE + ICON_GAP);
        }

        void render(
                GuiGraphics guiGraphics,
                Font font,
                int x,
                int maxWidth
        ) {
            int textX = x;
            if (iconU != NO_ICON) {
                int iconY = BAR_TOP + (BAR_HEIGHT - StatusAtlas.ICON_SIZE) / 2;
                if (iconU == STAR_ICON) {
                    StatusAtlas.star(guiGraphics, x, iconY, lit);
                } else {
                    StatusAtlas.icon(guiGraphics, iconU, x, iconY);
                }
                textX += StatusAtlas.ICON_SIZE + ICON_GAP;
            }

            int textWidth = Math.max(0, maxWidth - (textX - x));
            if (value == null) {
                // Single line sits on the bar's centre line.
                guiGraphics.drawString(
                        font,
                        fit(font, label, textWidth),
                        textX,
                        BAR_TOP + (BAR_HEIGHT - font.lineHeight) / 2,
                        labelColor,
                        true
                );
                return;
            }

            guiGraphics.drawString(
                    font,
                    fit(font, label, textWidth),
                    textX,
                    BAR_TOP + 6,
                    labelColor,
                    true
            );
            guiGraphics.drawString(
                    font,
                    fit(font, value, textWidth),
                    textX,
                    BAR_TOP + 18,
                    valueColor,
                    true
            );
        }
    }

    /** Truncates with an ellipsis so {@code text} fits {@code maxWidth}. */
    private static String fit(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int budget = maxWidth - font.width(ellipsis);
        if (budget <= 0) {
            return "";
        }
        return font.plainSubstrByWidth(text, budget) + ellipsis;
    }

    // ---------------------------------------------------------------
    // Chrome
    // ---------------------------------------------------------------

    private static void renderBar(
            GuiGraphics guiGraphics,
            int barX,
            int barWidth
    ) {
        guiGraphics.fill(
                barX + 3,
                BAR_TOP + 3,
                barX + barWidth - 3,
                BAR_TOP + BAR_HEIGHT - 3,
                BAR_FILL
        );

        // Scenario-path labyrinth, faint enough that it never fights text.
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 0.14F);
        StatusAtlas.labyrinth(
                guiGraphics,
                barX + 4,
                BAR_TOP + 4,
                barWidth - 8,
                BAR_HEIGHT - 8
        );
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        StatusAtlas.frame(guiGraphics, barX, BAR_TOP, barWidth, BAR_HEIGHT);

        // Brackets overhang the bar vertically, so they anchor the ends.
        int bracketY = BAR_TOP
                + (BAR_HEIGHT - StatusAtlas.BRACKET_HEIGHT) / 2;
        int rightBracketX = barX + barWidth - StatusAtlas.BRACKET_WIDTH;
        StatusAtlas.bracket(guiGraphics, barX, bracketY, false);
        StatusAtlas.bracket(guiGraphics, rightBracketX, bracketY, true);

        int rosetteY = bracketY
                + (StatusAtlas.BRACKET_HEIGHT - StatusAtlas.ROSETTE_SIZE) / 2;
        int rosetteInset =
                (StatusAtlas.BRACKET_WIDTH - StatusAtlas.ROSETTE_SIZE) / 2;
        StatusAtlas.seal(guiGraphics, barX + rosetteInset, rosetteY);
        StatusAtlas.gem(
                guiGraphics,
                rightBracketX + rosetteInset,
                rosetteY
        );
    }

    /** The title plate hanging under the bar, with the channel master. */
    private static void renderPlate(
            GuiGraphics guiGraphics,
            Font font,
            int barX,
            int barWidth
    ) {
        int plateWidth = font.width(PLATE_LABEL) + 26;
        int plateHeight = 18;
        int plateX = barX + (barWidth - plateWidth) / 2;
        int plateY = BAR_TOP + BAR_HEIGHT - 4;

        StatusAtlas.plate(
                guiGraphics,
                plateX,
                plateY,
                plateWidth,
                plateHeight
        );
        guiGraphics.drawString(
                font,
                PLATE_LABEL,
                plateX + (plateWidth - font.width(PLATE_LABEL)) / 2,
                plateY + (plateHeight - font.lineHeight) / 2 + 1,
                PLATE_TEXT,
                false
        );

        StatusAtlas.dokkaebi(
                guiGraphics,
                barX + (barWidth - StatusAtlas.DOKKAEBI_SIZE) / 2,
                plateY + plateHeight - 3
        );
    }

    /** Cyan, gold and violet motes drifting along the bar's long edges. */
    private static void renderMotes(
            GuiGraphics guiGraphics,
            int barX,
            int barWidth
    ) {
        long millis = Util.getMillis();

        for (int index = 0; index < MOTE_COUNT; index++) {
            double speed = 0.05 + (index % 4) * 0.012;
            double phase = ((millis / 1000.0) * speed + index * 0.37) % 1.0;

            int alpha = (int) (Math.sin(phase * Math.PI) * 115.0);
            if (alpha <= 6) {
                continue;
            }

            // Motes travel along the bar rather than across it.
            int travel = (int) (phase * (barWidth - 2 * StatusAtlas.BRACKET_WIDTH));
            int x = barX + StatusAtlas.BRACKET_WIDTH + travel;
            boolean lower = index % 2 == 1;
            double wobble = Math.sin(millis / 950.0 + index * 1.7) * 2.5;
            int y = (lower ? BAR_TOP + BAR_HEIGHT - 4 : BAR_TOP + 2)
                    + (int) wobble;

            int size = 1 + (index % 2);
            guiGraphics.fill(
                    x,
                    y,
                    x + size,
                    y + size,
                    (alpha << 24)
                            | (MOTE_COLORS[index % MOTE_COLORS.length]
                                    & 0x00FFFFFF)
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
