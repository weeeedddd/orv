package com.weeeedddd.orv.client.gui.widget;

import com.weeeedddd.orv.client.gui.GuildAtlas;
import com.weeeedddd.orv.client.gui.GuildTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A small oak action button with a copper trim on hover. */
public final class WoodButton extends AbstractButton {

    private final Runnable onClick;
    private final int labelColor;

    public WoodButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            int labelColor,
            Runnable onClick
    ) {
        super(x, y, width, height, message);
        this.labelColor = labelColor;
        this.onClick = onClick;
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
            graphics.setColor(0.62F, 0.60F, 0.58F, 1.0F);
        }
        GuildAtlas.nineSlice(
                graphics,
                lit ? GuildAtlas.TAB_ON : GuildAtlas.TAB_OFF,
                getX(),
                getY(),
                getWidth(),
                getHeight()
        );
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        var font = Minecraft.getInstance().font;
        String label = GuildTheme.fit(
                font,
                getMessage().getString(),
                getWidth() - 6
        );
        graphics.drawString(
                font,
                label,
                getX() + (getWidth() - font.width(label)) / 2,
                getY() + (getHeight() - font.lineHeight) / 2 + 1,
                active ? labelColor : GuildTheme.OFFLINE,
                false
        );
    }
}
