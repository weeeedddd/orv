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

    private static final int ATLAS_W = 256;
    private static final int ATLAS_H = 128;

    /** Glowing filigree border; the centre is clear. */
    private static final int FRAME_U = 0;
    private static final int FRAME_V = 0;
    private static final int FRAME_SIZE = 48;
    private static final int FRAME_CORNER = 16;

    public static final int ROSETTE_SIZE = 24;
    private static final int SEAL_U = 48;
    private static final int SEAL_V = 0;
    private static final int GEM_U = 48;
    private static final int GEM_V = 24;

    public static final int ICON_SIZE = 16;
    public static final int ICON_COIN_U = 72;
    public static final int ICON_ENERGY_U = 88;
    public static final int ICON_SWORD_U = 104;
    private static final int ICON_V = 0;

    public static final int STAR_OFF_U = 72;
    public static final int STAR_ON_U = 88;
    private static final int STAR_V = 16;

    public static final int BRACKET_WIDTH = 44;
    public static final int BRACKET_HEIGHT = 56;
    private static final int BRACKET_LEFT_U = 128;
    private static final int BRACKET_RIGHT_U = 172;
    private static final int BRACKET_V = 0;

    private static final int PLATE_U = 216;
    private static final int PLATE_V = 0;
    private static final int PLATE_SIZE = 24;
    private static final int PLATE_CORNER = 8;

    public static final int DOKKAEBI_SIZE = 16;
    private static final int DOKKAEBI_U = 216;
    private static final int DOKKAEBI_V = 24;

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
        GuiBlit.nineSlice(graphics, TEXTURE, ATLAS_W, ATLAS_H,
                FRAME_U, FRAME_V, FRAME_SIZE, FRAME_CORNER,
                x, y, width, height);
    }

    /** Draws one of the ornate end brackets. */
    public static void bracket(
            GuiGraphics graphics,
            int x,
            int y,
            boolean right
    ) {
        GuiBlit.blit(graphics, TEXTURE, ATLAS_W, ATLAS_H, x, y,
                BRACKET_WIDTH, BRACKET_HEIGHT,
                right ? BRACKET_RIGHT_U : BRACKET_LEFT_U, BRACKET_V,
                BRACKET_WIDTH, BRACKET_HEIGHT);
    }

    /** Draws the gear-and-eye seal that sits in the left bracket. */
    public static void seal(GuiGraphics graphics, int x, int y) {
        GuiBlit.blit(graphics, TEXTURE, ATLAS_W, ATLAS_H, x, y,
                ROSETTE_SIZE, ROSETTE_SIZE, SEAL_U, SEAL_V,
                ROSETTE_SIZE, ROSETTE_SIZE);
    }

    /** Draws the faceted gem that sits in the right bracket. */
    public static void gem(GuiGraphics graphics, int x, int y) {
        GuiBlit.blit(graphics, TEXTURE, ATLAS_W, ATLAS_H, x, y,
                ROSETTE_SIZE, ROSETTE_SIZE, GEM_U, GEM_V,
                ROSETTE_SIZE, ROSETTE_SIZE);
    }

    /** Draws one of the 16x16 readout icons. */
    public static void icon(GuiGraphics graphics, int iconU, int x, int y) {
        GuiBlit.blit(graphics, TEXTURE, ATLAS_W, ATLAS_H, x, y,
                ICON_SIZE, ICON_SIZE, iconU, ICON_V, ICON_SIZE, ICON_SIZE);
    }

    /** Draws the star sigil; {@code lit} selects the filled variant. */
    public static void star(
            GuiGraphics graphics,
            int x,
            int y,
            boolean lit
    ) {
        GuiBlit.blit(graphics, TEXTURE, ATLAS_W, ATLAS_H, x, y,
                ICON_SIZE, ICON_SIZE,
                lit ? STAR_ON_U : STAR_OFF_U, STAR_V,
                ICON_SIZE, ICON_SIZE);
    }

    /** Draws the plate that hangs below the bar. */
    public static void plate(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height
    ) {
        GuiBlit.nineSlice(graphics, TEXTURE, ATLAS_W, ATLAS_H,
                PLATE_U, PLATE_V, PLATE_SIZE, PLATE_CORNER,
                x, y, width, height);
    }

    /** Draws the horned channel-master head. */
    public static void dokkaebi(GuiGraphics graphics, int x, int y) {
        GuiBlit.blit(graphics, TEXTURE, ATLAS_W, ATLAS_H, x, y,
                DOKKAEBI_SIZE, DOKKAEBI_SIZE, DOKKAEBI_U, DOKKAEBI_V,
                DOKKAEBI_SIZE, DOKKAEBI_SIZE);
    }

    /** Tiles the scenario-path labyrinth across the given rectangle. */
    public static void labyrinth(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height
    ) {
        GuiBlit.tile(graphics, TEXTURE, ATLAS_W, ATLAS_H,
                LAB_U, LAB_V, LAB_W, LAB_H, x, y, width, height);
    }

    private StatusAtlas() {
    }
}
