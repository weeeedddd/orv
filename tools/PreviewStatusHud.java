import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Offline preview of {@link ORVOverlayHud}'s layout over a stand-in dusk
 * scene, including the vanilla hotbar and hearts for context.
 *
 * <p>Development aid, not production code. It replays the same blitting and
 * layout constants against the generated atlas, and draws text at a fixed
 * 6px advance — close enough to Minecraft's default font to expose overflow
 * and centring mistakes, but not glyph-accurate.
 *
 * <p>Constants below are mirrored from ORVOverlayHud and StatusAtlas; if
 * those change, update them here too.
 */
public final class PreviewStatusHud {

    // Mirrored from ORVOverlayHud.
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
    private static final int UNDERLAY_OFFSET = 4;
    private static final int SEAL_INSET = 8;
    private static final int HEADER_TOP = 6;
    private static final int DIVIDER_Y = 34;
    private static final int ROW_TOP = 42;
    private static final int ROW_STEP = 15;
    private static final int ICON_X = 12;
    private static final int TEXT_X = 34;
    private static final int MOTE_COUNT = 14;

    // Mirrored from StatusAtlas.
    private static final int ATLAS_SIZE = 128;
    private static final int FRAME_SIZE = 48;
    private static final int FRAME_CORNER = 16;
    private static final int SEAL_SIZE = 24;
    private static final int SEAL_U = 48;
    private static final int ICON_COIN_U = 72;
    private static final int ICON_ENERGY_U = 88;
    private static final int ICON_SWORD_U = 104;
    private static final int STAR_OFF_U = 72;
    private static final int STAR_ON_U = 88;
    private static final int STAR_V = 16;
    private static final int HORN_U = 104;
    private static final int HORN_V = 16;
    private static final int HORN_SIZE = 8;
    private static final int LAB_V = 48;
    private static final int LAB_W = 64;
    private static final int LAB_H = 32;

    private static final int SCREEN_W = 640;
    private static final int SCREEN_H = 360;
    private static final int ADVANCE = 6;
    private static final int LINE_HEIGHT = 9;

    private static BufferedImage atlas;
    private static BufferedImage canvas;
    private static Graphics2D text;

    public static void main(String[] args) throws IOException {
        atlas = ImageIO.read(new File(
                "src/main/resources/assets/orv/textures/gui/status_hud.png"));

        render("empty", 0L, 0L, 0, "None");
        render("filled", 1250L, 340L, 7, "Demon-like Judge of Fire");
    }

