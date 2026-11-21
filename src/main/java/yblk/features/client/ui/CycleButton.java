package yblk.features.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import yblk.features.client.FeaturesScreen;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** One button that steps through labels. Same fill and border as {@link DarkButton}. */
public final class CycleButton extends AbstractWidget {
    private final Font font;
    private final List<Component> labels;
    private final IntSupplier index;
    private final IntConsumer onChange;
    private final int accent;
    private final int hoverFill;

    public CycleButton(Font font, int x, int y, int w, int h, List<Component> labels,
                       IntSupplier index, IntConsumer onChange, int accent, int hoverFill) {
        super(x, y, w, h, Component.empty());
        this.font = font;
        this.labels = labels;
        this.index = index;
        this.onChange = onChange;
        this.accent = accent;
        this.hoverFill = hoverFill;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hover = isHoveredOrFocused();
        int i = labels.isEmpty() ? 0 : Math.floorMod(index.getAsInt(), labels.size());
        Component label = labels.isEmpty() ? Component.empty() : labels.get(i);
        graphics.fill(getX(), getY(), getX() + width, getY() + height, hover ? hoverFill : DarkButton.IDLE_FILL);
        FeaturesScreen.drawGradientBorder(graphics, getX(), getY(), width, height, accent, DarkButton.BORDER_BOTTOM);
        graphics.drawCenteredString(font, label, getX() + width / 2, getY() + (height - 8) / 2,
                hover ? 0xF0F0F0 : 0xC0C0C8);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (labels.isEmpty()) {
            return;
        }
        onChange.accept(Math.floorMod(index.getAsInt() + 1, labels.size()));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
