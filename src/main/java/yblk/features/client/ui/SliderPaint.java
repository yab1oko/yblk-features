package yblk.features.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import yblk.features.client.FeaturesScreen;

public final class SliderPaint {
    private SliderPaint() {
    }

    /** True Night darkness slider: filled track and a 6px knob. Returns the row advance. */
    public static int fillKnob(GuiGraphics graphics, int x, int y, int width, float value,
                               int accent, boolean hover) {
        int trackY = y + 5;
        int trackH = 4;
        float clamped = Mth.clamp(value, 0f, 1f);
        graphics.fill(x, trackY, x + width, trackY + trackH, 0xFF2A2A32);
        int fillR = x + Math.round(width * clamped);
        if (fillR > x) {
            graphics.fill(x, trackY, fillR, trackY + trackH, accent);
        }
        int knobW = 6;
        int knobX = Mth.clamp(fillR - knobW / 2, x, x + width - knobW);
        graphics.fill(knobX, trackY - 2, knobX + knobW, trackY + trackH + 2,
                hover ? 0xFFF0F0F0 : 0xFFC0C0C8);
        return y + 14 + 4;
    }

    /** Color picker channel: tinted fill, white knob, value is 0..255. */
    public static void channel(GuiGraphics graphics, net.minecraft.client.gui.Font font,
                               int labelX, int trackX, int trackW, int y, String label, int value, int tint, int accent) {
        graphics.drawString(font, label, labelX, y + 1, 0xE0E0E8, false);
        graphics.fill(trackX, y + 4, trackX + trackW, y + 8, 0xFF2A2A32);
        graphics.fill(trackX, y + 4, trackX + Math.round(trackW * (value / 255f)), y + 8, tint);
        int knobX = trackX + Math.round(trackW * (value / 255f)) - 3;
        graphics.fill(knobX, y + 1, knobX + 6, y + 11, 0xFFF0F0F0);
        FeaturesScreen.drawGradientBorder(graphics, knobX, y + 1, 6, 10, accent, 0xFF303038);
        graphics.drawString(font, Integer.toString(value), trackX + trackW + 6, y + 1, 0xC8C8D0, false);
    }

    /** Real First Person offset slider: 2px track and a 4px thumb. */
    public static void thinThumb(GuiGraphics graphics, int left, int right, int y, int height,
                                 float t, boolean selected, int accent) {
        int thumbX = left + Math.round((right - left - 4) * Mth.clamp(t, 0f, 1f));
        graphics.fill(left, y + 5, right, y + 7, 0xFF2A2A32);
        graphics.fill(thumbX, y + 2, thumbX + 4, y + height - 2, selected ? accent : 0xFF6A8A9A);
    }
}