    private static void render(
            String name,
            long coins,
            long energy,
            int strength,
            String constellation
    ) throws IOException {
        newCanvas();

        String title = "[ SYSTEM STATUS ]";
        String coinsText = "Coins: " + coins;
        String energyText = "Energy: " + energy;
        String strengthText = "Strength: Lv. " + strength;
        String constellationText = "Constellation: " + constellation;
        boolean lit = !"None".equals(constellation);

        int rowsWidth = width(coinsText);
        rowsWidth = Math.max(rowsWidth, width(energyText));
        rowsWidth = Math.max(rowsWidth, width(strengthText));
        rowsWidth = Math.max(rowsWidth, width(constellationText));

        int headerWidth = SEAL_INSET * 2 + SEAL_SIZE * 2 + 16 + width(title);
        int panelWidth = Math.max(MIN_PANEL_WIDTH,
                Math.max(headerWidth, TEXT_X + rowsWidth + 18));

        // Panel planes.
        rect(PANEL_X + UNDERLAY_OFFSET, PANEL_Y + UNDERLAY_OFFSET,
                PANEL_X + panelWidth + UNDERLAY_OFFSET,
                PANEL_Y + PANEL_HEIGHT + UNDERLAY_OFFSET, PANEL_UNDERLAY);
        rect(PANEL_X, PANEL_Y, PANEL_X + panelWidth,
                PANEL_Y + PANEL_HEIGHT, PANEL_BACKGROUND);

        // Labyrinth at 22% opacity.
        tile(0, LAB_V, LAB_W, LAB_H, PANEL_X + 6, PANEL_Y + DIVIDER_Y + 4,
                panelWidth - 12, PANEL_HEIGHT - DIVIDER_Y - 10, 0.18);

        // Filigree frame.
        nineSlice(0, 0, FRAME_SIZE, FRAME_CORNER, PANEL_X, PANEL_Y,
                panelWidth, PANEL_HEIGHT);

        // Header.
        int sealY = PANEL_Y + HEADER_TOP;
        blit(PANEL_X + SEAL_INSET, sealY, SEAL_SIZE, SEAL_SIZE,
                SEAL_U, 0, SEAL_SIZE, SEAL_SIZE, 1.0);
        blit(PANEL_X + panelWidth - SEAL_INSET - SEAL_SIZE, sealY,
                SEAL_SIZE, SEAL_SIZE, SEAL_U, 0, SEAL_SIZE, SEAL_SIZE, 1.0);
        draw(title, PANEL_X + (panelWidth - width(title)) / 2,
                sealY + (SEAL_SIZE - LINE_HEIGHT) / 2, ACCENT_CYAN);

        int dividerY = PANEL_Y + DIVIDER_Y;
        rect(PANEL_X + 10, dividerY, PANEL_X + panelWidth - 10,
                dividerY + 1, ACCENT_CYAN);
        blit(PANEL_X + 3, dividerY - 4, HORN_SIZE, HORN_SIZE,
                HORN_U, HORN_V, HORN_SIZE, HORN_SIZE, 1.0);
        blit(PANEL_X + panelWidth - 3 - HORN_SIZE, dividerY - 4,
                HORN_SIZE, HORN_SIZE, HORN_U, HORN_V, HORN_SIZE, HORN_SIZE, 1.0);

        // Rows.
        row(0, coinsText, COINS_GOLD, ICON_COIN_U, 0);
        row(1, energyText, ENERGY_BLUE, ICON_ENERGY_U, 0);
        row(2, strengthText, STRENGTH_RED, ICON_SWORD_U, 0);
        blit(PANEL_X + ICON_X, rowY(3) - 4, 16, 16,
                lit ? STAR_ON_U : STAR_OFF_U, STAR_V, 16, 16, 1.0);
        draw(constellationText, PANEL_X + TEXT_X, rowY(3),
                CONSTELLATION_PURPLE);

        // Motes, sampled at a fixed instant.
        long millis = 4200L;
        int[] moteColors = {ACCENT_CYAN, COINS_GOLD, CONSTELLATION_PURPLE};
        for (int i = 0; i < MOTE_COUNT; i++) {
            double speed = 0.07 + (i % 4) * 0.015;
            double phase = ((millis / 1000.0) * speed + i * 0.41) % 1.0;
            int alpha = (int) (Math.sin(phase * Math.PI) * 120.0);
            if (alpha <= 6) {
                continue;
            }
            boolean right = i % 2 == 1;
            double wobble = Math.sin(millis / 900.0 + i * 1.7) * 3.0;
            int x = right ? PANEL_X + panelWidth - 5 + (int) wobble
                    : PANEL_X + 4 + (int) wobble;
            int y = PANEL_Y + PANEL_HEIGHT - 6
                    - (int) (phase * (PANEL_HEIGHT - 12));
            int size = 1 + (i % 2);
            rect(x, y, x + size, y + size,
                    (alpha << 24) | (moteColors[i % 3] & 0xFFFFFF));
        }

        hotbarAndHearts();

        File out = new File("/tmp/claude-0/-home-user/"
                + "6ebea994-640a-5f8a-872a-9643c99f5e6a/scratchpad/"
                + "hud-" + name + ".png");
        BufferedImage scaled = new BufferedImage(SCREEN_W * 3, SCREEN_H * 3,
                BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < SCREEN_H * 3; y++) {
            for (int x = 0; x < SCREEN_W * 3; x++) {
                scaled.setRGB(x, y, canvas.getRGB(x / 3, y / 3));
            }
        }
        ImageIO.write(scaled, "PNG", out);
        System.out.println("Wrote " + out.getPath());
    }

