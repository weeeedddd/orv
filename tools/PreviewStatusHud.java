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
    private static final int BAR_MIN_WIDTH = 260;
    private static final int SEGMENT_GAP = 16;
    private static final int SEGMENT_GAP_MIN = 8;
    private static final int ICON_GAP = 4;
    private static final int CONSTELLATION_MIN_WIDTH = 70;
    private static final int MOTE_COUNT = 18;

    // Mirrored from StatusAtlas.
    private static final int ATLAS_W = 256;
    private static final int ATLAS_H = 128;
    private static final int FRAME_SIZE = 48;
    private static final int FRAME_CORNER = 16;
    private static final int ROSETTE_SIZE = 24;
    private static final int SEAL_U = 48;
    private static final int SEAL_V = 0;
    private static final int GEM_U = 48;
    private static final int GEM_V = 24;
    private static final int ICON_SIZE = 16;
    private static final int ICON_COIN_U = 72;
    private static final int ICON_ENERGY_U = 88;
    private static final int ICON_SWORD_U = 104;
    private static final int STAR_OFF_U = 72;
    private static final int STAR_ON_U = 88;
    private static final int STAR_V = 16;
    private static final int BRACKET_W = 44;
    private static final int BRACKET_H = 56;
    private static final int BRACKET_L_U = 128;
    private static final int BRACKET_R_U = 172;
    private static final int PLATE_U = 216;
    private static final int PLATE_SIZE = 24;
    private static final int PLATE_CORNER = 8;
    private static final int DOKKAEBI_U = 216;
    private static final int DOKKAEBI_V = 24;
    private static final int DOKKAEBI_SIZE = 16;
    private static final int LAB_V = 48;
    private static final int LAB_W = 64;
    private static final int LAB_H = 32;

    private static int SCREEN_W = 960;
    private static int SCREEN_H = 540;
    private static final int ADVANCE = 6;
    private static final int LINE_HEIGHT = 9;

    private static BufferedImage atlas;
    private static BufferedImage canvas;
    private static Graphics2D text;

    public static void main(String[] args) throws IOException {
        atlas = ImageIO.read(new File(
                "src/main/resources/assets/orv/textures/gui/status_hud.png"));

        render("bar-mock", 12L, 30L, 100L, 1, "#BIHYUNG-412", "");

        // Narrow screen: verifies the gap-tightening and truncation path.
        SCREEN_W = 640;
        SCREEN_H = 360;
        render("bar-narrow", 12L, 30L, 100L, 1, "#BIHYUNG-412", "");
    }

    private record Seg(int iconU, boolean lit, String label, String value,
                       int labelColor, int valueColor) {
        static final int NO_ICON = -1;
        static final int STAR_ICON = -2;

        int width() {
            int t = Math.max(label.length() * ADVANCE,
                    value == null ? 0 : value.length() * ADVANCE);
            return t + (iconU == NO_ICON ? 0 : ICON_SIZE + ICON_GAP);
        }
    }

    private static void render(String name, long coins, long energy,
                               long maxEnergy, int strength, String channel,
                               String constellation) throws IOException {
        newCanvas();

        int barWidth = Math.max(BAR_MIN_WIDTH,
                Math.min(BAR_MAX_WIDTH, SCREEN_W - BAR_MARGIN * 2));
        int barX = (SCREEN_W - barWidth) / 2;

        // --- bar chrome ---
        rect(barX + 3, BAR_TOP + 3, barX + barWidth - 3,
                BAR_TOP + BAR_HEIGHT - 3, BAR_FILL);
        tile(0, LAB_V, LAB_W, LAB_H, barX + 4, BAR_TOP + 4,
                barWidth - 8, BAR_HEIGHT - 8, 0.14);
        nineSlice(0, 0, FRAME_SIZE, FRAME_CORNER, barX, BAR_TOP,
                barWidth, BAR_HEIGHT);

        int bracketY = BAR_TOP + (BAR_HEIGHT - BRACKET_H) / 2;
        int rightBracketX = barX + barWidth - BRACKET_W;
        blit(barX, bracketY, BRACKET_W, BRACKET_H, BRACKET_L_U, 0,
                BRACKET_W, BRACKET_H, 1.0);
        blit(rightBracketX, bracketY, BRACKET_W, BRACKET_H, BRACKET_R_U, 0,
                BRACKET_W, BRACKET_H, 1.0);
        int rosetteY = bracketY + (BRACKET_H - ROSETTE_SIZE) / 2;
        int rosetteInset = (BRACKET_W - ROSETTE_SIZE) / 2;
        blit(barX + rosetteInset, rosetteY, ROSETTE_SIZE, ROSETTE_SIZE,
                SEAL_U, SEAL_V, ROSETTE_SIZE, ROSETTE_SIZE, 1.0);
        blit(rightBracketX + rosetteInset, rosetteY, ROSETTE_SIZE,
                ROSETTE_SIZE, GEM_U, GEM_V, ROSETTE_SIZE, ROSETTE_SIZE, 1.0);

        // --- segments ---
        boolean lit = !constellation.isBlank();
        String chan = channel.isBlank() ? "OFFLINE" : channel;
        Seg[] segs = {
            new Seg(Seg.NO_ICON, false, "[Kanal: " + chan + "]", null,
                    CHANNEL_TEXT, CHANNEL_TEXT),
            new Seg(ICON_COIN_U, false, "Coins: " + coins, null,
                    COINS_GOLD, COINS_GOLD),
            new Seg(ICON_SWORD_U, false, "Strength: Lv. " + strength, null,
                    STRENGTH_RED, STRENGTH_RED),
            new Seg(ICON_ENERGY_U, false,
                    "Energy: " + energy + " / " + maxEnergy, null,
                    ENERGY_BLUE, ENERGY_BLUE),
            new Seg(Seg.STAR_ICON, lit, "Constellation:",
                    lit ? "[" + constellation + "]" : "[Searching Star Stream]",
                    CONSTELLATION_PURPLE, CONSTELLATION_VALUE),
        };

        int available = barWidth - 2 * BRACKET_W - 8;
        int gaps = segs.length - 1;
        int[] widths = new int[segs.length];
        int content = 0;
        for (int i = 0; i < segs.length; i++) {
            widths[i] = segs[i].width();
            content += widths[i];
        }
        int gap = SEGMENT_GAP;
        if (content + gap * gaps > available && gaps > 0) {
            gap = Math.max(SEGMENT_GAP_MIN,
                    Math.min(SEGMENT_GAP, (available - content) / gaps));
        }
        int overflow = content + gap * gaps - available;
        if (overflow > 0) {
            int last = segs.length - 1;
            int shrink = Math.min(overflow,
                    Math.max(0, widths[last] - CONSTELLATION_MIN_WIDTH));
            widths[last] -= shrink;
            overflow -= shrink;
        }
        if (overflow > 0) {
            widths[0] -= Math.min(overflow, Math.max(0, widths[0] - 40));
        }

        int x = barX + BRACKET_W + 4;
        for (int i = 0; i < segs.length; i++) {
            drawSeg(segs[i], x, widths[i]);
            x += widths[i] + gap;
        }

        // --- hanging plate ---
        String plateLabel = "[SYSTEM STATUS - INCORPORATED]";
        int plateWidth = plateLabel.length() * ADVANCE + 26;
        int plateHeight = 18;
        int plateX = barX + (barWidth - plateWidth) / 2;
        int plateY = BAR_TOP + BAR_HEIGHT - 4;
        nineSlice(PLATE_U, 0, PLATE_SIZE, PLATE_CORNER, plateX, plateY,
                plateWidth, plateHeight);
        draw(plateLabel, plateX + (plateWidth - plateLabel.length() * ADVANCE) / 2,
                plateY + (plateHeight - LINE_HEIGHT) / 2 + 1, PLATE_TEXT, false);
        blit(barX + (barWidth - DOKKAEBI_SIZE) / 2, plateY + plateHeight - 3,
                DOKKAEBI_SIZE, DOKKAEBI_SIZE, DOKKAEBI_U, DOKKAEBI_V,
                DOKKAEBI_SIZE, DOKKAEBI_SIZE, 1.0);

        // --- motes, sampled at a fixed instant ---
        long millis = 5200L;
        int[] moteColors = {GLOW_CYAN, COINS_GOLD, CONSTELLATION_PURPLE};
        for (int i = 0; i < MOTE_COUNT; i++) {
            double speed = 0.05 + (i % 4) * 0.012;
            double phase = ((millis / 1000.0) * speed + i * 0.37) % 1.0;
            int alpha = (int) (Math.sin(phase * Math.PI) * 115.0);
            if (alpha <= 6) continue;
            int travel = (int) (phase * (barWidth - 2 * BRACKET_W));
            int mx = barX + BRACKET_W + travel;
            boolean lower = i % 2 == 1;
            double wobble = Math.sin(millis / 950.0 + i * 1.7) * 2.5;
            int my = (lower ? BAR_TOP + BAR_HEIGHT - 4 : BAR_TOP + 2)
                    + (int) wobble;
            int size = 1 + (i % 2);
            rect(mx, my, mx + size, my + size,
                    (alpha << 24) | (moteColors[i % 3] & 0xFFFFFF));
        }

        hotbarAndHearts();

        File out = new File("/tmp/claude-0/-home-user/"
                + "6ebea994-640a-5f8a-872a-9643c99f5e6a/scratchpad/"
                + "hud-" + name + ".png");
        BufferedImage scaled = new BufferedImage(SCREEN_W * 3, SCREEN_H * 3,
                BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < SCREEN_H * 3; y++)
            for (int px = 0; px < SCREEN_W * 3; px++)
                scaled.setRGB(px, y, canvas.getRGB(px / 3, y / 3));
        ImageIO.write(scaled, "PNG", out);
        System.out.println("Wrote " + out.getPath());
    }

    private static void drawSeg(Seg s, int x, int maxWidth) {
        int textX = x;
        if (s.iconU() != Seg.NO_ICON) {
            int iconY = BAR_TOP + (BAR_HEIGHT - ICON_SIZE) / 2;
            if (s.iconU() == Seg.STAR_ICON) {
                blit(x, iconY, ICON_SIZE, ICON_SIZE,
                        s.lit() ? STAR_ON_U : STAR_OFF_U, STAR_V,
                        ICON_SIZE, ICON_SIZE, 1.0);
            } else {
                blit(x, iconY, ICON_SIZE, ICON_SIZE, s.iconU(), 0,
                        ICON_SIZE, ICON_SIZE, 1.0);
            }
            textX += ICON_SIZE + ICON_GAP;
        }
        int textWidth = Math.max(0, maxWidth - (textX - x));
        if (s.value() == null) {
            draw(fit(s.label(), textWidth), textX,
                    BAR_TOP + (BAR_HEIGHT - LINE_HEIGHT) / 2, s.labelColor(), true);
            return;
        }
        draw(fit(s.label(), textWidth), textX, BAR_TOP + 6, s.labelColor(), true);
        draw(fit(s.value(), textWidth), textX, BAR_TOP + 18, s.valueColor(), true);
    }

    private static String fit(String t, int maxWidth) {
        if (t.length() * ADVANCE <= maxWidth) return t;
        int chars = Math.max(0, maxWidth / ADVANCE - 1);
        return chars == 0 ? "" : t.substring(0, Math.min(chars, t.length())) + "\u2026";
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

    private static void draw(String s, int x, int y, int argb, boolean shadow) {
        if (shadow) {
            text.setColor(new Color(darken(argb), true));
            for (int i = 0; i < s.length(); i++) {
                text.drawString(String.valueOf(s.charAt(i)),
                        x + i * ADVANCE + 1, y + LINE_HEIGHT - 1);
            }
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
                        || sx < 0 || sy < 0 || sx >= ATLAS_W
                        || sy >= ATLAS_H) {
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
