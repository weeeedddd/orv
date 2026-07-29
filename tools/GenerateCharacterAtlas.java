import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Generates the character sheet atlas at
 * src/main/resources/assets/orv/textures/gui/character_panel.png.
 *
 * Deterministic: rerunning reproduces the same bytes.
 *
 * Atlas layout (128x128, mirrored by CharacterAtlas):
 *   FRAME  (  0,  0)  48x48  9-slice, corner 16, twin glowing rules
 *   EYE    ( 48,  0)  24x24  watching-eye sigil for the top corners
 *   GEAR   ( 72,  0)  20x20  small clockwork gear
 *   HORN   ( 92,  0)  16x16  dokkaebi horn-and-eye sigil
 *   STARS  (  0, 48)  64x48  tileable constellation field
 */
public final class GenerateCharacterAtlas {

    private static final int SIZE = 128;

    private static final int GLOW = 0xFF7FD4E8;
    private static final int RULE = 0xFF4FB4D8;
    private static final int MID = 0xFF3E8FB0;
    private static final int DEEP = 0xFF10344A;
    private static final int NAVY = 0xFF081A2E;
    private static final int GOLD = 0xFFE8C87A;
    private static final int STAR = 0xFFDCEEF8;
    private static final int OUTLINE = 0xFF04101C;

