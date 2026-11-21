package yblk.features.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import yblk.features.client.FeaturesScreen;

/**
 * Done / action button used across Features screens.
 * Accent and hover fill stay per-screen so each menu keeps its current colors.
 */
public final class DarkButton extends AbstractWidget {
    public static final int IDLE_FILL = 0xFF1A1A22;
    public static final int BORDER_BOTTOM = 0xFF0A0A0E;

    private final Font font;
    private final Runnable action;
    private final int accent;
    private final int hoverFill;
    private final int idleFill;

    public DarkButton(Font font, int x, int y, int w, int h, Component message, Runnable action,
                      int accent, int hoverFill) {
        this(font, x, y, w, h, message, action, accent, hoverFill, IDLE_FILL);
    }

    public DarkButton(Font font, int x, int y, int w, int h, Component message, Runnable action,
                      int accent, int hoverFill, int idleFill) {
        super(x, y, w, h, message);
        this.font = font;
        this.action = action;
        this.accent = accent;
        this.hoverFill = hoverFill;
        this.idleFill = idleFill;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hover = isHoveredOrFocused();
        graphics.fill(getX(), getY(), getX() + width, getY() + height, hover ? hoverFill : idleFill);
        FeaturesScreen.drawGradientBorder(graphics, getX(), getY(), width, height, accent, BORDER_BOTTOM);
        graphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2,
                hover ? 0xF0F0F0 : 0xC0C0C8);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        action.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
