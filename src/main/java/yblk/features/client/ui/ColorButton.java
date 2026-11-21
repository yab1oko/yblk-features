package yblk.features.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import yblk.features.client.FeaturesScreen;

/** 18×12 color swatch. Scrolling screens paint it and keep their own hit target. */
public final class ColorButton {
    private ColorButton() {
    }

    public static void paint(GuiGraphics graphics, int x, int y, int rgb) {
        graphics.fill(x, y, x + 18, y + 12, 0xFF000000 | (rgb & 0xFFFFFF));
        FeaturesScreen.drawGradientBorder(graphics, x, y, 18, 12, 0xFFE0E0E0, 0xFF404040);
    }
}
