package yblk.features.client.ui;

import net.minecraft.client.gui.GuiGraphics;

import java.util.function.IntConsumer;

/**
 * Scrollable clip region. Screens still place their own rows; this only owns
 * the scissor and the matching scrollbar so both stay lined up.
 */
public final class ScrollPanel {
    private ScrollPanel() {
    }

    public static void begin(GuiGraphics graphics, int left, int top, int right, int bottom) {
        graphics.enableScissor(left + 2, top + 2, right - 2, bottom - 2);
    }

    public static void end(GuiGraphics graphics, int right, int top, int bottom,
                           int scroll, int maxScroll, int thumbColor, IntConsumer onScroll) {
        graphics.disableScissor();
        ScrollBar.draw(graphics, right, top, bottom, scroll, maxScroll, thumbColor, onScroll);
    }
}
