import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Offline preview of {@link GuildScreen}'s layout.
 *
 * <p>This is a development aid, not production code: it replays the same
 * nine-slice maths and the same layout constants against the generated
 * atlas so the panel can be eyeballed without launching Minecraft. Text
 * is drawn with a 6px fixed advance, which matches the average advance of
 * Minecraft's default font closely enough to expose overflow and
 * centring mistakes.
 *
 * <p>Constants below are mirrored from GuildScreen; if that file's layout
 * changes, update them here too.
 */
public final class PreviewGuildScreen {

    // Mirrored from GuildScreen.
    private static final int PANEL_MAX_WIDTH = 720;
    private static final int PANEL_HEIGHT = 344;
    private static final int ROW_HEIGHT = 26;
    private static final int MARGIN = 14;
    private static final int SIDEBAR_WIDTH = 40;
    private static final int TAB_HEIGHT = 24;

    // Mirrored from GuildTheme.
    private static final int PLAQUE_TEXT = 0xFF3B2A0C;
    private static final int PLAQUE_TEXT_DIM = 0xFF6B5219;
    private static final int GOLD_INLAY = 0xFFF2D98A;
    private static final int GOLD = 0xFFD4A73C;
    private static final int COPPER_ETCH = 0xFFD9975A;
    private static final int COPPER_DIM = 0xFF8F6238;
    private static final int TEXT = 0xFFEBDCC2;
    private static final int MUTED = 0xFF9A7B55;
    private static final int INK = 0xFF3A2A16;
    private static final int INK_DIM = 0xFF6A5335;
    private static final int ONLINE = 0xFF7FD98A;
    private static final int OFFLINE = 0xFF6B5A48;
    private static final int DANGER = 0xFFE0705F;
    private static final int GLOW = 0xFF7FC8FF;
    private static final int BACKDROP = 0xC0100A04;

    // Mirrored from GuildAtlas.
    private static final int[] FRAME = {64, 0, 48, 16};
    private static final int[] PARCHMENT = {112, 0, 48, 16};
    private static final int[] PLAQUE = {160, 0, 48, 12};
    private static final int[] TAB_OFF = {0, 64, 24, 8};
    private static final int[] TAB_ON = {24, 64, 24, 8};
    private static final int[] ROW_EVEN = {48, 64, 24, 8};
    private static final int[] ROW_ODD = {72, 64, 24, 8};
    private static final int[] SIDEBAR = {96, 64, 32, 12};
    private static final int ICON_V = 96;

    private static final int SCREEN_W = 960;
    private static final int SCREEN_H = 540;
    private static final int ADVANCE = 6;
    private static final int LINE_HEIGHT = 9;

    private static BufferedImage atlas;
    private static BufferedImage canvas;
    private static Graphics2D text;

    public static void main(String[] args) throws IOException {
        atlas = ImageIO.read(new File(
                "src/main/resources/assets/orv/textures/gui/guild_panel.png"));

        render("members", Mode.MEMBERS);
        render("empty", Mode.EMPTY);
        render("quests", Mode.QUESTS);
    }

    private enum Mode { MEMBERS, EMPTY, QUESTS }

