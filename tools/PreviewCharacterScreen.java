import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Offline preview of {@link CharacterInfoScreen}'s layout.
 *
 * <p>Development aid, not production code. It replays the same blitting and
 * layout constants against the generated atlas, and draws text at a fixed
 * 6px advance — close enough to Minecraft's default font to expose overflow
 * and centring mistakes, but not glyph-accurate.
 *
 * <p>Constants below are mirrored from CharacterInfoScreen and
 * CharacterAtlas; if those change, update them here too.
 */
public final class PreviewCharacterScreen {

    // Mirrored from CharacterInfoScreen.
    private static final int PANEL_WIDTH = 448;
    private static final int PANEL_MARGIN = 20;
    private static final int CONTENT_INSET = 22;
    private static final int HEADER_HEIGHT = 40;
    private static final int LINE_HEIGHT = 11;
    private static final int ENTRY_GAP = 7;
    private static final int LABEL_GAP = 6;
    private static final int BACKDROP = 0xF2020610;
    private static final int PANEL_FILL = 0xE60A1E33;
    private static final int HEADER_TEXT = 0xFFEAF6FC;
    private static final int LABEL = 0xFFBBDCEC;
    private static final int VALUE = 0xFFE8C87A;
    private static final int TAG = 0xFF7E9AAC;
    private static final int BODY = 0xFFE2EEF5;
    private static final int RULE = 0xFF4FB4D8;
    private static final int STREAM_COLUMN_SPACING = 13;
    private static final int STREAM_TRAIL = 14;
    private static final int STREAM_HEAD = 0xFFB6FFD2;
    private static final int STREAM_BODY = 0xFF3FD07A;
    private static final String STREAM_GLYPHS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ<>[]{}/\\|+*#%&$@?!";

    // Mirrored from CharacterAtlas.
    private static final int ATLAS = 128;
    private static final int FRAME_SIZE = 48;
    private static final int FRAME_CORNER = 16;
    private static final int EYE_U = 48;
    private static final int EYE_SIZE = 24;
    private static final int GEAR_U = 72;
    private static final int GEAR_SIZE = 20;
    private static final int HORN_U = 92;
    private static final int HORN_SIZE = 16;
    private static final int STARS_V = 48;
    private static final int STARS_W = 64;
    private static final int STARS_H = 48;

    private static final int SCREEN_W = 960;
    private static final int SCREEN_H = 540;
    private static final int ADVANCE = 6;

    private static BufferedImage atlas;
    private static BufferedImage canvas;
    private static Graphics2D text;

    private record Value(String text, String tag, boolean sigil) {
    }

    private record Entry(String label, boolean body, Value[] values) {
    }

    private static Value v(String t) {
        return new Value(t, null, false);
    }

    private static Value v(String t, String tag) {
        return new Value(t, tag, false);
    }

