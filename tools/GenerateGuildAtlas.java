import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Generates the guild GUI atlas at
 * src/main/resources/assets/orv/textures/gui/guild_panel.png.
 *
 * The atlas is deterministic: rerunning it reproduces the exact same
 * bytes, so the checked-in PNG stays stable across machines.
 *
 * Atlas layout (256x256, see GuildScreen for the matching constants):
 *   WOOD       (  0,  0)  64x64  tileable dark oak planking
 *   FRAME      ( 64,  0)  48x48  9-slice, corner 16, copper/gold border
 *   PARCHMENT  (112,  0)  48x48  9-slice, corner 16, aged paper card
 *   PLAQUE     (160,  0)  48x48  9-slice, corner 12, polished gold
 *   TAB_OFF    (  0, 64)  24x24  9-slice, corner 8, carved oak plank
 *   TAB_ON     ( 24, 64)  24x24  9-slice, corner 8, raised + blue glow
 *   ROW_EVEN   ( 48, 64)  24x24  9-slice, corner 8, inset list row
 *   ROW_ODD    ( 72, 64)  24x24  9-slice, corner 8, inset list row
 *   SIDEBAR    ( 96, 64)  32x32  9-slice, corner 12, bracketed panel
 *   ICON_QUILL (  0, 96)  16x16
 *   ICON_HANDS ( 16, 96)  16x16
 *   ICON_KEY   ( 32, 96)  16x16
 */
public final class GenerateGuildAtlas {

    private static final int SIZE = 256;

    // Dark oak
    private static final int OAK_DEEP = 0xFF241608;
    private static final int OAK_BASE = 0xFF3B2616;
    private static final int OAK_MID = 0xFF4A2F1B;
    private static final int OAK_LIGHT = 0xFF5C3D23;

    // Copper
    private static final int COPPER_DARK = 0xFF6E4119;
    private static final int COPPER_BASE = 0xFFB87333;
    private static final int COPPER_LIGHT = 0xFFD9975A;

    // Gold
    private static final int GOLD_DARK = 0xFF8C6A1E;
    private static final int GOLD_BASE = 0xFFD4A73C;
    private static final int GOLD_LIGHT = 0xFFF2D98A;

    // Parchment
    private static final int PARCH_BASE = 0xFFE8D5A8;
    private static final int PARCH_SHADE = 0xFFC9AE7C;
    private static final int PARCH_EDGE = 0xFF9C7F51;

    private static final int OUTLINE = 0xFF140B04;
    private static final int GLOW = 0xFF7FC8FF;