    private static void render(String name, Mode mode) throws IOException {
        newCanvas();

        int panelWidth = Math.min(PANEL_MAX_WIDTH, Math.max(400, SCREEN_W - 24));
        int left = (SCREEN_W - panelWidth) / 2;
        int top = Math.max(8, (SCREEN_H - PANEL_HEIGHT) / 2);
        int contentLeft = left + MARGIN;
        int sidebarX = left + panelWidth - MARGIN - SIDEBAR_WIDTH;
        int contentRight = sidebarX - 8;
        int contentWidth = contentRight - contentLeft;
        int tabTop = top + 62;
        int contentTop = top + 92;
        int contentBottom = top + PANEL_HEIGHT - MARGIN;
        int listTop = contentTop + 24;
        int rankX = contentLeft + Math.min(260, contentWidth / 2);

        // Panel chrome.
        tileWood(left, top, panelWidth, PANEL_HEIGHT);
        nineSlice(FRAME, left, top, panelWidth, PANEL_HEIGHT);

        // Plaque.
        int plaqueWidth = Math.min(340, panelWidth - 2 * MARGIN);
        int plaqueX = left + MARGIN;
        int plaqueY = top + 12;
        nineSlice(PLAQUE, plaqueX, plaqueY, plaqueWidth, 38);
        String rank = mode == Mode.EMPTY ? "NO GUILD" : "LEADER";
        String guild = mode == Mode.EMPTY ? "None" : "Golden Dawn";
        draw("[ Guild Console | Rank: " + rank + " ]",
                plaqueX + 10, plaqueY + 8, PLAQUE_TEXT);
        draw("[ GUILD SYSTEM | CURRENT GUILD: " + guild + " ]",
                plaqueX + 10, plaqueY + 21, PLAQUE_TEXT_DIM);

        // Sidebar.
        int sidebarHeight = 8 * 2 + 3 * 28 + 2 * 8;
        nineSlice(SIDEBAR, sidebarX, contentTop - 4, SIDEBAR_WIDTH,
                sidebarHeight);
        int iconSize = 28;
        int iconX = sidebarX + (SIDEBAR_WIDTH - iconSize) / 2;
        for (int i = 0; i < 3; i++) {
            int iconY = contentTop - 4 + 8 + i * (iconSize + 8);
            nineSlice(TAB_OFF, iconX, iconY, iconSize, iconSize);
            blit(iconX + 6, iconY + 6, 16, 16, i * 16, ICON_V, 16, 16);
        }

        // Tabs.
        String[] labels = {"MEMBERS", "INVITE", "GUILD QUESTS"};
        int active = mode == Mode.QUESTS ? 2 : 0;
        int gap = 4;
        int tabWidth = (contentWidth - gap * 2) / 3;
        for (int i = 0; i < 3; i++) {
            boolean on = i == active;
            int lift = on ? 2 : 0;
            int x = contentLeft + i * (tabWidth + gap);
            nineSlice(on ? TAB_ON : TAB_OFF, x, tabTop - lift,
                    tabWidth, TAB_HEIGHT + lift);
            int w = labels[i].length() * ADVANCE;
            draw(labels[i], x + (tabWidth - w) / 2,
                    tabTop - lift + (TAB_HEIGHT + lift - LINE_HEIGHT) / 2 + 1,
                    on ? GOLD_INLAY : COPPER_DIM);
        }

        if (mode == Mode.QUESTS) {
            int cardWidth = Math.min(320, contentWidth - 20);
            int cardX = contentLeft + (contentWidth - cardWidth) / 2;
            int cardY = contentTop + (contentBottom - contentTop - 108) / 2;
            nineSlice(PARCHMENT, cardX, cardY, cardWidth, 108);
            centered("* Guild Quest Archive *", cardX + cardWidth / 2,
                    cardY + 26, INK);
            centered("Scenario contracts will be indexed here.",
                    cardX + cardWidth / 2, cardY + 48, INK_DIM);
            centered("Status: module reserved", cardX + cardWidth / 2,
                    cardY + 70, INK_DIM);
        } else {
            // Column headers.
            int headerY = contentTop + 8;
            draw("MEMBER NAME", contentLeft + 2, headerY, COPPER_ETCH);
            draw("GUILD RANK", rankX, headerY, COPPER_ETCH);
            rect(contentLeft, headerY + 12, contentRight, headerY + 13,
                    COPPER_DIM);

            if (mode == Mode.EMPTY) {
                int cardWidth = Math.min(230, contentWidth - 40);
                int cardHeight = 62;
                int cardX = contentLeft + (contentWidth - cardWidth) / 2;
                int blockHeight = cardHeight + 9 + 24;
                int cardY = contentTop
                        + (contentBottom - contentTop - blockHeight) / 2;
                int cx = cardX + cardWidth / 2;
                rect(cx - 18, cardY + cardHeight, cx + 18,
                        cardY + cardHeight + 4, 0xFF5C3D23);
                rect(cx - 30, cardY + cardHeight + 4, cx + 30,
                        cardY + cardHeight + 7, 0xFF3B2616);
                rect(cx - 30, cardY + cardHeight + 7, cx + 30,
                        cardY + cardHeight + 9, 0xFF241608);
                nineSlice(PARCHMENT, cardX, cardY, cardWidth, cardHeight);
                centered("No entries found.", cx,
                        cardY + cardHeight / 2 - 4, INK);
                centered("Guild Member List: Currently unassigned to a guild.",
                        contentLeft + contentWidth / 2,
                        cardY + cardHeight + 9 + 15, GOLD);
            } else {
                String[] names = {"Kim_Dokja", "YooJoonghyuk", "HanSooyoung",
                        "LeeHyunsung", "JungHeewon", "LeeGilyoung",
                        "ShinYoosung"};
                String[] roles = {"Leader", "Vice-Leader", "Vice-Leader",
                        "Member", "Member", "Member", "Member"};
                for (int i = 0; i < names.length; i++) {
                    int rowY = listTop + i * ROW_HEIGHT;
                    nineSlice(i % 2 == 0 ? ROW_EVEN : ROW_ODD, contentLeft,
                            rowY, contentWidth, ROW_HEIGHT - 4);
                    rect(contentLeft + 10, rowY + 9, contentLeft + 15,
                            rowY + 14, i < 5 ? ONLINE : OFFLINE);
                    draw(names[i], contentLeft + 22, rowY + 7, TEXT);
                    draw(roles[i], rankX, rowY + 7,
                            i == 0 ? GOLD_INLAY
                                    : roles[i].startsWith("Vice")
                                    ? COPPER_ETCH : TEXT);

                    // Action buttons for non-self rows.
                    if (i == 0) {
                        continue;
                    }
                    String[] actions = roles[i].startsWith("Vice")
                            ? new String[] {"Demote", "Transfer", "Kick"}
                            : new String[] {"Promote", "Kick"};
                    int bw = 52;
                    int total = actions.length * bw + (actions.length - 1) * 3;
                    int bx = contentRight - 6 - total;
                    for (String action : actions) {
                        nineSlice(TAB_OFF, bx, rowY + 2, bw, ROW_HEIGHT - 8);
                        int w = action.length() * ADVANCE;
                        draw(action, bx + (bw - w) / 2,
                                rowY + 2 + (ROW_HEIGHT - 8 - LINE_HEIGHT) / 2 + 1,
                                action.equals("Kick") ? DANGER : GOLD_INLAY);
                        bx += bw + 3;
                    }
                }
            }
        }

        // Motes.
        for (int i = 0; i < 16; i++) {
            int corner = i % 4;
            double phase = ((i * 0.37) + 0.25) % 1.0;
            int alpha = (int) (Math.sin(phase * Math.PI) * 110.0);
            if (alpha <= 6) {
                continue;
            }
            double spread = Math.abs(Math.sin(i * 12.9898)) * 30.0;
            double travel = phase * 46.0;
            boolean leftSide = corner == 0 || corner == 2;
            boolean topSide = corner < 2;
            int x = leftSide ? left + 8 + (int) spread
                    : left + panelWidth - 8 - (int) spread;
            int y = topSide ? top + 10 + (int) (travel * 0.55)
                    : top + PANEL_HEIGHT - 10 - (int) travel;
            int size = 1 + (i % 2);
            rect(x, y, x + size, y + size, (alpha << 24) | (GLOW & 0xFFFFFF));
        }

        File out = new File("/tmp/claude-0/-home-user/"
                + "6ebea994-640a-5f8a-872a-9643c99f5e6a/scratchpad/"
                + "preview-" + name + ".png");
        BufferedImage scaled = new BufferedImage(
                SCREEN_W * 2, SCREEN_H * 2, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < SCREEN_H * 2; y++) {
            for (int x = 0; x < SCREEN_W * 2; x++) {
                scaled.setRGB(x, y, canvas.getRGB(x / 2, y / 2));
            }
        }
        ImageIO.write(scaled, "PNG", out);
        System.out.println("Wrote " + out.getPath());
    }

