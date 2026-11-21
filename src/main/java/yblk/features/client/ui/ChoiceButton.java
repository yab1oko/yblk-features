package yblk.features.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/** Selected-state chip: tab or mode. Underline uses the screen accent. */
public final class ChoiceButton extends AbstractWidget {
    private final Font font;
    private final BooleanSupplier selected;
    private final Runnable onSelect;
    private final int accent;
    private final int activeFill;

    public ChoiceButton(Font font, int x, int y, int w, int h, Component message,
                        BooleanSupplier selected, Runnable onSelect, int accent, int activeFill) {
        super(x, y, w, h, message);
        this.font = font;
        this.selected = selected;
        this.onSelect = onSelect;
        this.accent = accent;
        this.activeFill = activeFill;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean active = selected.getAsBoolean();
        UiFrames.chip(graphics, font, getX(), getY(), width, height, getMessage(), active, false, activeFill, accent);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onSelect.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
