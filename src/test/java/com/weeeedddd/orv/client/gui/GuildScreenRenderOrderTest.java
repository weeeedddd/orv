package com.weeeedddd.orv.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class GuildScreenRenderOrderTest {

    @Test
    void rendersWorldBlurBeforeGuildContentAndDoesNotRepeatIt() throws Exception {
        GuildScreen screen = spy(new GuildScreen());
        GuiGraphics graphics = mock(GuiGraphics.class);
        setFont(screen, mock(Font.class));
        screen.width = 800;
        screen.height = 400;

        doNothing().when(screen).renderWorldBackground(
                same(graphics),
                anyInt(),
                anyInt(),
                anyFloat()
        );

        screen.render(graphics, 12, 34, 0.5F);

        InOrder order = inOrder(screen, graphics);
        order.verify(screen).renderWorldBackground(graphics, 12, 34, 0.5F);
        order.verify(graphics).fill(
                0,
                0,
                screen.width,
                screen.height,
                GuildTheme.BACKDROP
        );
        order.verify(screen).renderBackground(graphics, 12, 34, 0.5F);
        verify(screen, times(1)).renderWorldBackground(
                graphics,
                12,
                34,
                0.5F
        );
    }

    private static void setFont(GuildScreen screen, Font font)
            throws ReflectiveOperationException {
        Field field = Screen.class.getDeclaredField("font");
        field.setAccessible(true);
        field.set(screen, font);
    }
}
