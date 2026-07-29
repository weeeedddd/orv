package com.weeeedddd.orv.client.gui;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The system character sheet: a tall translucent panel over a cascading
 * star-stream backdrop.
 *
 * <p>The readout is fixed to the approved design mock — see {@link #ENTRIES}.
 * Nothing here reads player state yet; wiring it up means replacing that
 * list with data pulled from an attachment.
 */
public final class CharacterInfoScreen extends Screen {

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
    /**
     * Glyph pool for the star stream. Restricted to characters the default
     * font is guaranteed to carry, so no column ever renders as tofu.
     */
    private static final String STREAM_GLYPHS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ<>[]{}/\\|+*#%&$@?!";

    private int scroll;
    private int maxScroll;

    public CharacterInfoScreen() {
        super(Component.literal("System | Character Information"));
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        graphics.fill(0, 0, width, height, BACKDROP);
        renderStarStream(graphics);

        int panelWidth = Math.min(PANEL_WIDTH, width - PANEL_MARGIN * 2);
        int panelHeight = height - PANEL_MARGIN * 2;
        int panelX = (width - panelWidth) / 2;
        int panelY = PANEL_MARGIN;

        renderPanel(graphics, panelX, panelY, panelWidth, panelHeight);
        renderHeader(graphics, panelX, panelY, panelWidth);

        int contentTop = panelY + HEADER_HEIGHT;
        int contentBottom = panelY + panelHeight - CONTENT_INSET;
        int contentHeight = contentBottom - contentTop;

        // Scissor keeps the scrolled body from spilling over the frame.
        graphics.enableScissor(
                panelX + CONTENT_INSET - 4,
                contentTop,
                panelX + panelWidth - CONTENT_INSET + 4,
                contentBottom
        );
        int drawn = renderEntries(
                graphics,
                panelX + CONTENT_INSET,
                contentTop + 4 - scroll,
                panelWidth - CONTENT_INSET * 2
        );
        graphics.disableScissor();

        maxScroll = Math.max(0, drawn + 8 - contentHeight);
        scroll = Math.min(scroll, maxScroll);
        if (maxScroll > 0) {
            renderScrollbar(
                    graphics,
                    panelX + panelWidth - CONTENT_INSET + 8,
                    contentTop,
                    contentHeight
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        if (maxScroll > 0) {
            scroll = Math.max(
                    0,
                    Math.min(maxScroll, scroll - (int) (scrollY * 18))
            );
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------
    // Chrome
    // ---------------------------------------------------------------

    private void renderPanel(
            GuiGraphics graphics,
            int x,
            int y,
            int panelWidth,
            int panelHeight
    ) {
        graphics.fill(x + 6, y + 6, x + panelWidth - 6,
                y + panelHeight - 6, PANEL_FILL);

        // Constellation field behind the readout.
        graphics.setColor(1.0F, 1.0F, 1.0F, 0.35F);
        CharacterAtlas.stars(graphics, x + 7, y + 7,
                panelWidth - 14, panelHeight - 14);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        CharacterAtlas.frame(graphics, x, y, panelWidth, panelHeight);

        // Eyes watch from the upper corners, gears sit at the foot.
        CharacterAtlas.eye(graphics, x + 12, y + 12);
        CharacterAtlas.eye(graphics,
                x + panelWidth - 12 - CharacterAtlas.EYE_SIZE, y + 12);
        CharacterAtlas.gear(graphics, x + 14,
                y + panelHeight - 14 - CharacterAtlas.GEAR_SIZE);
        CharacterAtlas.gear(graphics,
                x + panelWidth - 14 - CharacterAtlas.GEAR_SIZE,
                y + panelHeight - 14 - CharacterAtlas.GEAR_SIZE);
    }

    private void renderHeader(
            GuiGraphics graphics,
            int x,
            int y,
            int panelWidth
    ) {
        String title = "[ SYSTEM | CHARACTER INFORMATION ]";
        graphics.drawString(
                font,
                title,
                x + (panelWidth - font.width(title)) / 2,
                y + 24,
                HEADER_TEXT,
                false
        );
        graphics.fill(x + 40, y + HEADER_HEIGHT - 4,
                x + panelWidth - 40, y + HEADER_HEIGHT - 3, RULE);
    }

    private void renderScrollbar(
            GuiGraphics graphics,
            int x,
            int top,
            int trackHeight
    ) {
        int thumbHeight = Math.max(
                20,
                trackHeight * trackHeight / (trackHeight + maxScroll)
        );
        int travel = trackHeight - thumbHeight;
        int thumbY = top + (maxScroll == 0 ? 0 : travel * scroll / maxScroll);

        graphics.fill(x, top, x + 2, top + trackHeight, 0x3350A0C0);
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, RULE);
    }

    /**
     * Columns of glyphs raining down the whole screen. Each column is
     * seeded from its index, so the pattern is stable frame to frame and
     * only the head position advances.
     */
    private void renderStarStream(GuiGraphics graphics) {
        long millis = Util.getMillis();
        int columns = width / STREAM_COLUMN_SPACING + 1;

        for (int column = 0; column < columns; column++) {
            double speed = 26.0 + (column % 7) * 11.0;
            double phase = (millis / 1000.0 * speed + column * 37.0)
                    % (height + STREAM_TRAIL * LINE_HEIGHT);
            int headY = (int) phase - STREAM_TRAIL * LINE_HEIGHT;
            int x = column * STREAM_COLUMN_SPACING + 2;

            for (int i = 0; i < STREAM_TRAIL; i++) {
                int y = headY + i * LINE_HEIGHT;
                if (y < -LINE_HEIGHT || y > height) {
                    continue;
                }
                // The head is brightest; the tail fades out behind it.
                double falloff = i / (double) STREAM_TRAIL;
                int alpha = (int) (210 * (1.0 - falloff) * (1.0 - falloff));
                if (alpha <= 4) {
                    continue;
                }
                boolean head = i >= STREAM_TRAIL - 2;
                int color = (alpha << 24)
                        | ((head ? STREAM_HEAD : STREAM_BODY) & 0x00FFFFFF);

                // Glyphs shuffle slowly so the columns look alive.
                int glyph = Math.floorMod(
                        column * 31 + i * 17 + (int) (millis / 220),
                        STREAM_GLYPHS.length()
                );
                graphics.drawString(
                        font,
                        String.valueOf(STREAM_GLYPHS.charAt(glyph)),
                        x,
                        y,
                        color,
                        false
                );
            }
        }
    }

    // ---------------------------------------------------------------
    // Readout
    // ---------------------------------------------------------------

    /**
     * Draws every entry and returns the total height consumed, which the
     * caller uses to work out the scroll range.
     */
    private int renderEntries(
            GuiGraphics graphics,
            int x,
            int top,
            int contentWidth
    ) {
        int y = top;

        for (Entry entry : ENTRIES) {
            int labelWidth = font.width(entry.label());
            int valueX = x + labelWidth + LABEL_GAP;

            graphics.drawString(font, entry.label(), x, y, LABEL, false);

            for (int index = 0; index < entry.values().size(); index++) {
                Value value = entry.values().get(index);
                int lineY = y + index * LINE_HEIGHT;
                int textColor = entry.body() ? BODY : VALUE;

                graphics.drawString(
                        font,
                        value.text(),
                        valueX,
                        lineY,
                        textColor,
                        false
                );
                if (value.tag() != null) {
                    graphics.drawString(
                            font,
                            value.tag(),
                            valueX + font.width(value.text()) + 5,
                            lineY,
                            TAG,
                            false
                    );
                }
                if (value.sigil()) {
                    CharacterAtlas.horn(
                            graphics,
                            valueX + font.width(value.text()) + 5,
                            lineY - 4
                    );
                }
            }

            y += Math.max(1, entry.values().size()) * LINE_HEIGHT + ENTRY_GAP;
        }

        return y - top;
    }

    /**
     * One labelled block. {@code body} switches the value colour to prose
     * white, used for the evaluation paragraph.
     */
    private record Entry(String label, boolean body, List<Value> values) {
        Entry(String label, List<Value> values) {
            this(label, false, values);
        }
    }

    /**
     * One value line, optionally followed by a muted tag such as
     * {@code (STOLEN)} or by the dokkaebi sigil.
     */
    private record Value(String text, String tag, boolean sigil) {
        Value(String text) {
            this(text, null, false);
        }

        Value(String text, String tag) {
            this(text, tag, false);
        }
    }

    // Fixed readout, transcribed from the design mock. The spellings
    // "PARTRON OF THE ARTS" and "SAGE'S EUE" are reproduced as drawn.
    private static final List<Entry> ENTRIES = List.of(
            new Entry("NAME:", List.of(new Value("CHOI EUNBYEOL"))),
            new Entry("AGE:", List.of(new Value("25"))),
            new Entry("CONSTELLATION SUPPORT:", List.of(
                    new Value("RADIANT PARIAH", null, true)
            )),
            new Entry("PERSONAL ATTRIBUTES:", List.of(
                    new Value("PARTRON OF THE ARTS", "(RARE)"),
                    new Value("PRIMA BALLERINA ASSOLUTA", "(RARE)"),
                    new Value("PAINTER", "(COMMON)")
            )),
            new Entry("PERSONAL SKILLS:", List.of(
                    new Value("[HIGH FLEXIBILITY LV.5]"),
                    new Value("[PITIFUL CON-ARTIST LV.6]"),
                    new Value("[MUSCLE MEMORY LV.4]"),
                    new Value("[APT STUDENT LV.1]"),
                    new Value("[LIGHT-FOOTED LV.3]"),
                    new Value("[BLACKENING LV.1]", "(STOLEN)"),
                    new Value("[COLD RESISTANCE LV.4]", "(STOLEN)"),
                    new Value("[LIE DETECTION LV.4]", "(STOLEN)"),
                    new Value("[MENTAL BARRIER LV.5]", "(STOLEN)"),
                    new Value("[ADEPT SWIMMER LV.2]", "(STOLEN)"),
                    new Value("[SAGE'S EUE LV.8]", "(STOLEN)"),
                    new Value("[HEAT RESISTANCE LV.6]", "(STOLEN)")
            )),
            new Entry("STIGMAS:", List.of(
                    new Value("[STEAL LV.2]"),
                    new Value("[REGRESSION]", "(STOLEN)"),
                    new Value("[TRANSMISSION]", "(STOLEN)")
            )),
            new Entry("OVERALL STATS:", List.of(
                    new Value("[CONSTITUTION LV.6],"),
                    new Value("[STRENGTH LV.6],"),
                    new Value("[AGILITY LV.4]"),
                    new Value("[MAGIC POWER LV.3]")
            )),
            new Entry("OVERALL EVALUATION:", true, List.of(
                    new Value("PRODIGAL DANCER FALLEN FROM GRACE DUE"),
                    new Value("TO SABOTAGE. RATHER THAN"),
                    new Value("DESPAIRING, CHOSE TO REVEL IN"),
                    new Value("DECEPTION AND USE WHAT SHE HAS LEFT"),
                    new Value("AT HER DISPOSAL. KIND OF PATHETIC AND"),
                    new Value("A WASTE OF GOOD LOOKS.")
            ))
    );
}