    // -------------------------------------------------------------------

    private static void newCanvas() {
        canvas = new BufferedImage(SCREEN_W, SCREEN_H,
                BufferedImage.TYPE_INT_ARGB);
        // Stand-in for the guild hall behind the GUI.
        for (int y = 0; y < SCREEN_H; y++) {
            for (int x = 0; x < SCREEN_W; x++) {
                double d = Math.hypot(x - SCREEN_W * 0.3, y - SCREEN_H * 0.2)
                        / SCREEN_W;
                int warm = (int) (150 * Math.max(0.0, 1.0 - d * 1.4));
                canvas.setRGB(x, y, 0xFF000000
                        | (Math.min(255, 40 + warm) << 16)
                        | (Math.min(255, 28 + warm * 3 / 4) << 8)
                        | Math.min(255, 18 + warm / 2));
            }
        }
        rect(0, 0, SCREEN_W, SCREEN_H, BACKDROP);

        text = canvas.createGraphics();
        text.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        text.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
    }

    /** Alpha-composites a flat colour over the canvas. */
    private static void rect(int x1, int y1, int x2, int y2, int argb) {
        double a = ((argb >>> 24) & 0xFF) / 255.0;
        for (int y = Math.max(0, y1); y < Math.min(SCREEN_H, y2); y++) {
            for (int x = Math.max(0, x1); x < Math.min(SCREEN_W, x2); x++) {
                canvas.setRGB(x, y, mix(canvas.getRGB(x, y), argb, a));
            }
        }
    }

