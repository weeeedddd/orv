import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Generates the status HUD atlas at
 * src/main/resources/assets/orv/textures/gui/status_hud.png.
 *
 * Deterministic: rerunning reproduces the same bytes.
 *
 * Atlas layout (128x128, mirrored by StatusAtlas):
 *   FRAME      (  0,  0)  48x48  9-slice, corner 16, glowing filigree
 *   SEAL       ( 48,  0)  24x24  gear + eye alchemy seal
 *   COIN       ( 72,  0)  16x16  dokkaebi-stamped gold coin
 *   ENERGY     ( 88,  0)  16x16  energy vortex
 *   SWORD      (104,  0)  16x16  sword rune seal
 *   STAR_OFF   ( 72, 16)  16x16  hollow star sigil
 *   STAR_ON    ( 88, 16)  16x16  lit star sigil
 *   HORN       (104, 16)   8x8   dokkaebi horn + eye motif
 *   LABYRINTH  (  0, 48)  64x32  scenario-path glow, tileable in x
 */
public final class GenerateStatusAtlas {

    private static final int SIZE = 128;

    private static final int CYAN = 0xFF00E5FF;
    private static final int CYAN_SOFT = 0xFF55FFFF;
    private static final int GOLD = 0xFFFFD700;
    private static final int GOLD_LIGHT = 0xFFFFE98A;
    private static final int GOLD_DARK = 0xFFA8811F;
    private static final int COPPER = 0xFFB87333;
    private static final int COPPER_LIGHT = 0xFFD9975A;
    private static final int PURPLE = 0xFFAA55FF;
    private static final int PURPLE_LIGHT = 0xFFD9B0FF;
    private static final int RED = 0xFFFF5555;
    private static final int RED_DARK = 0xFF8E2626;
    private static final int OUTLINE = 0xFF12161F;