    private static final Entry[] ENTRIES = {
        new Entry("NAME:", false, new Value[] {v("CHOI EUNBYEOL")}),
        new Entry("AGE:", false, new Value[] {v("25")}),
        new Entry("CONSTELLATION SUPPORT:", false,
                new Value[] {new Value("RADIANT PARIAH", null, true)}),
        new Entry("PERSONAL ATTRIBUTES:", false, new Value[] {
            v("PARTRON OF THE ARTS", "(RARE)"),
            v("PRIMA BALLERINA ASSOLUTA", "(RARE)"),
            v("PAINTER", "(COMMON)")}),
        new Entry("PERSONAL SKILLS:", false, new Value[] {
            v("[HIGH FLEXIBILITY LV.5]"),
            v("[PITIFUL CON-ARTIST LV.6]"),
            v("[MUSCLE MEMORY LV.4]"),
            v("[APT STUDENT LV.1]"),
            v("[LIGHT-FOOTED LV.3]"),
            v("[BLACKENING LV.1]", "(STOLEN)"),
            v("[COLD RESISTANCE LV.4]", "(STOLEN)"),
            v("[LIE DETECTION LV.4]", "(STOLEN)"),
            v("[MENTAL BARRIER LV.5]", "(STOLEN)"),
            v("[ADEPT SWIMMER LV.2]", "(STOLEN)"),
            v("[SAGE'S EUE LV.8]", "(STOLEN)"),
            v("[HEAT RESISTANCE LV.6]", "(STOLEN)")}),
        new Entry("STIGMAS:", false, new Value[] {
            v("[STEAL LV.2]"),
            v("[REGRESSION]", "(STOLEN)"),
            v("[TRANSMISSION]", "(STOLEN)")}),
        new Entry("OVERALL STATS:", false, new Value[] {
            v("[CONSTITUTION LV.6],"),
            v("[STRENGTH LV.6],"),
            v("[AGILITY LV.4]"),
            v("[MAGIC POWER LV.3]")}),
        new Entry("OVERALL EVALUATION:", true, new Value[] {
            v("PRODIGAL DANCER FALLEN FROM GRACE DUE"),
            v("TO SABOTAGE. RATHER THAN"),
            v("DESPAIRING, CHOSE TO REVEL IN"),
            v("DECEPTION AND USE WHAT SHE HAS LEFT"),
            v("AT HER DISPOSAL. KIND OF PATHETIC AND"),
            v("A WASTE OF GOOD LOOKS.")}),
    };

    public static void main(String[] args) throws IOException {
        atlas = ImageIO.read(new File(
                "src/main/resources/assets/orv/textures/gui/character_panel.png"));
        newCanvas();

        rect(0, 0, SCREEN_W, SCREEN_H, BACKDROP);
        starStream();

        int panelWidth = Math.min(PANEL_WIDTH, SCREEN_W - PANEL_MARGIN * 2);
        int panelHeight = SCREEN_H - PANEL_MARGIN * 2;
        int panelX = (SCREEN_W - panelWidth) / 2;
        int panelY = PANEL_MARGIN;

        // Panel.
        rect(panelX + 6, panelY + 6, panelX + panelWidth - 6,
                panelY + panelHeight - 6, PANEL_FILL);
        tile(0, STARS_V, STARS_W, STARS_H, panelX + 7, panelY + 7,
                panelWidth - 14, panelHeight - 14, 0.35);
        nineSlice(0, 0, FRAME_SIZE, FRAME_CORNER, panelX, panelY,
                panelWidth, panelHeight);
        blit(panelX + 12, panelY + 12, EYE_SIZE, EYE_SIZE, EYE_U, 0,
                EYE_SIZE, EYE_SIZE, 1.0);
        blit(panelX + panelWidth - 12 - EYE_SIZE, panelY + 12,
                EYE_SIZE, EYE_SIZE, EYE_U, 0, EYE_SIZE, EYE_SIZE, 1.0);
        blit(panelX + 14, panelY + panelHeight - 14 - GEAR_SIZE,
                GEAR_SIZE, GEAR_SIZE, GEAR_U, 0, GEAR_SIZE, GEAR_SIZE, 1.0);
        blit(panelX + panelWidth - 14 - GEAR_SIZE,
                panelY + panelHeight - 14 - GEAR_SIZE,
                GEAR_SIZE, GEAR_SIZE, GEAR_U, 0, GEAR_SIZE, GEAR_SIZE, 1.0);

        // Header.
        String title = "[ SYSTEM | CHARACTER INFORMATION ]";
        draw(title, panelX + (panelWidth - title.length() * ADVANCE) / 2,
                panelY + 24, HEADER_TEXT);
        rect(panelX + 40, panelY + HEADER_HEIGHT - 4,
                panelX + panelWidth - 40, panelY + HEADER_HEIGHT - 3, RULE);

        // Entries.
        int x = panelX + CONTENT_INSET;
        int y = panelY + HEADER_HEIGHT + 4;
        for (Entry entry : ENTRIES) {
            int valueX = x + entry.label().length() * ADVANCE + LABEL_GAP;
            draw(entry.label(), x, y, LABEL);
            for (int i = 0; i < entry.values().length; i++) {
                Value value = entry.values()[i];
                int lineY = y + i * LINE_HEIGHT;
                draw(value.text(), valueX, lineY,
                        entry.body() ? BODY : VALUE);
                int after = valueX + value.text().length() * ADVANCE + 5;
                if (value.tag() != null) {
                    draw(value.tag(), after, lineY, TAG);
                }
                if (value.sigil()) {
                    blit(after, lineY - 4, HORN_SIZE, HORN_SIZE, HORN_U, 0,
                            HORN_SIZE, HORN_SIZE, 1.0);
                }
            }
            y += Math.max(1, entry.values().length) * LINE_HEIGHT + ENTRY_GAP;
        }

        int used = y - (panelY + HEADER_HEIGHT + 4);
        int available = panelHeight - HEADER_HEIGHT - CONTENT_INSET;
        System.out.println("content " + used + "px, available " + available
                + "px, overflow " + Math.max(0, used + 8 - available) + "px");

        File out = new File("/tmp/claude-0/-home-user/"
                + "6ebea994-640a-5f8a-872a-9643c99f5e6a/scratchpad/"
                + "character-sheet.png");
        BufferedImage scaled = new BufferedImage(SCREEN_W * 2, SCREEN_H * 2,
                BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < SCREEN_H * 2; j++) {
            for (int i = 0; i < SCREEN_W * 2; i++) {
                scaled.setRGB(i, j, canvas.getRGB(i / 2, j / 2));
            }
        }
        ImageIO.write(scaled, "PNG", out);
        System.out.println("Wrote " + out.getPath());
    }

