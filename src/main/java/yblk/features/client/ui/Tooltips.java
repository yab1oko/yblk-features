package yblk.features.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class Tooltips {
    private Tooltips() {
    }

    public static void show(GuiGraphics graphics, Font font, Component text, int mouseX, int mouseY) {
        graphics.renderTooltip(font, text, mouseX, mouseY);
    }
}
