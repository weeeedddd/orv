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
        GuiBlit.nineSlice(
                graphics,
                TEXTURE,
                ATLAS_SIZE,
                slice.u(),
                slice.v(),
                slice.size(),
                slice.corner(),
                x,
                y,
                width,
                height
        );
    }

    /**
     * Fills the given rectangle with repeated wood tiles, so the grain keeps
     * a constant scale at any panel size.
     */
    public static void tileWood(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height
    ) {
        GuiBlit.tile(
                graphics,
                TEXTURE,
                ATLAS_SIZE,
                WOOD_U,
                WOOD_V,
                WOOD_TILE,
                WOOD_TILE,
                x,
                y,
                width,
                height
        );
    }

    /** Draws one of the 16x16 sidebar icons. */
    public static void icon(
            GuiGraphics graphics,
            int iconU,
            int x,
            int y
    ) {
        GuiBlit.blit(
                graphics,
                TEXTURE,
                ATLAS_SIZE,
                x,
                y,
                ICON_SIZE,
                ICON_SIZE,
                iconU,
                ICON_V,
                ICON_SIZE,
                ICON_SIZE
        );
    }

    private GuildAtlas() {
    }
}
