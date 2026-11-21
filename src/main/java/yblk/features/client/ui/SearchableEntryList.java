package yblk.features.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Catalog list chrome: selection and hover fills, scroll index, row hit-testing.
 * The screen paints the row contents.
 */
public final class SearchableEntryList<T> {
    public int left;
    public int top;
    public int width;
    public int height;
    public int rowHeight = 22;
    public int scroll;
    public int selectedIndex;
    public int selectedColor = 0xFF1E1E26;
    public int hoverColor = 0xFF1E1E26;

    private List<T> entries = List.of();

    public void setEntries(List<T> entries) {
        this.entries = entries == null ? List.of() : entries;
    }

    public List<T> entries() {
        return entries;
    }

    public int maxScroll() {
        int visible = Math.max(1, height / rowHeight);
        return Math.max(0, entries.size() - visible);
    }

    public void clamp() {
        selectedIndex = Mth.clamp(selectedIndex, 0, Math.max(0, entries.size() - 1));
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, BiConsumer<Row<T>, GuiGraphics> painter) {
        int visible = Math.max(1, height / rowHeight);
        for (int i = 0; i < visible; i++) {
            int index = scroll + i;
            if (index >= entries.size()) {
                break;
            }
            int rowY = top + i * rowHeight;
            boolean selected = index == selectedIndex;
            boolean hovered = mouseX >= left && mouseX < left + width
                    && mouseY >= rowY && mouseY < rowY + rowHeight;
            if (selected) {
                graphics.fill(left, rowY, left + width, rowY + rowHeight, selectedColor);
            } else if (hovered) {
                graphics.fill(left, rowY, left + width, rowY + rowHeight, hoverColor);
            }
            painter.accept(new Row<>(entries.get(index), index, left, rowY, width, rowHeight, selected), graphics);
        }
    }

    public int indexAt(double mouseX, double mouseY) {
        if (mouseX < left || mouseX >= left + width || mouseY < top || mouseY >= top + height) {
            return -1;
        }
        int row = (int) ((mouseY - top) / rowHeight);
        int index = scroll + row;
        if (index < 0 || index >= entries.size()) {
            return -1;
        }
        return index;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < left || mouseX >= left + width || mouseY < top || mouseY >= top + height) {
            return false;
        }
        scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, maxScroll());
        return true;
    }

    public record Row<T>(T entry, int index, int x, int y, int w, int h, boolean selected) {
    }
}