    private static void starStream() {
        long millis = 3400L;
        int columns = SCREEN_W / STREAM_COLUMN_SPACING + 1;
        for (int column = 0; column < columns; column++) {
            double speed = 26.0 + (column % 7) * 11.0;
            double phase = (millis / 1000.0 * speed + column * 37.0)
                    % (SCREEN_H + STREAM_TRAIL * LINE_HEIGHT);
            int headY = (int) phase - STREAM_TRAIL * LINE_HEIGHT;
            int x = column * STREAM_COLUMN_SPACING + 2;
            for (int i = 0; i < STREAM_TRAIL; i++) {
                int y = headY + i * LINE_HEIGHT;
                if (y < -LINE_HEIGHT || y > SCREEN_H) {
                    continue;
                }
                double falloff = i / (double) STREAM_TRAIL;
                int alpha = (int) (210 * (1.0 - falloff) * (1.0 - falloff));
                if (alpha <= 4) {
                    continue;
                }
                boolean head = i >= STREAM_TRAIL - 2;
                int color = (alpha << 24)
                        | ((head ? STREAM_HEAD : STREAM_BODY) & 0xFFFFFF);
                int glyph = Math.floorMod(
                        column * 31 + i * 17 + (int) (millis / 220),
                        STREAM_GLYPHS.length());
                draw(String.valueOf(STREAM_GLYPHS.charAt(glyph)), x, y, color);
            }
        }
    }

    // -------------------------------------------------------------------

    private static void newCanvas() {
        canvas = new BufferedImage(SCREEN_W, SCREEN_H,
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SCREEN_H; y++) {
            for (int x = 0; x < SCREEN_W; x++) {
                canvas.setRGB(x, y, 0xFF000000);
            }
        }
        text = canvas.createGraphics();
        text.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        text.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
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
        double a = ((argb >>> 24) & 0xFF) / 255.0;
        text.setColor(new Color((int) (((argb >> 16) & 0xFF) * a),
                (int) (((argb >> 8) & 0xFF) * a), (int) ((argb & 0xFF) * a)));
        for (int i = 0; i < s.length(); i++) {
            text.drawString(String.valueOf(s.charAt(i)),
                    x + i * ADVANCE, y + 7);
        }
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
                        || sx < 0 || sy < 0 || sx >= ATLAS || sy >= ATLAS) {
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

    private PreviewCharacterScreen() {
    }
}
