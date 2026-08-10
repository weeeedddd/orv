package com.weeeedddd.orv.client.gui.widget;

import com.weeeedddd.orv.client.gui.GuildAtlas;
import com.weeeedddd.orv.client.gui.GuildTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A carved dark oak tab with inlaid gold lettering. The selected tab sits
 * proud and carries a faint blue rim light along its top edge.
 */
public final class GuildTabButton extends AbstractButton {

    private final Runnable onSelect;
    private final boolean selected;

    public GuildTabButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            boolean selected,
            Runnable onSelect
    ) {
        super(x, y, width, height, message);
        this.selected = selected;
        this.onSelect = onSelect;
    }

    @Override
    public void onPress() {
        onSelect.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    @Override
    protected void renderWidget(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        // The selected tab is drawn two pixels taller so it reads as raised
        // and merges into the content area below it.
        int lift = selected ? 2 : 0;
        int top = getY() - lift;
        int height = getHeight() + lift;

        if (!selected && isHovered()) {
            graphics.setColor(1.12F, 1.10F, 1.06F, 1.0F);
        }
        GuildAtlas.nineSlice(
                graphics,
                selected ? GuildAtlas.TAB_ON : GuildAtlas.TAB_OFF,
                getX(),
                top,
                getWidth(),
                height
        );
        // Flush before clearing the tint; GuiGraphics applies the shader
        // colour when the batch is flushed, not when it is set.
        graphics.flush();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        var font = Minecraft.getInstance().font;
        String label = GuildTheme.fit(
                font,
                getMessage().getString(),
                getWidth() - 10
        );
        graphics.drawString(
                font,
                label,
                getX() + (getWidth() - font.width(label)) / 2,
                top + (height - font.lineHeight) / 2 + 1,
                selected ? GuildTheme.GOLD_INLAY : GuildTheme.COPPER_DIM,
                false
        );
    }
}
