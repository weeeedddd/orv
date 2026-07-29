package com.weeeedddd.orv.client.gui;

import com.weeeedddd.orv.OrvMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Region constants and blitting helpers for the character sheet atlas.
 *
 * <p>The atlas itself is produced by
 * {@code tools/GenerateCharacterAtlas.java}; the regions below must stay in
 * sync with the layout documented there.
 */
public final class CharacterAtlas {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    OrvMod.MOD_ID,
                    "textures/gui/character_panel.png"
            );

    private static final int ATLAS_SIZE = 128;

    /** Twin glowing rules; the centre is clear. */
    private static final int FRAME_U = 0;
    private static final int FRAME_V = 0;
    private static final int FRAME_SIZE = 48;
    private static final int FRAME_CORNER = 16;

    public static final int EYE_SIZE = 24;
    private static final int EYE_U = 48;
    private static final int EYE_V = 0;

    public static final int GEAR_SIZE = 20;
    private static final int GEAR_U = 72;
    private static final int GEAR_V = 0;

    public static final int HORN_SIZE = 16;
    private static final int HORN_U = 92;
    private static final int HORN_V = 0;

    private static final int STARS_U = 0;
    private static final int STARS_V = 48;
    private static final int STARS_W = 64;
    private static final int STARS_H = 48;

    /** Draws the panel border around the given rectangle. */
    public static void frame(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height
    ) {
        GuiBlit.nineSlice(graphics, TEXTURE, ATLAS_SIZE, ATLAS_SIZE,
                FRAME_U, FRAME_V, FRAME_SIZE, FRAME_CORNER,
                x, y, width, height);
    }

    /** Draws the watching-eye sigil. */
    public static void eye(GuiGraphics graphics, int x, int y) {
        GuiBlit.blit(graphics, TEXTURE, ATLAS_SIZE, ATLAS_SIZE, x, y,
                EYE_SIZE, EYE_SIZE, EYE_U, EYE_V, EYE_SIZE, EYE_SIZE);
    }

    /** Draws the small clockwork gear. */
    public static void gear(GuiGraphics graphics, int x, int y) {
        GuiBlit.blit(graphics, TEXTURE, ATLAS_SIZE, ATLAS_SIZE, x, y,
                GEAR_SIZE, GEAR_SIZE, GEAR_U, GEAR_V, GEAR_SIZE, GEAR_SIZE);
    }

    /** Draws the dokkaebi horn-and-eye sigil. */
    public static void horn(GuiGraphics graphics, int x, int y) {
        GuiBlit.blit(graphics, TEXTURE, ATLAS_SIZE, ATLAS_SIZE, x, y,
                HORN_SIZE, HORN_SIZE, HORN_U, HORN_V, HORN_SIZE, HORN_SIZE);
    }

    /** Tiles the constellation field across the given rectangle. */
    public static void stars(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height
    ) {
        GuiBlit.tile(graphics, TEXTURE, ATLAS_SIZE, ATLAS_SIZE,
                STARS_U, STARS_V, STARS_W, STARS_H, x, y, width, height);
    }

    private CharacterAtlas() {
    }
}
