package yblk.features.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import yblk.features.client.FeaturesScreen;

/** Panel chrome and choice chips shared by the settings screens. */
public final class UiFrames {
    private UiFrames() {
    }

    public static void panel(GuiGraphics graphics, int x, int y, int w, int h, int fill, int accent) {
        graphics.fill(x, y, x + w, y + h, fill);
        FeaturesScreen.drawGradientBorder(graphics, x, y, w, h, accent, DarkButton.BORDER_BOTTOM);
    }

    public static void chip(GuiGraphics graphics, Font font, int x, int y, int w, int h,
                            Component label, boolean active, boolean hover, int activeFill, int accent) {
        graphics.fill(x, y, x + w, y + h, active ? activeFill : (hover ? 0xFF2A2A32 : DarkButton.IDLE_FILL));
        if (active) {
            graphics.fill(x, y + h - 2, x + w, y + h, accent);
        }
        graphics.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, active ? 0xF0F0F0 : 0x9A9AA8);
    }
}
