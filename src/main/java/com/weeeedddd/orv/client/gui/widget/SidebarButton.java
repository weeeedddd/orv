package com.weeeedddd.orv.client.gui.widget;

import com.weeeedddd.orv.client.gui.GuildAtlas;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * An icon button seated in the sidebar. The icon itself carries the
 * meaning; the label is used for the tooltip and for narration.
 */
public final class SidebarButton extends AbstractButton {

    private final Runnable onClick;
    private final int iconU;

    public SidebarButton(
            int x,
            int y,
            int size,
            Component message,
            int iconU,
            Runnable onClick
    ) {
        super(x, y, size, size, message);
        this.iconU = iconU;
        this.onClick = onClick;
        setTooltip(Tooltip.create(message));
    }

    @Override
    public void onPress() {
        onClick.run();
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
        boolean lit = active && isHovered();
        if (!active) {
            graphics.setColor(0.55F, 0.53F, 0.50F, 1.0F);
        }
        GuildAtlas.nineSlice(
                graphics,
                lit ? GuildAtlas.TAB_ON : GuildAtlas.TAB_OFF,
                getX(),
                getY(),
                getWidth(),
                getHeight()
        );
        GuildAtlas.icon(
                graphics,
                iconU,
                getX() + (getWidth() - GuildAtlas.ICON_SIZE) / 2,
                getY() + (getHeight() - GuildAtlas.ICON_SIZE) / 2
        );
        // Flush before clearing the tint; GuiGraphics applies the shader
        // colour when the batch is flushed, not when it is set.
        graphics.flush();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