    private static void row(int index, String s, int color, int iconU, int v) {
        int y = rowY(index);
        blit(PANEL_X + ICON_X, y - 4, 16, 16, iconU, v, 16, 16, 1.0);
        draw(s, PANEL_X + TEXT_X, y, color);
    }

    private static int rowY(int index) {
        return PANEL_Y + ROW_TOP + index * ROW_STEP;
    }

    private static int width(String s) {
        return s.length() * ADVANCE;
    }

    // -------------------------------------------------------------------

    /** Stand-in dusk scene: amber horizon, indigo zenith, distant hills. */
    private static void newCanvas() {
        canvas = new BufferedImage(SCREEN_W, SCREEN_H,
                BufferedImage.TYPE_INT_ARGB);
        int horizon = (int) (SCREEN_H * 0.62);
        for (int y = 0; y < SCREEN_H; y++) {
            double t = Math.min(1.0, Math.max(0.0, y / (double) horizon));
            int r = (int) (24 + t * t * 210);
            int g = (int) (20 + t * t * 120);
            int b = (int) (60 + (1 - t) * 70);
            for (int x = 0; x < SCREEN_W; x++) {
                canvas.setRGB(x, y, 0xFF000000 | clamp(r) << 16
                        | clamp(g) << 8 | clamp(b));
            }
        }
        // Distant hills, kept blocky to read as voxel terrain.
        for (int x = 0; x < SCREEN_W; x++) {
            int h = (int) (18 * Math.sin(x / 90.0)
                    + 11 * Math.sin(x / 37.0 + 1.7) + 26);
            for (int y = horizon - h; y < SCREEN_H; y++) {
                double depth = (y - (horizon - h)) / (double) (SCREEN_H);
                int shade = (int) (26 + depth * 40);
                canvas.setRGB(x, y, 0xFF000000 | clamp(shade + 14) << 16
                        | clamp(shade + 6) << 8 | clamp(shade + 22));
            }
        }

        text = canvas.createGraphics();
        text.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        text.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
    }

    /** Simplified vanilla hotbar and hearts, drawn crisp for context. */
    private static void hotbarAndHearts() {
        int barW = 182;
        int barH = 22;
        int barX = (SCREEN_W - barW) / 2;
        int barY = SCREEN_H - barH - 4;

        rect(barX, barY, barX + barW, barY + barH, 0xDD1B1B1B);
        rect(barX, barY, barX + barW, barY + 1, 0xFF6E6E6E);
        rect(barX, barY + barH - 1, barX + barW, barY + barH, 0xFF000000);
        for (int i = 0; i <= 9; i++) {
            int x = barX + i * 20;
            rect(x, barY, x + 1, barY + barH, 0xFF4A4A4A);
        }
        // Selection highlight around the first slot.
        int selX = barX - 1;
        rect(selX, barY - 1, selX + 24, barY + barH + 1, 0x00000000);
        outline(selX, barY - 1, 24, barH + 2, 0xFFFFFFFF);

        for (int i = 0; i < 10; i++) {
            int hx = barX + i * 8;
            int hy = barY - 12;
            heart(hx, hy);
        }
    }

    private static void heart(int x, int y) {
        String[] rows = {
                ".RR.RR.",
                "RRRRRRR",
                "RRRRRRR",
                ".RRRRR.",
                "..RRR..",
                "...R..."
        };
        for (int j = 0; j < rows.length; j++) {
            for (int i = 0; i < rows[j].length(); i++) {
                if (rows[j].charAt(i) == 'R') {
                    rect(x + i, y + j, x + i + 1, y + j + 1, 0xFFD62B2B);
                }
            }
        }
    }

