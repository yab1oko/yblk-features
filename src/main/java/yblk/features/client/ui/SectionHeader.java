package yblk.features.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import yblk.features.client.SettingFocus;

public final class SectionHeader {
    private SectionHeader() {
    }

    public static int draw(GuiGraphics graphics, Font font, int x, int y, Component text) {
        return draw(graphics, font, x, y, text, 12);
    }

    public static int draw(GuiGraphics graphics, Font font, int x, int y, Component text, int advance) {
        SettingFocus.mark(graphics, text, x, y, font.width(text), 8);
        graphics.drawString(font, text, x, y, 0xC8C8D0, false);
        return y + advance;
    }
}
