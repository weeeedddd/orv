package com.weeeedddd.orv.client.gui;

import com.weeeedddd.orv.OrvMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Region constants and blitting helpers for the guild GUI atlas.
 *
 * <p>The atlas itself is produced by {@code tools/GenerateGuildAtlas.java};
 * the regions below must stay in sync with the layout documented there.
 */
public final class GuildAtlas {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    OrvMod.MOD_ID,
                    "textures/gui/guild_panel.png"
            );

    private static final int ATLAS_SIZE = 256;

    /** Tileable dark oak planking. */
    public static final int WOOD_U = 0;
    public static final int WOOD_V = 0;
    public static final int WOOD_TILE = 64;

    public static final Slice FRAME = new Slice(64, 0, 48, 16);
    public static final Slice PARCHMENT = new Slice(112, 0, 48, 16);
    public static final Slice PLAQUE = new Slice(160, 0, 48, 12);
    public static final Slice TAB_OFF = new Slice(0, 64, 24, 8);
    public static final Slice TAB_ON = new Slice(24, 64, 24, 8);
    public static final Slice ROW_EVEN = new Slice(48, 64, 24, 8);
    public static final Slice ROW_ODD = new Slice(72, 64, 24, 8);
    public static final Slice SIDEBAR = new Slice(96, 64, 32, 12);

    public static final int ICON_SIZE = 16;
    public static final int ICON_QUILL_U = 0;
    public static final int ICON_LETTER_U = 16;
    public static final int ICON_KEY_U = 32;
    public static final int ICON_V = 96;

    /**
     * A square atlas region drawn as a nine-slice.
     *
     * @param u      left edge in the atlas
     * @param v      top edge in the atlas
     * @param size   width and height of the region
     * @param corner size of the four fixed corner pieces
     */
    public record Slice(int u, int v, int size, int corner) {
    }

    /** Draws {@code slice} stretched to fill the given rectangle. */
    public static void nineSlice(
            GuiGraphics graphics,
            Slice slice,
            int x,
            int y,
            int width,
            int height
    ) {
        int corner = slice.corner();
        int size = slice.size();
        int u = slice.u();
        int v = slice.v();

        // Source width of the stretchable middle band.
        int band = size - corner * 2;
        int innerWidth = width - corner * 2;
        int innerHeight = height - corner * 2;
        int farU = u + size - corner;
        int farV = v + size - corner;
        int farX = x + width - corner;
        int farY = y + height - corner;

        blit(graphics, x, y, corner, corner, u, v, corner, corner);
        blit(graphics, farX, y, corner, corner, farU, v, corner, corner);
        blit(graphics, x, farY, corner, corner, u, farV, corner, corner);
        blit(graphics, farX, farY, corner, corner, farU, farV, corner, corner);

        if (innerWidth > 0) {
            blit(graphics, x + corner, y, innerWidth, corner,
                    u + corner, v, band, corner);
            blit(graphics, x + corner, farY, innerWidth, corner,
                    u + corner, farV, band, corner);
        }
        if (innerHeight > 0) {
            blit(graphics, x, y + corner, corner, innerHeight,
                    u, v + corner, corner, band);
            blit(graphics, farX, y + corner, corner, innerHeight,
                    farU, v + corner, corner, band);
        }
        if (innerWidth > 0 && innerHeight > 0) {
            blit(graphics, x + corner, y + corner, innerWidth, innerHeight,
                    u + corner, v + corner, band, band);
        }
    }

    /**
     * Fills the given rectangle with repeated wood tiles. Partial tiles at
     * the right and bottom edges are cropped rather than squashed, so the
     * grain keeps a constant scale at any panel size.
     */
    public static void tileWood(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height
    ) {
        for (int offsetY = 0; offsetY < height; offsetY += WOOD_TILE) {
            int tileHeight = Math.min(WOOD_TILE, height - offsetY);
            for (int offsetX = 0; offsetX < width; offsetX += WOOD_TILE) {
                int tileWidth = Math.min(WOOD_TILE, width - offsetX);
                blit(graphics, x + offsetX, y + offsetY, tileWidth, tileHeight,
                        WOOD_U, WOOD_V, tileWidth, tileHeight);
            }
        }
    }

    /** Draws one of the 16x16 sidebar icons. */
    public static void icon(
            GuiGraphics graphics,
            int iconU,
            int x,
            int y
    ) {
        blit(graphics, x, y, ICON_SIZE, ICON_SIZE,
                iconU, ICON_V, ICON_SIZE, ICON_SIZE);
    }

    private static void blit(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int u,
            int v,
            int sourceWidth,
            int sourceHeight
    ) {
        graphics.blit(
                TEXTURE,
                x,
                y,
                width,
                height,
                (float) u,
                (float) v,
                sourceWidth,
                sourceHeight,
                ATLAS_SIZE,
                ATLAS_SIZE
        );
    }

    private GuildAtlas() {
    }
}