    private static void outline(int x, int y, int w, int h, int argb) {
        rect(x, y, x + w, y + 1, argb);
        rect(x, y + h - 1, x + w, y + h, argb);
        rect(x, y, x + 1, y + h, argb);
        rect(x + w - 1, y, x + w, y + h, argb);
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    private static void rect(int x1, int y1, int x2, int y2, int argb) {
        double a = ((argb >>> 24) & 0xFF) / 255.0;
        if (a <= 0) {
            return;
        }
        for (int y = Math.max(0, y1); y < Math.min(SCREEN_H, y2); y++) {
            for (int x = Math.max(0, x1); x < Math.min(SCREEN_W, x2); x++) {
                canvas.setRGB(x, y, mix(canvas.getRGB(x, y), argb, a));
            }
        }
    }

    private static void draw(String s, int x, int y, int argb) {
        // Drop shadow, as drawString(..., true) would render it.
        text.setColor(new Color(darken(argb), true));
        for (int i = 0; i < s.length(); i++) {
            text.drawString(String.valueOf(s.charAt(i)),
                    x + i * ADVANCE + 1, y + LINE_HEIGHT - 1);
        }
        text.setColor(new Color(argb, true));
        for (int i = 0; i < s.length(); i++) {
            text.drawString(String.valueOf(s.charAt(i)),
                    x + i * ADVANCE, y + LINE_HEIGHT - 2);
        }
    }

    private static int darken(int argb) {
        int r = ((argb >> 16) & 0xFF) / 4;
        int g = ((argb >> 8) & 0xFF) / 4;
        int b = (argb & 0xFF) / 4;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static void nineSlice(int u, int v, int size, int c,
                                  int x, int y, int w, int h) {
        int band = size - c * 2;
        int iw = w - c * 2;
        int ih = h - c * 2;
        int fu = u + size - c;
        int fv = v + size - c;
        int fx = x + w - c;
        int fy = y + h - c;
        blit(x, y, c, c, u, v, c, c, 1.0);
        blit(fx, y, c, c, fu, v, c, c, 1.0);
        blit(x, fy, c, c, u, fv, c, c, 1.0);
        blit(fx, fy, c, c, fu, fv, c, c, 1.0);
        if (iw > 0) {
            blit(x + c, y, iw, c, u + c, v, band, c, 1.0);
            blit(x + c, fy, iw, c, u + c, fv, band, c, 1.0);
        }
        if (ih > 0) {
            blit(x, y + c, c, ih, u, v + c, c, band, 1.0);
            blit(fx, y + c, c, ih, fu, v + c, c, band, 1.0);
        }
        if (iw > 0 && ih > 0) {
            blit(x + c, y + c, iw, ih, u + c, v + c, band, band, 1.0);
        }
    }

    private static void tile(int u, int v, int tw, int th,
                             int x, int y, int w, int h, double opacity) {
        for (int oy = 0; oy < h; oy += th) {
            int dh = Math.min(th, h - oy);
            for (int ox = 0; ox < w; ox += tw) {
                int dw = Math.min(tw, w - ox);
                blit(x + ox, y + oy, dw, dh, u, v, dw, dh, opacity);
            }
        }
    }

    private static void blit(int dx, int dy, int dw, int dh,
                             int u, int v, int sw, int sh, double opacity) {
        for (int j = 0; j < dh; j++) {
            int sy = v + (int) ((long) j * sh / dh);
            for (int i = 0; i < dw; i++) {
                int sx = u + (int) ((long) i * sw / dw);
                int px = dx + i;
                int py = dy + j;
                if (px < 0 || py < 0 || px >= SCREEN_W || py >= SCREEN_H
                        || sx < 0 || sy < 0 || sx >= ATLAS_SIZE
                        || sy >= ATLAS_SIZE) {
                    continue;
                }
                int src = atlas.getRGB(sx, sy);
                double a = ((src >>> 24) & 0xFF) / 255.0 * opacity;
                if (a <= 0.0) {
                    continue;
                }
                canvas.setRGB(px, py, mix(canvas.getRGB(px, py), src, a));
            }
        }
    }

    private static int mix(int dst, int src, double a) {
        int r = (int) (((dst >> 16) & 0xFF) * (1 - a) + ((src >> 16) & 0xFF) * a);
        int g = (int) (((dst >> 8) & 0xFF) * (1 - a) + ((src >> 8) & 0xFF) * a);
        int b = (int) ((dst & 0xFF) * (1 - a) + (src & 0xFF) * a);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private PreviewStatusHud() {
    }
}
