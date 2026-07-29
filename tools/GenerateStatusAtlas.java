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
 * Atlas layout (256x128, mirrored by StatusAtlas):
 *   FRAME      (  0,  0)  48x48  9-slice, corner 16, glowing filigree
 *   SEAL       ( 48,  0)  24x24  gear + eye alchemy seal
 *   COIN       ( 72,  0)  16x16  dokkaebi-stamped gold coin
 *   ENERGY     ( 88,  0)  16x16  energy vortex
 *   SWORD      (104,  0)  16x16  sword rune seal
 *   STAR_OFF   ( 72, 16)  16x16  hollow star sigil
 *   STAR_ON    ( 88, 16)  16x16  lit star sigil
 *   HORN       (104, 16)   8x8   dokkaebi horn + eye motif
 *   GEM        ( 48, 24)  24x24  faceted gem rosette (right bracket)
 *   LABYRINTH  (  0, 48)  64x32  scenario-path glow, tileable in x
 *   BRACKET_L  (128,  0)  44x56  ornate left end bracket
 *   BRACKET_R  (172,  0)  44x56  ornate right end bracket
 *   PLATE      (216,  0)  24x24  9-slice, corner 8, hanging title plate
 *   DOKKAEBI   (216, 24)  16x16  horned channel-master head
 */
public final class GenerateStatusAtlas {

    private static final int WIDTH = 256;
    private static final int HEIGHT = 128;

    private static final int CYAN = 0xFF00E5FF;
    private static final int CYAN_SOFT = 0xFF55FFFF;
    private static final int GOLD = 0xFFFFD700;
    private static final int GOLD_LIGHT = 0xFFFFE98A;
    private static final int GOLD_DARK = 0xFFA8811F;
    private static final int COPPER = 0xFFB87333;
    private static final int COPPER_LIGHT = 0xFFD9975A;
    private static final int COPPER_DARK = 0xFF6E4119;
    private static final int PURPLE = 0xFFAA55FF;
    private static final int PURPLE_LIGHT = 0xFFD9B0FF;
    private static final int RED = 0xFFFF5555;
    private static final int RED_DARK = 0xFF8E2626;
    private static final int OUTLINE = 0xFF12161F;

