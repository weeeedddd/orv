package com.weeeedddd.orv.client.gui;

import com.weeeedddd.orv.character.CharacterProfile;
import com.weeeedddd.orv.client.character.CharacterClientCache;
import com.weeeedddd.orv.client.system.SystemDataClientCache;
import com.weeeedddd.orv.client.system.SystemDataSnapshot;
import com.weeeedddd.orv.network.SyncCharacterProfilePayload;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The system character sheet: a tall translucent panel over a cascading
 * star-stream backdrop.
 *
 * <p>Every row is built from live server data: the character profile
 * arrives on {@code orv:character_sync} and the economy values on
 * {@code orv:system_sync}. Nothing on this screen is hardcoded.
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

        // Constellation field behind the readout. The batch has to be
        // flushed while the tint is still set: GuiGraphics applies the
        // shader colour at flush time, so resetting it first would draw the
        // field at full opacity on top of the text.
        graphics.setColor(1.0F, 1.0F, 1.0F, 0.35F);
        CharacterAtlas.stars(graphics, x + 7, y + 7,
                panelWidth - 14, panelHeight - 14);
        graphics.flush();
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

        // Chrome first, then the readout batches on top.
        graphics.flush();
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

        for (Entry entry : buildEntries()) {
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

    /**
     * Builds the sheet from the two client caches. Lists that the server
     * has not populated render an explicit empty row rather than vanishing,
     * so the section headings stay stable between players.
     */
    private List<Entry> buildEntries() {
        SyncCharacterProfilePayload sheet = CharacterClientCache.snapshot();
        SystemDataSnapshot system = minecraft == null
                || minecraft.player == null
                ? null
                : SystemDataClientCache
                        .find(minecraft.player.getUUID())
                        .orElse(null);

        if (sheet == null) {
            return List.of(new Entry(
                    "STATUS:",
                    true,
                    List.of(new Value("Awaiting system sync..."))
            ));
        }

        CharacterProfile profile = sheet.profile();
        List<Entry> entries = new ArrayList<>();

        entries.add(new Entry("NAME:",
                List.of(new Value(sheet.playerName().toUpperCase()))));
        entries.add(new Entry("LEVEL:",
                List.of(new Value("LV. " + sheet.level()))));
        if (profile.age() > 0) {
            entries.add(new Entry("AGE:",
                    List.of(new Value(String.valueOf(profile.age())))));
        }

        boolean bound = system != null
                && !system.constellationName().isBlank();
        entries.add(new Entry("CONSTELLATION SUPPORT:", List.of(new Value(
                bound
                        ? system.constellationName().toUpperCase()
                        : "[SEARCHING STAR STREAM]",
                null,
                bound
        ))));

        entries.add(new Entry("COINS:", List.of(new Value(
                system == null ? "0" : String.valueOf(system.coins())
        ))));
        entries.add(new Entry("ENERGY:", List.of(new Value(
                system == null
                        ? "0 / 0"
                        : system.energy() + " / " + system.maxEnergy()
        ))));

        entries.add(new Entry("PERSONAL ATTRIBUTES:",
                attributeRows(profile.attributes())));
        entries.add(new Entry("PERSONAL SKILLS:",
                skillRows(profile.skills())));
        entries.add(new Entry("STIGMAS:", skillRows(sheet.stigmas())));

        return entries;
    }

    private static List<Value> skillRows(List<CharacterProfile.Skill> skills) {
        if (skills.isEmpty()) {
            return List.of(new Value("[NONE RECORDED]"));
        }
        List<Value> rows = new ArrayList<>(skills.size());
        for (CharacterProfile.Skill skill : skills) {
            rows.add(new Value(
                    skill.display(),
                    skill.stolen() ? "(STOLEN)" : null
            ));
        }
        return rows;
    }

    private static List<Value> attributeRows(
            List<CharacterProfile.Attribute> attributes
    ) {
        if (attributes.isEmpty()) {
            return List.of(new Value("[NONE RECORDED]"));
        }
        List<Value> rows = new ArrayList<>(attributes.size());
        for (CharacterProfile.Attribute attribute : attributes) {
            rows.add(new Value(
                    attribute.display(),
                    attribute.rarity().tag()
            ));
        }
        return rows;
    }
}
