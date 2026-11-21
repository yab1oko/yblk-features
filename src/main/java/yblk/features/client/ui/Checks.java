package yblk.features.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import yblk.features.client.SettingFocus;

import java.util.function.Consumer;

public final class Checks {
    public static final int FRAME = 0xFFE8E8F0;

    private Checks() {
    }

    public static void box(GuiGraphics graphics, int x, int y, int size, boolean on, int mark) {
        graphics.fill(x, y, x + size, y + 1, FRAME);
        graphics.fill(x, y + size - 1, x + size, y + size, FRAME);
        graphics.fill(x, y, x + 1, y + size, FRAME);
        graphics.fill(x + size - 1, y, x + size, y + size, FRAME);
        if (on) {
            graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, mark);
        }
    }

    public static void cross(GuiGraphics graphics, int x, int y, int size, boolean on, int crossColor) {
        graphics.fill(x, y, x + size, y + 1, FRAME);
        graphics.fill(x, y + size - 1, x + size, y + size, FRAME);
        graphics.fill(x, y, x + 1, y + size, FRAME);
        graphics.fill(x + size - 1, y, x + size, y + size, FRAME);
        if (on) {
            for (int i = 2; i < size - 2; i++) {
                graphics.fill(x + i, y + i, x + i + 1, y + i + 1, crossColor);
                graphics.fill(x + size - 1 - i, y + i, x + size - i, y + i + 1, crossColor);
            }
        }
    }

    /**
     * @param markOnlyWhenInteractive local-chat style: a locked row draws an empty box
     * @param dimWhenLocked           overlay the box when the row cannot be clicked
     * @param labelColor              fixed label color, or {@code -1} for the on/off/locked palette
     */
    public static int row(GuiGraphics graphics, Font font, int x, int y, int rowH, int check, int hitW,
                          boolean on, boolean interactive, int mark, Component label,
                          boolean dimWhenLocked, boolean markOnlyWhenInteractive, int labelGap, int labelColor,
                          Consumer<ClickHit> hits, Runnable action) {
        boolean painted = markOnlyWhenInteractive ? on && interactive : on;
        box(graphics, x, y + 3, check, painted, mark);
        if (dimWhenLocked && !interactive) {
            graphics.fill(x, y + 3, x + check, y + 3 + check, 0x66000000);
        }
        int color = labelColor >= 0
                ? labelColor
                : (interactive ? (on ? 0xF0F0F0 : 0x9A9AA8) : 0x606068);
        SettingFocus.mark(graphics, label, x, y, hitW, rowH);
        graphics.drawString(font, label, x + check + labelGap, y + 3, color, false);
        if (interactive && hits != null && action != null) {
            hits.accept(new ClickHit(x, y, hitW, rowH - 2, action));
        }
        return y + rowH;
    }
}
