package com.weeeedddd.orv.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Texture blitting shared by the mod's atlases: a stretching blit, a
 * nine-slice built on top of it, and a clipped tiler.
 */
public final class GuiBlit {

    /** Draws {@code sourceWidth}x{@code sourceHeight} stretched to fit. */
    public static void blit(
            GuiGraphics graphics,
            ResourceLocation texture,
            int atlasSize,
            int x,
            int y,
            int width,
            int height,
            int u,
            int v,
            int sourceWidth,
            int sourceHeight
    ) {
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.blit(
                texture,
                x,
                y,
                width,
                height,
                (float) u,
                (float) v,
                sourceWidth,
                sourceHeight,
                atlasSize,
                atlasSize
        );
    }

    /**
     * Draws a square atlas region as a nine-slice: the four {@code corner}
     * sized corners stay fixed while the edges and centre stretch.
     */
    public static void nineSlice(
            GuiGraphics graphics,
            ResourceLocation texture,
            int atlasSize,
            int u,
            int v,
            int size,
            int corner,
            int x,
            int y,
            int width,
            int height
    ) {
        // Source width of the stretchable middle band.
        int band = size - corner * 2;
        int innerWidth = width - corner * 2;
        int innerHeight = height - corner * 2;
        int farU = u + size - corner;
        int farV = v + size - corner;
        int farX = x + width - corner;
        int farY = y + height - corner;

        blit(graphics, texture, atlasSize, x, y, corner, corner,
                u, v, corner, corner);
        blit(graphics, texture, atlasSize, farX, y, corner, corner,
                farU, v, corner, corner);
        blit(graphics, texture, atlasSize, x, farY, corner, corner,
                u, farV, corner, corner);
        blit(graphics, texture, atlasSize, farX, farY, corner, corner,
                farU, farV, corner, corner);

        if (innerWidth > 0) {
            blit(graphics, texture, atlasSize, x + corner, y,
                    innerWidth, corner, u + corner, v, band, corner);
            blit(graphics, texture, atlasSize, x + corner, farY,
                    innerWidth, corner, u + corner, farV, band, corner);
        }
        if (innerHeight > 0) {
            blit(graphics, texture, atlasSize, x, y + corner,
                    corner, innerHeight, u, v + corner, corner, band);
            blit(graphics, texture, atlasSize, farX, y + corner,
                    corner, innerHeight, farU, v + corner, corner, band);
        }
        if (innerWidth > 0 && innerHeight > 0) {
            blit(graphics, texture, atlasSize, x + corner, y + corner,
                    innerWidth, innerHeight,
                    u + corner, v + corner, band, band);
        }
    }

    /**
     * Fills a rectangle with repeated copies of an atlas region. Partial
     * tiles at the right and bottom edges are cropped rather than squashed,
     * so the pattern keeps a constant scale at any size.
     */
    public static void tile(
            GuiGraphics graphics,
            ResourceLocation texture,
            int atlasSize,
            int u,
            int v,
            int tileWidth,
            int tileHeight,
            int x,
            int y,
            int width,
            int height
    ) {
        for (int offsetY = 0; offsetY < height; offsetY += tileHeight) {
            int drawHeight = Math.min(tileHeight, height - offsetY);
            for (int offsetX = 0; offsetX < width; offsetX += tileWidth) {
                int drawWidth = Math.min(tileWidth, width - offsetX);
                blit(graphics, texture, atlasSize,
                        x + offsetX, y + offsetY, drawWidth, drawHeight,
                        u, v, drawWidth, drawHeight);
            }
        }
    }

    private GuiBlit() {
    }
}