    private static final BufferedImage IMG =
            new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);

    public static void main(String[] args) throws IOException {
        wood(0, 0, 64, 64);
        frame(64, 0);
        parchment(112, 0);
        plaque(160, 0);
        tab(0, 64, false);
        tab(24, 64, true);
        listRow(48, 64, 0x30000000);
        listRow(72, 64, 0x16FFD9A8);
        sidebar(96, 64);
        icon(0, 96, QUILL);
        icon(16, 96, HANDS);
        icon(32, 96, KEY);

        File out = new File(args.length > 0 ? args[0]
                : "src/main/resources/assets/orv/textures/gui/guild_panel.png");
        File parent = out.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        ImageIO.write(IMG, "PNG", out);
        System.out.println("Wrote " + out.getPath());
    }

    // ---------------------------------------------------------------
    // Regions
    // ---------------------------------------------------------------

    /** Tileable dark oak planking; grain runs horizontally. */
    private static void wood(int ox, int oy, int w, int h) {
        for (int y = 0; y < h; y++) {
            int localY = y % 16;
            boolean seam = localY == 0;
            boolean underSeam = localY == 1;
            boolean bottomLip = localY == 15;

            for (int x = 0; x < w; x++) {
                int color;
                if (seam) {
                    color = OAK_DEEP;
                } else if (underSeam) {
                    color = blend(OAK_BASE, OAK_LIGHT, 0.35);
                } else if (bottomLip) {
                    color = blend(OAK_BASE, OAK_DEEP, 0.45);
                } else {
                    // Row tone varies per plank so planks read separately.
                    double rowTone = hash(x / 64, y) * 0.18
                            + hash(y / 16, localY * 7) * 0.22;
                    // Streaks are periodic in x, so the tile wraps cleanly.
                    double streak =
                            Math.sin(2 * Math.PI * x * 3 / (double) w + y * 0.9)
                                    * 0.30
                            + Math.sin(2 * Math.PI * x * 7 / (double) w + y * 1.7)
                                    * 0.18
                            + Math.sin(2 * Math.PI * x * 13 / (double) w + y * 2.6)
                                    * 0.10;
                    double t = clamp01(0.45 + rowTone * 0.5 + streak * 0.28);
                    color = blend(OAK_DEEP, OAK_LIGHT, t);
                    // Occasional tight grain line for a polished look.
                    if (streak < -0.44) {
                        color = blend(color, OAK_DEEP, 0.55);
                    } else if (streak > 0.46) {
                        color = blend(color, OAK_MID, 0.40);
                    }
                }
                px(ox + x, oy + y, color);
            }
        }
    }

    /**
     * 9-slice copper frame with an etched band, an inner gold rule and
     * corner rivets. The centre stays transparent: the frame is an
     * overlay drawn on top of the wood body.
     */
    private static void frame(int ox, int oy) {
        int s = 48;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                // Distance to the nearest edge, mirrored so all four
                // sides and both slice seams stay symmetric.
                int dx = Math.min(x, s - 1 - x);
                int dy = Math.min(y, s - 1 - y);
                int d = Math.min(dx, dy);

                Integer color = null;
                switch (d) {
                    case 0 -> color = OUTLINE;
                    case 1 -> color = blend(COPPER_DARK, OUTLINE, 0.30);
                    case 2 -> {
                        // Bevel: light on top/left, dark on bottom/right.
                        boolean lit = (dx == 2 && x < s / 2)
                                || (dy == 2 && y < s / 2);
                        color = lit ? COPPER_LIGHT : COPPER_DARK;
                    }
                    case 3, 4, 5 -> {
                        // Etched band, period 8 along the run so the
                        // stretched edge slices tile seamlessly.
                        int run = (dx < dy) ? y : x;
                        int etch = Math.floorMod(run, 8);
                        double t = (etch == 0 || etch == 4) ? 0.20
                                : (etch == 2 || etch == 6) ? 0.72 : 0.45;
                        color = blend(COPPER_DARK, COPPER_LIGHT, t);
                    }
                    case 6 -> color = GOLD_BASE;
                    case 7 -> color = GOLD_DARK;
                    case 8 -> color = blend(OAK_DEEP, OUTLINE, 0.40);
                    default -> color = null;
                }

                if (color != null) {
                    px(ox + x, oy + y, color);
                }
            }
        }
        // Corner rivets, inside the 16px corner slices.
        for (int cx : new int[] {8, s - 9}) {
            for (int cy : new int[] {8, s - 9}) {
                rivet(ox + cx, oy + cy);
            }
        }
    }

    /** Small domed gold rivet centred on (cx, cy). */
    private static void rivet(int cx, int cy) {
        for (int y = -2; y <= 2; y++) {
            for (int x = -2; x <= 2; x++) {
                int r2 = x * x + y * y;
                if (r2 > 4) {
                    continue;
                }
                int color = r2 == 4 ? OUTLINE
                        : (x <= 0 && y <= 0) ? GOLD_LIGHT
                        : (r2 <= 1) ? GOLD_BASE : GOLD_DARK;
                px(cx + x, cy + y, color);
            }
        }
    }

    /** 9-slice aged parchment card with a darkened, worn border. */
    private static void parchment(int ox, int oy) {
        int s = 48;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                int d = Math.min(Math.min(x, s - 1 - x),
                        Math.min(y, s - 1 - y));
                int color;
                if (d == 0) {
                    color = blend(PARCH_EDGE, OUTLINE, 0.55);
                } else if (d == 1) {
                    color = PARCH_EDGE;
                } else if (d == 2) {
                    color = blend(PARCH_SHADE, PARCH_EDGE, 0.45);
                } else {
                    // Mottled fibre texture, periodic so slices tile.
                    double mottle =
                            Math.sin(2 * Math.PI * x * 5 / (double) s + y * 1.1)
                                    * 0.5
                            + Math.sin(2 * Math.PI * y * 7 / (double) s + x * 0.8)
                                    * 0.5;
                    double t = clamp01(0.62 + mottle * 0.22
                            + hash(x, y) * 0.16);
                    color = blend(PARCH_SHADE, PARCH_BASE, t);
                    // Soft inner shadow just inside the border.
                    if (d == 3) {
                        color = blend(color, PARCH_SHADE, 0.45);
                    }
                }
                px(ox + x, oy + y, color);
            }
        }
    }

    /** 9-slice polished gold plaque with a vertical sheen. */
    private static void plaque(int ox, int oy) {
        int s = 48;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                int dx = Math.min(x, s - 1 - x);
                int dy = Math.min(y, s - 1 - y);
                int d = Math.min(dx, dy);
                int color;
                if (d == 0) {
                    color = OUTLINE;
                } else if (d == 1) {
                    boolean lit = (dy == 1 && y < s / 2)
                            || (dx == 1 && x < s / 2);
                    color = lit ? GOLD_LIGHT : GOLD_DARK;
                } else {
                    // Sheen runs bright at the top and deepens toward the
                    // bottom. The middle band is flat and the two ends meet
                    // it at the same value, so stretching the 9-slice centre
                    // cannot reveal a seam.
                    double t;
                    if (y < 12) {
                        t = lerp(0.92, 0.55, (y - 2) / 10.0);
                    } else if (y >= s - 12) {
                        t = lerp(0.55, 0.28, (y - (s - 12)) / 11.0);
                    } else {
                        t = 0.55;
                    }
                    color = blend(GOLD_DARK, GOLD_LIGHT,
                            clamp01(t + hash(x, y) * 0.06));
                    if (d == 2) {
                        color = blend(color, GOLD_DARK, 0.35);
                    }
                }
                px(ox + x, oy + y, color);
            }
        }
    }

    /**
     * 9-slice carved oak tab. The active variant sits proud and carries
     * a faint blue glow along its top edge.
     */
    private static void tab(int ox, int oy, boolean active) {
        int s = 24;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                int dx = Math.min(x, s - 1 - x);
                int dy = Math.min(y, s - 1 - y);
                int d = Math.min(dx, dy);
                int color;
                if (d == 0) {
                    color = OUTLINE;
                } else if (d == 1) {
                    boolean lit = (dy == 1 && y < s / 2)
                            || (dx == 1 && x < s / 2);
                    color = active
                            ? (lit ? COPPER_LIGHT : COPPER_DARK)
                            : (lit ? OAK_LIGHT : OAK_DEEP);
                } else {
                    double streak =
                            Math.sin(2 * Math.PI * x * 3 / (double) s + y * 1.4)
                                    * 0.5;
                    double t = clamp01((active ? 0.58 : 0.38)
                            + streak * 0.16 + hash(x, y) * 0.12);
                    color = blend(OAK_DEEP, OAK_LIGHT, t);
                }
                px(ox + x, oy + y, color);
            }
        }

        if (active) {
            // Faint blue rim light along the top edge only.
            for (int x = 1; x < s - 1; x++) {
                px(ox + x, oy + 1, blend(GLOW, COPPER_LIGHT, 0.45));
                px(ox + x, oy + 2, blend(peek(ox + x, oy + 2), GLOW, 0.28));
                px(ox + x, oy + 3, blend(peek(ox + x, oy + 3), GLOW, 0.12));
            }
        }
    }

    /** 9-slice inset list row; {@code tintArgb} is composited on top. */
    private static void listRow(int ox, int oy, int tintArgb) {
        int s = 24;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                int dy = Math.min(y, s - 1 - y);
                int color;
                if (dy == 0) {
                    // Carved-in look: dark on top, light on bottom.
                    color = y == 0 ? 0xFF1C1108 : blend(OAK_LIGHT, OAK_BASE, 0.5);
                } else {
                    double t = clamp01(0.34 + hash(x, y) * 0.14);
                    color = blend(OAK_DEEP, OAK_MID, t);
                }
                px(ox + x, oy + y, over(tintArgb, color));
            }
        }
    }

    /** 9-slice sidebar panel: polished wood held by copper brackets. */
    private static void sidebar(int ox, int oy) {
        int s = 32;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                int dx = Math.min(x, s - 1 - x);
                int dy = Math.min(y, s - 1 - y);
                int d = Math.min(dx, dy);
                int color;
                if (d == 0) {
                    color = OUTLINE;
                } else if (d == 1) {
                    color = COPPER_BASE;
                } else if (d == 2) {
                    boolean lit = (dy == 2 && y < s / 2)
                            || (dx == 2 && x < s / 2);
                    color = lit ? COPPER_LIGHT : COPPER_DARK;
                } else if (d == 3) {
                    color = blend(OAK_DEEP, OUTLINE, 0.35);
                } else {
                    double t = clamp01(0.42
                            + Math.sin(2 * Math.PI * y * 4 / (double) s) * 0.14
                            + hash(x, y) * 0.14);
                    color = blend(OAK_DEEP, OAK_MID, t);
                }
                px(ox + x, oy + y, color);
            }
        }
        // Bracket rivets inside the 12px corner slices.
        for (int cx : new int[] {6, s - 7}) {
            for (int cy : new int[] {6, s - 7}) {
                rivet(ox + cx, oy + cy);
            }
        }
    }

    // ---------------------------------------------------------------
    // Icons
    // ---------------------------------------------------------------

    private static final String[] QUILL = {
            "...........oo...",
            "..........oGGo..",
            ".........oGwGo..",
            "........oGwGo...",
            ".......oGwGo....",
            "......oGwGo.....",
            ".....oGwGo......",
            "....oGwGo.......",
            "...oGwGo........",
            "...oowo.........",
            "................",
            ".oooooooooooo...",
            ".opppppppppqo...",
            ".opqppqpppppo...",
            ".oppppppqpppo...",
            ".oooooooooooo...",
    };

    /** Sealed invitation letter: reads far better at 16px than a handshake. */
    private static final String[] HANDS = {
            "................",
            "................",
            ".oooooooooooooo.",
            ".opppppppppppqo.",
            ".oppqpppppppqpo.",
            ".opppqpppppqppo.",
            ".oppppqpppqpppo.",
            ".opppppqqpppppo.",
            ".oppppRRRRppppo.",
            ".oppppRrrRppppo.",
            ".oppppRRRRppppo.",
            ".opppppppppppqo.",
            ".oppppppppppppo.",
            ".oooooooooooooo.",
            "................",
            "................",
    };

    private static final String[] KEY = {
            "................",
            "..oooo..........",
            ".oGGGGo.........",
            "oGGdoGGo........",
            "oGdo.oGo........",
            "oGGdoGGo........",
            ".oGGGGo.........",
            "..oGGGo.........",
            "...oGGGo........",
            "....oGGGo.......",
            ".....oGGGo......",
            "......oGGGo.....",
            ".......oGGGoo...",
            "........oGGGGo..",
            ".........oGooGo.",
            "..........ooooo.",
    };

    private static void icon(int ox, int oy, String[] rows) {
        for (int y = 0; y < rows.length; y++) {
            String row = rows[y];
            for (int x = 0; x < row.length(); x++) {
                int color = switch (row.charAt(x)) {
                    case 'o' -> OUTLINE;
                    case 'p' -> PARCH_BASE;
                    case 'q' -> PARCH_SHADE;
                    case 'w' -> 0xFFF7F2E4;
                    case 'g' -> GOLD_BASE;
                    case 'G' -> GOLD_LIGHT;
                    case 'd' -> GOLD_DARK;
                    case 'c' -> COPPER_BASE;
                    case 'C' -> COPPER_LIGHT;
                    case 'k' -> COPPER_DARK;
                    case 'R' -> 0xFF8E2626;
                    case 'r' -> 0xFFC94040;
                    default -> 0;
                };
                if (color != 0) {
                    px(ox + x, oy + y, color);
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static void px(int x, int y, int argb) {
        if (x >= 0 && y >= 0 && x < SIZE && y < SIZE) {
            IMG.setRGB(x, y, argb);
        }
    }

    private static int peek(int x, int y) {
        return IMG.getRGB(x, y);
    }

    /** Linear blend; {@code t} of 0 yields {@code a}, 1 yields {@code b}. */
    private static int blend(int a, int b, double t) {
        double f = clamp01(t);
        int alpha = (int) Math.round(ch(a, 24) + (ch(b, 24) - ch(a, 24)) * f);
        int red = (int) Math.round(ch(a, 16) + (ch(b, 16) - ch(a, 16)) * f);
        int green = (int) Math.round(ch(a, 8) + (ch(b, 8) - ch(a, 8)) * f);
        int blue = (int) Math.round(ch(a, 0) + (ch(b, 0) - ch(a, 0)) * f);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    /** Source-over composite of {@code src} onto opaque {@code dst}. */
    private static int over(int src, int dst) {
        double a = ch(src, 24) / 255.0;
        return blend(dst, 0xFF000000 | (src & 0xFFFFFF), a);
    }

    private static int ch(int argb, int shift) {
        return (argb >>> shift) & 0xFF;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * clamp01(t);
    }

    /** Deterministic value noise in [-1, 1]. */
    private static double hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return ((h & 0xFFFF) / 32767.5) - 1.0;
    }

    private GenerateGuildAtlas() {
    }
}
