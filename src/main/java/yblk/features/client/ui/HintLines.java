package yblk.features.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Wraps a hint inside the panel instead of letting one long line run off the edge. */
public final class HintLines {
    private HintLines() {
    }

    public static int draw(GuiGraphics graphics, Font font, Component text, int x, int y, int width, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(40, width));
        int count = Math.min(maxLines, lines.size());
        for (int i = 0; i < count; i++) {
            graphics.drawString(font, lines.get(i), x, y + i * 10, color, false);
        }
        return count * 10;
    }
}
