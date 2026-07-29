package com.weeeedddd.orv.client.gui;

import net.minecraft.client.gui.Font;

/** Shared palette and text helpers for the guild GUI. */
public final class GuildTheme {

    /** Etched lettering on the polished gold plaque. */
    public static final int PLAQUE_TEXT = 0xFF3B2A0C;
    public static final int PLAQUE_TEXT_DIM = 0xFF6B5219;

    /** Inlaid gold lettering on carved oak. */
    public static final int GOLD_INLAY = 0xFFF2D98A;
    public static final int GOLD = 0xFFD4A73C;

    /** Etched copper used for column headers. */
    public static final int COPPER_ETCH = 0xFFD9975A;
    public static final int COPPER_DIM = 0xFF8F6238;

    /** Body text on dark oak. */
    public static final int TEXT = 0xFFEBDCC2;
    public static final int MUTED = 0xFF9A7B55;

    /** Ink on the parchment card. */
    public static final int INK = 0xFF3A2A16;
    public static final int INK_DIM = 0xFF6A5335;

    public static final int ONLINE = 0xFF7FD98A;
    public static final int OFFLINE = 0xFF6B5A48;
    public static final int DANGER = 0xFFE0705F;

    /** Faint blue rim light on the active tab and the drifting motes. */
    public static final int GLOW = 0xFF7FC8FF;

    /** Backdrop behind the panel. */
    public static final int BACKDROP = 0xC0100A04;

    /**
     * Truncates {@code text} with an ellipsis so it fits {@code maxWidth}.
     * Returns the input untouched when it already fits.
     */
    public static String fit(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int budget = maxWidth - font.width(ellipsis);
        if (budget <= 0) {
            return ellipsis;
        }
        return font.plainSubstrByWidth(text, budget) + ellipsis;
    }

    private GuildTheme() {
    }
}