    private static final BufferedImage IMG =
            new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);

    public static void main(String[] args) throws IOException {
        frame(0, 0);
        seal(48, 0);
        icon(72, 0, COIN);
        vortex(88, 0);
        swordSeal(104, 0);
        star(72, 16, false);
        star(88, 16, true);
        icon(104, 16, HORN);
        gem(48, 24);
        labyrinth(0, 48, 64, 32);
        bracket(128, 0, false);
        bracket(172, 0, true);
        plate(216, 0);
        icon(216, 24, DOKKAEBI);

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

    /** 24x24 faceted gem rosette seated in a bronze collar. */
    private static void gem(int ox, int oy) {
        int s = 24;
        double c = (s - 1) / 2.0;

        ring(ox + (int) c, oy + (int) c, 11.4, 9.0, COPPER);
        ring(ox + (int) c, oy + (int) c, 10.4, 9.6, COPPER_LIGHT);

        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                double dx = x - c;
                double dy = y - c;
                double r = Math.hypot(dx, dy);
                if (r > 8.6) {
                    continue;
                }
                // Facets: brighten toward the upper left, darken opposite.
                double facet = (-dx - dy) / 12.0;
                double t = clamp01(0.5 + facet * 0.55 - r / 22.0);
                px(ox + x, oy + y, blend(0xFF4B1E7A, PURPLE_LIGHT, t));
            }
        }
        // Specular glint.
        px(ox + 8, oy + 7, 0xFFFFFFFF);
        px(ox + 9, oy + 7, 0xFFFFFFFF);
        px(ox + 8, oy + 8, withAlpha(0xFFFFFFFF, 190));
    }

    /**
     * 44x56 ornate end bracket: a bevelled bronze plate with a recessed
     * socket for the rosette, plus a scrolled crown and foot that overhang
     * the bar. {@code mirrored} flips the scrollwork so the curls always
     * face outward.
     */
    private static void bracket(int ox, int oy, boolean mirrored) {
        int w = 44;
        int h = 56;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sx = mirrored ? w - 1 - x : x;

                // Stepped silhouette: the plate narrows toward the crown
                // and the foot so the volutes read as separate castings.
                int inset;
                if (y < 4 || y >= h - 4) {
                    inset = 10;
                } else if (y < 8 || y >= h - 8) {
                    inset = 7;
                } else if (y < 11 || y >= h - 11) {
                    inset = 4;
                } else {
                    inset = 0;
                }
                if (sx < inset || sx >= w - inset) {
                    continue;
                }

                int dx = Math.min(sx - inset, w - 1 - inset - sx);
                int dy = Math.min(y, h - 1 - y);
                int d = Math.min(dx, dy);

                int color;
                if (d == 0) {
                    color = OUTLINE;
                } else if (d == 1) {
                    color = (y < h / 2) ? COPPER_LIGHT : COPPER_DARK;
                } else if (d <= 3) {
                    color = blend(COPPER, COPPER_LIGHT,
                            (y < h / 2) ? 0.55 : 0.15);
                } else {
                    color = blend(COPPER_DARK, COPPER, 0.45);
                }
                px(ox + x, oy + y, color);
            }
        }

        // Recessed socket the rosette sits in.
        int cx = ox + w / 2;
        int cy = oy + h / 2;
        ring(cx, cy, 13.2, 12.0, OUTLINE);
        for (int y = -12; y <= 12; y++) {
            for (int x = -12; x <= 12; x++) {
                if (Math.hypot(x, y) <= 12.0) {
                    px(cx + x, cy + y, withAlpha(OUTLINE, 225));
                }
            }
        }

        // Volutes curling outward at the crown and the foot.
        int outerX = mirrored ? ox + w - 12 : ox + 11;
        int innerX = mirrored ? ox + w - 23 : ox + 22;
        scroll(outerX, oy + 9, mirrored, 4.6);
        scroll(innerX, oy + 6, mirrored, 2.8);
        scroll(outerX, oy + h - 10, mirrored, 4.6);
        scroll(innerX, oy + h - 7, mirrored, 2.8);
    }

    /** Curled scroll used on the bracket crown and foot. */
    private static void scroll(
            int cx,
            int cy,
            boolean mirrored,
            double outerRadius
    ) {
        int dir = mirrored ? -1 : 1;
        int turns = Math.max(2, (int) Math.round(outerRadius / 1.5));
        for (int i = 0; i < turns; i++) {
            double radius = outerRadius - i * 1.4;
            if (radius < 0.8) {
                break;
            }
            for (double a = -Math.PI * 0.2; a < Math.PI * 1.15; a += 0.08) {
                int x = (int) Math.round(cx + Math.cos(a) * radius * dir);
                int y = (int) Math.round(cy - Math.sin(a) * radius);
                px(x, y, i == 0 ? COPPER_LIGHT : (i == 1 ? GOLD : GOLD_LIGHT));
            }
        }
    }

    /** 24x24 nine-slice plate that hangs below the bar. */
    private static void plate(int ox, int oy) {
        int w = 24;
        int h = 24;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int dx = Math.min(x, w - 1 - x);
                int dy = Math.min(y, h - 1 - y);
                int d = Math.min(dx, dy);

                int color;
                if (d == 0) {
                    color = OUTLINE;
                } else if (d == 1) {
                    color = (y < h / 2) ? COPPER_LIGHT : COPPER_DARK;
                } else if (d == 2) {
                    color = COPPER;
                } else if (d == 3) {
                    color = blend(COPPER_DARK, OUTLINE, 0.5);
                } else {
                    color = withAlpha(0xFF0A0C12, 236);
                }
                px(ox + x, oy + y, color);
            }
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

    private static final String[] DOKKAEBI = {
            "................",
            ".C............C.",
            ".CC..........CC.",
            "..CC........CC..",
            "..oCkkkkkkkkCo..",
            "..okkkkkkkkkko..",
            ".okkyykkkkyykko.",
            ".okkyykkkkyykko.",
            ".okkkkkkkkkkkko.",
            ".okkkkkkkkkkkko.",
            ".okkkRRRRRRkkko.",
            "..okkkkkkkkkko..",
            "...oookkkkooo...",
            "......oooo......",
            "................",
            "................"
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
                    case 'k' -> 0xFF1B1F2B;
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

    /** Linear blend; {@code t} of 0 yields {@code a}, 1 yields {@code b}. */
    private static int blend(int a, int b, double t) {
        double f = clamp01(t);
        int alpha = (int) Math.round(comp(a, 24) + (comp(b, 24) - comp(a, 24)) * f);
        int red = (int) Math.round(comp(a, 16) + (comp(b, 16) - comp(a, 16)) * f);
        int green = (int) Math.round(comp(a, 8) + (comp(b, 8) - comp(a, 8)) * f);
        int blue = (int) Math.round(comp(a, 0) + (comp(b, 0) - comp(a, 0)) * f);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int comp(int argb, int shift) {
        return (argb >>> shift) & 0xFF;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    /** Source-over composite so glows layer instead of replacing. */
    private static void px(int x, int y, int argb) {
        if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) {
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