    private static final BufferedImage IMG =
            new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);

    public static void main(String[] args) throws IOException {
        frame(0, 0);
        seal(48, 0);
        icon(72, 0, COIN);
        vortex(88, 0);
        swordSeal(104, 0);
        star(72, 16, false);
        star(88, 16, true);
        icon(104, 16, HORN);
        labyrinth(0, 48, 64, 32);

        File out = new File(args.length > 0 ? args[0]
                : "src/main/resources/assets/orv/textures/gui/status_hud.png");
        File parent = out.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        ImageIO.write(IMG, "PNG", out);
        System.out.println("Wrote " + out.getPath());
    }

    // ---------------------------------------------------------------
    // Frame
    // ---------------------------------------------------------------

    /**
     * Nine-slice filigree border: a bright gold rule cushioned by copper and
     * a soft outward glow, with constellation nodes punched along the run.
     * The centre stays clear so the panel fill shows through.
     */
    private static void frame(int ox, int oy) {
        int s = 48;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                int dx = Math.min(x, s - 1 - x);
                int dy = Math.min(y, s - 1 - y);
                int d = Math.min(dx, dy);

                Integer color = switch (d) {
                    case 0 -> withAlpha(CYAN, 40);
                    case 1 -> withAlpha(COPPER, 110);
                    case 2 -> GOLD;
                    case 3 -> withAlpha(COPPER_LIGHT, 170);
                    case 4 -> withAlpha(CYAN, 70);
                    case 5 -> withAlpha(CYAN, 26);
                    default -> null;
                };
                if (color != null) {
                    px(ox + x, oy + y, color);
                }

                // Constellation nodes every 8px along the bright rule.
                if (d == 2) {
                    int run = (dx < dy) ? y : x;
                    if (Math.floorMod(run, 8) == 0) {
                        px(ox + x, oy + y, GOLD_LIGHT);
                        // Small cross-glow around each node.
                        if (dx < dy) {
                            px(ox + x - 1, oy + y, withAlpha(GOLD_LIGHT, 150));
                            px(ox + x + 1, oy + y, withAlpha(GOLD_LIGHT, 150));
                        } else {
                            px(ox + x, oy + y - 1, withAlpha(GOLD_LIGHT, 150));
                            px(ox + x, oy + y + 1, withAlpha(GOLD_LIGHT, 150));
                        }
                    }
                }
            }
        }

        // Alchemical corner rosettes. They hug the corner tightly so they
        // stay clear of the panel's content area at any panel size.
        for (int cx : new int[] {7, s - 8}) {
            for (int cy : new int[] {7, s - 8}) {
                ring(ox + cx, oy + cy, 4.6, 3.4, withAlpha(CYAN, 190));
                ring(ox + cx, oy + cy, 2.6, 1.4, withAlpha(GOLD, 220));
                px(ox + cx, oy + cy, GOLD_LIGHT);
            }
        }
    }

    // ---------------------------------------------------------------
    // Seals and icons
    // ---------------------------------------------------------------

    /** 24x24 alchemy seal fusing a toothed gear with a watching eye. */
    private static void seal(int ox, int oy) {
        int s = 24;
        double c = (s - 1) / 2.0;

        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                double dx = x - c;
                double dy = y - c;
                double r = Math.hypot(dx, dy);
                double a = Math.atan2(dy, dx);

                // Gear teeth: eight blocks riding the outer ring.
                double tooth = Math.cos(a * 8.0);
                if (r > 8.4 && r <= 11.2 && tooth > 0.25) {
                    px(ox + x, oy + y, r > 10.2
                            ? withAlpha(COPPER, 210) : COPPER_LIGHT);
                }
                // Gear body ring.
                if (r > 7.4 && r <= 8.8) {
                    px(ox + x, oy + y, GOLD);
                }
                if (r > 6.4 && r <= 7.4) {
                    px(ox + x, oy + y, withAlpha(GOLD_DARK, 220));
                }
                // Inner glow field the eye sits in.
                if (r <= 6.4) {
                    px(ox + x, oy + y, withAlpha(OUTLINE, 190));
                }
            }
        }

        // Almond eye, drawn as two mirrored arcs about the centre line.
        for (int x = -6; x <= 6; x++) {
            double t = x / 6.0;
            int half = (int) Math.round(3.4 * (1.0 - t * t));
            for (int y = -half; y <= half; y++) {
                int px = (int) (ox + c + x);
                int py = (int) (oy + c + y);
                boolean edge = Math.abs(y) >= half;
                px(px, py, edge ? withAlpha(CYAN, 230) : withAlpha(CYAN_SOFT, 90));
            }
        }
        // Pupil.
        for (int y = -2; y <= 2; y++) {
            for (int x = -2; x <= 2; x++) {
                if (x * x + y * y > 4) {
                    continue;
                }
                int px = (int) (ox + c + x);
                int py = (int) (oy + c + y);
                px(px, py, x * x + y * y <= 1 ? GOLD_LIGHT : OUTLINE);
            }
        }
    }

    /** 16x16 two-armed energy vortex. */
    private static void vortex(int ox, int oy) {
        double c = 7.5;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dx = x - c;
                double dy = y - c;
                double r = Math.hypot(dx, dy);
                if (r > 7.4 || r < 0.8) {
                    continue;
                }
                double a = Math.atan2(dy, dx);
                // Two spiral arms: the angle advances with radius, so the
                // arms curl inward toward the core.
                double spiral = (a * 2.0 + r * 1.6) / (2 * Math.PI);
                double f = spiral - Math.floor(spiral);
                if (f < 0.38 || r < 2.2) {
                    // Stay solid near the core and only soften at the rim.
                    int alpha = (int) (120 + (1.0 - r / 7.4) * 135);
                    px(ox + x, oy + y, withAlpha(
                            f < 0.15 || r < 2.2 ? CYAN_SOFT : CYAN,
                            Math.max(120, Math.min(255, alpha))));
                }
            }
        }
        // Bright core.
        px(ox + 7, oy + 7, CYAN_SOFT);
        px(ox + 8, oy + 7, CYAN_SOFT);
        px(ox + 7, oy + 8, CYAN_SOFT);
        px(ox + 8, oy + 8, CYAN_SOFT);
    }

    /** 16x16 seal ring enclosing a sword rune. */
    private static void swordSeal(int ox, int oy) {
        ring(ox + 8, oy + 8, 7.4, 6.4, withAlpha(RED_DARK, 220));
        ring(ox + 8, oy + 8, 6.2, 5.6, withAlpha(RED, 90));
        icon(ox, oy, SWORD);
    }

    /** 16x16 four-point star sigil, hollow when unlit. */
    private static void star(int ox, int oy, boolean lit) {
        for (int y = 0; y < STAR.length; y++) {
            String row = STAR[y];
            for (int x = 0; x < row.length(); x++) {
                char ch = row.charAt(x);
                if (ch == '.') {
                    continue;
                }
                int color;
                if (ch == 'o') {
                    color = lit ? PURPLE_LIGHT : withAlpha(PURPLE, 170);
                } else {
                    if (!lit) {
                        continue;
                    }
                    color = PURPLE;
                }
                px(ox + x, oy + y, color);
            }
        }
        if (lit) {
            px(ox + 7, oy + 7, 0xFFFFFFFF);
            px(ox + 8, oy + 7, 0xFFFFFFFF);
            px(ox + 7, oy + 8, 0xFFFFFFFF);
            px(ox + 8, oy + 8, 0xFFFFFFFF);
        }
    }

    /**
     * Faint labyrinth of orthogonal runs, used as the "scenario path" glow
     * behind the readouts. Tileable along x.
     */
    private static void labyrinth(int ox, int oy, int w, int h) {
        int cell = 8;
        for (int gy = 0; gy < h / cell; gy++) {
            for (int gx = 0; gx < w / cell; gx++) {
                int x = gx * cell;
                int y = gy * cell;
                double pick = hash(gx, gy);

                // Every cell contributes one or two runs, so the pattern
                // reads as a maze rather than as loose dashes.
                if (pick < -0.2) {
                    hLine(ox + x, oy + y + cell / 2, cell);
                } else if (pick < 0.3) {
                    vLine(ox + x + cell / 2, oy + y, cell);
                } else {
                    hLine(ox + x, oy + y + cell / 2, cell / 2 + 1);
                    vLine(ox + x + cell / 2, oy + y, cell / 2 + 1);
                }
            }
        }
    }

    private static void hLine(int x, int y, int length) {
        for (int i = 0; i < length; i++) {
            px(x + i, y, withAlpha(PURPLE, 120));
            px(x + i, y + 1, withAlpha(CYAN, 45));
        }
    }

    private static void vLine(int x, int y, int length) {
        for (int i = 0; i < length; i++) {
            px(x, y + i, withAlpha(PURPLE, 120));
            px(x + 1, y + i, withAlpha(CYAN, 45));
        }
    }

    // ---------------------------------------------------------------
    // Pixel maps
    // ---------------------------------------------------------------

    private static final String[] COIN = {
            ".....oooooo.....",
            "...ooLLLLLLoo...",
            "..oLLGGGGGGLLo..",
            ".oLLGGGGGGGGGLo.",
            ".oLGGdGGGGddGGo.",
            "oLGGGdGGGGdGGGGo",
            "oLGGGGdGGdGGGGGo",
            "oLGGGdddddddGGGo",
            "oLGGGdGGGGdGGGGo",
            "oGGGGdGdGdGGGGGo",
            "oGGGGdddddGGGGGo",
            ".oGGGGdddGGGGGo.",
            ".oGGGGGGGGGGGGo.",
            "..oGGGGGGGGGGo..",
            "...ooGGGGGGoo...",
            ".....oooooo....."
    };

    private static final String[] SWORD = {
            "................",
            "................",
            ".......RR.......",
            ".......RR.......",
            ".......RR.......",
            "......RRRR......",
            "....RRRRRRRR....",
            "......RRRR......",
            ".......RR.......",
            ".......RR.......",
            "......rRRr......",
            ".......RR.......",
            "......rrrr......",
            "................",
            "................",
            "................"
    };

    private static final String[] STAR = {
            "................",
            ".......oo.......",
            ".......pp.......",
            "......oppo......",
            "......oppo......",
            ".....oppppo.....",
            "..oooppppppooo..",
            ".oppppppppppppo.",
            ".oppppppppppppo.",
            "..oooppppppooo..",
            ".....oppppo.....",
            "......oppo......",
            "......oppo......",
            ".......pp.......",
            ".......oo.......",
            "................"
    };

    private static final String[] HORN = {
            "o......o",
            "oC....Co",
            ".oC..Co.",
            "..oCCo..",
            ".oCyyCo.",
            ".oCyyCo.",
            "..oCCo..",
            "...oo..."
    };

    private static void icon(int ox, int oy, String[] rows) {
        for (int y = 0; y < rows.length; y++) {
            String row = rows[y];
            for (int x = 0; x < row.length(); x++) {
                int color = switch (row.charAt(x)) {
                    case 'o' -> OUTLINE;
                    case 'L' -> GOLD_LIGHT;
                    case 'G' -> GOLD;
                    case 'd' -> GOLD_DARK;
                    case 'R' -> RED;
                    case 'r' -> RED_DARK;
                    case 'p' -> PURPLE;
                    case 'C' -> COPPER_LIGHT;
                    case 'y' -> CYAN_SOFT;
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

    /** Draws a filled annulus between {@code outer} and {@code inner}. */
    private static void ring(
            int cx,
            int cy,
            double outer,
            double inner,
            int argb
    ) {
        int span = (int) Math.ceil(outer);
        for (int y = -span; y <= span; y++) {
            for (int x = -span; x <= span; x++) {
                double r = Math.hypot(x, y);
                if (r <= outer && r >= inner) {
                    px(cx + x, cy + y, argb);
                }
            }
        }
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    /** Source-over composite so glows layer instead of replacing. */
    private static void px(int x, int y, int argb) {
        if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) {
            return;
        }
        int src = argb;
        double sa = ((src >>> 24) & 0xFF) / 255.0;
        if (sa >= 1.0) {
            IMG.setRGB(x, y, src);
            return;
        }
        int dst = IMG.getRGB(x, y);
        double da = ((dst >>> 24) & 0xFF) / 255.0;
        double outA = sa + da * (1 - sa);
        if (outA <= 0.0) {
            return;
        }
        int r = channel(src, 16, sa, dst, da, outA);
        int g = channel(src, 8, sa, dst, da, outA);
        int b = channel(src, 0, sa, dst, da, outA);
        IMG.setRGB(x, y, ((int) Math.round(outA * 255) << 24)
                | r << 16 | g << 8 | b);
    }

    private static int channel(
            int src,
            int shift,
            double sa,
            int dst,
            double da,
            double outA
    ) {
        double s = ((src >>> shift) & 0xFF) / 255.0;
        double d = ((dst >>> shift) & 0xFF) / 255.0;
        double v = (s * sa + d * da * (1 - sa)) / outA;
        return (int) Math.round(Math.max(0, Math.min(1, v)) * 255);
    }

    /** Deterministic value noise in [-1, 1]. */
    private static double hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return ((h & 0xFFFF) / 32767.5) - 1.0;
    }

    private GenerateStatusAtlas() {
    }
}
