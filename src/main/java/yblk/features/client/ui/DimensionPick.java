package yblk.features.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import yblk.features.client.RegistrySuggest;

import java.util.List;

/** A short list of real dimensions (name and id), including ones added by other mods. */
public final class DimensionPick {
    public static final int ROW = 14;

    private DimensionPick() {
    }

    public static List<RegistrySuggest.Row> visible(String query, int max) {
        List<RegistrySuggest.Row> all = RegistrySuggest.dimensions(query);
        if (all.size() <= max) {
            return all;
        }
        return all.subList(0, max);
    }

    public static void draw(GuiGraphics graphics, Font font, List<RegistrySuggest.Row> rows, String selected, int x, int y, int w) {
        for (int i = 0; i < rows.size(); i++) {
            RegistrySuggest.Row row = rows.get(i);
            int ry = y + i * ROW;
            boolean on = row.id().equals(selected);
            graphics.fill(x, ry, x + w, ry + ROW - 1, on ? 0xFF2A2040 : 0xFF121218);
            graphics.drawString(font, font.plainSubstrByWidth(row.label(), w - 6), x + 3, ry + 3, on ? 0xFFF0F0F0 : 0xFFD0D0D8, false);
        }
    }
}