    /** Draws text at a fixed 6px advance, approximating Minecraft's font. */
    private static void draw(String s, int x, int y, int argb) {
        text.setColor(new Color(argb, true));
        for (int i = 0; i < s.length(); i++) {
            text.drawString(String.valueOf(s.charAt(i)),
                    x + i * ADVANCE, y + LINE_HEIGHT - 2);
        }
    }

    private static void centered(String s, int cx, int y, int argb) {
        draw(s, cx - s.length() * ADVANCE / 2, y, argb);
    }

    private static void nineSlice(int[] slice, int x, int y, int w, int h) {
        int u = slice[0];
        int v = slice[1];
        int size = slice[2];
        int c = slice[3];
        int band = size - c * 2;
        int iw = w - c * 2;
        int ih = h - c * 2;
        int fu = u + size - c;
        int fv = v + size - c;
        int fx = x + w - c;
        int fy = y + h - c;

        blit(x, y, c, c, u, v, c, c);
        blit(fx, y, c, c, fu, v, c, c);
        blit(x, fy, c, c, u, fv, c, c);
        blit(fx, fy, c, c, fu, fv, c, c);
        if (iw > 0) {
            blit(x + c, y, iw, c, u + c, v, band, c);
            blit(x + c, fy, iw, c, u + c, fv, band, c);
        }
        if (ih > 0) {
            blit(x, y + c, c, ih, u, v + c, c, band);
            blit(fx, y + c, c, ih, fu, v + c, c, band);
        }
        if (iw > 0 && ih > 0) {
            blit(x + c, y + c, iw, ih, u + c, v + c, band, band);
        }
    }

    private static void tileWood(int x, int y, int w, int h) {
        for (int oy = 0; oy < h; oy += 64) {
            int th = Math.min(64, h - oy);
            for (int ox = 0; ox < w; ox += 64) {
                int tw = Math.min(64, w - ox);
                blit(x + ox, y + oy, tw, th, 0, 0, tw, th);
            }
        }
    }

    private static void blit(int dx, int dy, int dw, int dh,
                             int u, int v, int sw, int sh) {
        for (int j = 0; j < dh; j++) {
            int sy = v + (int) ((long) j * sh / dh);
            for (int i = 0; i < dw; i++) {
                int sx = u + (int) ((long) i * sw / dw);
                int px = dx + i;
                int py = dy + j;
                if (px < 0 || py < 0 || px >= SCREEN_W || py >= SCREEN_H) {
                    continue;
                }
                int src = atlas.getRGB(sx, sy);
                double a = ((src >>> 24) & 0xFF) / 255.0;
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

    private PreviewGuildScreen() {
    }
}
