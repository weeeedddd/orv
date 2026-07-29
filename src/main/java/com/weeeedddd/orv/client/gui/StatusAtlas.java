package com.weeeedddd.orv.client.gui;

import com.weeeedddd.orv.OrvMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Region constants and blitting helpers for the status HUD atlas.
 *
 * <p>The atlas itself is produced by {@code tools/GenerateStatusAtlas.java};
 * the regions below must stay in sync with the layout documented there.
 */
public final class StatusAtlas {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    OrvMod.MOD_ID,
                    "textures/gui/status_hud.png"
            );

    private static final int ATLAS_SIZE = 128;

    /** Glowing filigree border; the centre is clear. */
    private static final int FRAME_U = 0;
    private static final int FRAME_V = 0;
    private static final int FRAME_SIZE = 48;
    private static final int FRAME_CORNER = 16;

    public static final int SEAL_SIZE = 24;
    private static final int SEAL_U = 48;
    private static final int SEAL_V = 0;

    public static final int ICON_SIZE = 16;
    public static final int ICON_COIN_U = 72;
    public static final int ICON_ENERGY_U = 88;
    public static final int ICON_SWORD_U = 104;
    private static final int ICON_V = 0;

    public static final int STAR_OFF_U = 72;
    public static final int STAR_ON_U = 88;
    private static final int STAR_V = 16;

    public static final int HORN_SIZE = 8;
    private static final int HORN_U = 104;
    private static final int HORN_V = 16;

    private static final int LAB_U = 0;
    private static final int LAB_V = 48;
    private static final int LAB_W = 64;
    private static final int LAB_H = 32;

    /** Draws the filigree border around the given rectangle. */
    public static void frame(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height
    ) {
        GuiBlit.nineSlice(graphics, TEXTURE, ATLAS_SIZE,
                FRAME_U, FRAME_V, FRAME_SIZE, FRAME_CORNER,
                x, y, width, height);
    }

    /** Draws the gear-and-eye alchemy seal. */
    public static void seal(GuiGraphics graphics, int x, int y) {
        GuiBlit.blit(graphics, TEXTURE, ATLAS_SIZE, x, y,
                SEAL_SIZE, SEAL_SIZE, SEAL_U, SEAL_V, SEAL_SIZE, SEAL_SIZE);
    }

    /** Draws one of the 16x16 readout icons. */
    public static void icon(GuiGraphics graphics, int iconU, int x, int y) {
        GuiBlit.blit(graphics, TEXTURE, ATLAS_SIZE, x, y,
                ICON_SIZE, ICON_SIZE, iconU, ICON_V, ICON_SIZE, ICON_SIZE);
    }

    /** Draws the star sigil; {@code lit} selects the filled variant. */
    public static void star(
            GuiGraphics graphics,
            int x,
            int y,
            boolean lit
    ) {
        GuiBlit.blit(graphics, TEXTURE, ATLAS_SIZE, x, y,
                ICON_SIZE, ICON_SIZE,
                lit ? STAR_ON_U : STAR_OFF_U, STAR_V,
                ICON_SIZE, ICON_SIZE);
    }

    /** Draws the small dokkaebi horn-and-eye junction motif. */
    public static void horn(GuiGraphics graphics, int x, int y) {
        GuiBlit.blit(graphics, TEXTURE, ATLAS_SIZE, x, y,
                HORN_SIZE, HORN_SIZE, HORN_U, HORN_V, HORN_SIZE, HORN_SIZE);
    }

    /** Tiles the scenario-path labyrinth across the given rectangle. */
    public static void labyrinth(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height
    ) {
        GuiBlit.tile(graphics, TEXTURE, ATLAS_SIZE,
                LAB_U, LAB_V, LAB_W, LAB_H, x, y, width, height);
    }

    private StatusAtlas() {
    }
}