    private static final BufferedImage IMG =
            new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);

    public static void main(String[] args) throws IOException {
        frame(0, 0);
        eye(48, 0);
        gear(72, 0);
        horn(92, 0);
        stars(0, 48, 64, 48);

        File out = new File(args.length > 0 ? args[0]
                : "src/main/resources/assets/orv/textures/gui/character_panel.png");
        File parent = out.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        ImageIO.write(IMG, "PNG", out);
        System.out.println("Wrote " + out.getPath());
    }

    /**
     * Twin glowing rules with a dark channel between them, plus a soft
     * outward bloom. The centre stays clear so the panel fill shows through.
     */
    private static void frame(int ox, int oy) {
        int s = 48;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                int dx = Math.min(x, s - 1 - x);
                int dy = Math.min(y, s - 1 - y);
                int d = Math.min(dx, dy);

                Integer color = switch (d) {
                    case 0 -> withAlpha(GLOW, 55);
                    case 1 -> withAlpha(GLOW, 130);
                    case 2 -> GLOW;
                    case 3 -> withAlpha(DEEP, 220);
                    case 4 -> withAlpha(NAVY, 235);
                    case 5 -> RULE;
                    case 6 -> withAlpha(GLOW, 90);
                    case 7 -> withAlpha(GLOW, 30);
                    default -> null;
                };
                if (color != null) {
                    px(ox + x, oy + y, color);
                }

                // Tick marks every 8px along the outer rule.
                if (d == 2 && Math.floorMod((dx < dy) ? y : x, 8) == 0) {
                    px(ox + x, oy + y, 0xFFFFFFFF);
                }
            }
        }

        // Corner brackets: short right angles set inside the rules.
        for (int i = 0; i < 7; i++) {
            for (int[] c : new int[][] {{9 + i, 9}, {9, 9 + i},
                    {s - 10 - i, 9}, {s - 10, 9 + i},
                    {9 + i, s - 10}, {9, s - 10 - i},
                    {s - 10 - i, s - 10}, {s - 10, s - 10 - i}}) {
                px(ox + c[0], oy + c[1], i < 2 ? GOLD : withAlpha(GOLD, 170));
            }
        }
    }

    /** 24x24 watching-eye sigil ringed by radiating ticks. */
    private static void eye(int ox, int oy) {
        int s = 24;
        double c = (s - 1) / 2.0;

        ring(ox + (int) c, oy + (int) c, 11.4, 10.2, withAlpha(GLOW, 210));
        ring(ox + (int) c, oy + (int) c, 8.6, 7.8, withAlpha(RULE, 180));

        // Radiating ticks between the two rings.
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6.0;
            for (double r = 8.8; r <= 10.0; r += 0.5) {
                px((int) Math.round(ox + c + Math.cos(a) * r),
                        (int) Math.round(oy + c + Math.sin(a) * r),
                        withAlpha(GLOW, 200));
            }
        }

        // Almond eye.
        for (int x = -6; x <= 6; x++) {
            double t = x / 6.0;
            int half = (int) Math.round(3.6 * (1.0 - t * t));
            for (int y = -half; y <= half; y++) {
                boolean edge = Math.abs(y) >= half;
                px((int) (ox + c + x), (int) (oy + c + y),
                        edge ? GLOW : withAlpha(DEEP, 210));
            }
        }
        for (int y = -2; y <= 2; y++) {
            for (int x = -2; x <= 2; x++) {
                if (x * x + y * y > 4) {
                    continue;
                }
                px((int) (ox + c + x), (int) (oy + c + y),
                        x * x + y * y <= 1 ? 0xFFFFFFFF : GOLD);
            }
        }
    }

    /** 20x20 clockwork gear. */
    private static void gear(int ox, int oy) {
        int s = 20;
        double c = (s - 1) / 2.0;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                double dx = x - c;
                double dy = y - c;
                double r = Math.hypot(dx, dy);
                double a = Math.atan2(dy, dx);

                if (r > 6.8 && r <= 9.2 && Math.cos(a * 8.0) > 0.30) {
                    px(ox + x, oy + y, withAlpha(RULE, 220));
                }
                if (r > 5.4 && r <= 7.0) {
                    px(ox + x, oy + y, GLOW);
                }
                if (r > 2.2 && r <= 4.0) {
                    px(ox + x, oy + y, withAlpha(RULE, 200));
                }
            }
        }
    }

    /** 16x16 dokkaebi horn-and-eye sigil. */
    private static void horn(int ox, int oy) {
        String[] rows = {
                "................",
                "..G..........G..",
                "..GG........GG..",
                "...GG......GG...",
                "....oKKKKKKo....",
                "...oKKKKKKKKo...",
                "..oKKyyKKyyKKo..",
                "..oKKyyKKyyKKo..",
                "..oKKKKKKKKKKo..",
                "..oKKKKKKKKKKo..",
                "..oKKKggggKKKo..",
                "...oKKKKKKKKo...",
                "....oooKKooo....",
                ".......oo.......",
                "................",
                "................"
        };
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                int color = switch (rows[y].charAt(x)) {
                    case 'o' -> OUTLINE;
                    case 'K' -> 0xFF11283C;
                    case 'y' -> GLOW;
                    case 'g' -> GOLD;
                    case 'G' -> withAlpha(GOLD, 230);
                    default -> 0;
                };
                if (color != 0) {
                    px(ox + x, oy + y, color);
                }
            }
        }
    }

    /**
     * Tileable constellation field: scattered stars joined by faint lines,
     * drawn at low alpha so it sits behind the readout text.
     */
    private static void stars(int ox, int oy, int w, int h) {
        int[][] points = new int[14][2];
        for (int i = 0; i < points.length; i++) {
            points[i][0] = (int) ((hash(i, 7) * 0.5 + 0.5) * (w - 1));
            points[i][1] = (int) ((hash(i, 19) * 0.5 + 0.5) * (h - 1));
        }

        // Join each point to its nearest follower, so the field reads as
        // constellations rather than as loose noise.
        for (int i = 0; i < points.length; i++) {
            int best = -1;
            double bestDistance = Double.MAX_VALUE;
            for (int j = i + 1; j < points.length; j++) {
                double distance = Math.hypot(
                        points[i][0] - points[j][0],
                        points[i][1] - points[j][1]
                );
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = j;
                }
            }
            if (best >= 0) {
                line(ox, oy, points[i], points[best], w, h);
            }
        }

        for (int[] p : points) {
            px(ox + p[0], oy + p[1], withAlpha(STAR, 190));
            px(ox + p[0] - 1, oy + p[1], withAlpha(STAR, 70));
            px(ox + p[0] + 1, oy + p[1], withAlpha(STAR, 70));
            px(ox + p[0], oy + p[1] - 1, withAlpha(STAR, 70));
            px(ox + p[0], oy + p[1] + 1, withAlpha(STAR, 70));
        }
    }

    private static void line(int ox, int oy, int[] a, int[] b, int w, int h) {
        int steps = (int) Math.max(
                Math.abs(b[0] - a[0]),
                Math.abs(b[1] - a[1])
        );
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            int x = (int) Math.round(a[0] + (b[0] - a[0]) * t);
            int y = (int) Math.round(a[1] + (b[1] - a[1]) * t);
            if (x >= 0 && y >= 0 && x < w && y < h) {
                px(ox + x, oy + y, withAlpha(RULE, 55));
            }
        }
    }

    // ---------------------------------------------------------------

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
        double sa = ((argb >>> 24) & 0xFF) / 255.0;
        if (sa >= 1.0) {
            IMG.setRGB(x, y, argb);
            return;
        }
        int dst = IMG.getRGB(x, y);
        double da = ((dst >>> 24) & 0xFF) / 255.0;
        double outA = sa + da * (1 - sa);
        if (outA <= 0.0) {
            return;
        }
        int r = channel(argb, 16, sa, dst, da, outA);
        int g = channel(argb, 8, sa, dst, da, outA);
        int b = channel(argb, 0, sa, dst, da, outA);
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

    private GenerateCharacterAtlas() {
    }
}
