package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import yblk.features.Features;

import java.util.Locale;
import java.util.function.IntConsumer;

public class ColorPickerScreen extends Screen {
        private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFFE88C40;

    private final Screen parent;
    private final IntConsumer onApply;
    private final int initial;

    private int r;
    private int g;
    private int b;
    private EditBox hexBox;
    private int dragging = -1; // 0=R 1=G 2=B
    private int panelL;
    private int panelTop;
    private int sliderL;
    private int sliderW;

    public ColorPickerScreen(Screen parent, Component title, int rgb, IntConsumer onApply) {
        super(title);
        this.parent = parent;
        this.onApply = onApply;
        this.initial = rgb & 0xFFFFFF;
        this.r = (initial >> 16) & 0xFF;
        this.g = (initial >> 8) & 0xFF;
        this.b = initial & 0xFF;
    }

    @Override
    protected void init() {
        clearWidgets();
        int panelW = Math.min(280, width - 24);
        panelL = (width - panelW) / 2;
        panelTop = Math.max(24, height / 2 - 90);
        sliderL = panelL + 36;
        sliderW = panelW - 48 - 28;

        hexBox = new EditBox(font, panelL + 12, panelTop + 118, 90, 16, Component.literal("hex"));
        hexBox.setMaxLength(6);
        hexBox.setFilter(s -> s.chars().allMatch(c -> "0123456789abcdefABCDEF".indexOf(c) >= 0));
        hexBox.setValue(toHex(rgb()));
        hexBox.setResponder(this::onHexTyped);
        addRenderableWidget(hexBox);

        int btnY = panelTop + 148;
        int btnW = 80;
        addRenderableWidget(new DarkButton(width / 2 - btnW - 4, btnY, btnW, 18,
                Component.translatable("gui.done"), () -> {
                    onApply.accept(rgb());
                    onClose();
                }));
        addRenderableWidget(new DarkButton(width / 2 + 4, btnY, btnW, 18,
                Component.translatable("gui.cancel"), this::onClose));
    }

    private void onHexTyped(String raw) {
        if (raw == null || raw.length() != 6) {
            return;
        }
        try {
            int value = Integer.parseInt(raw, 16);
            r = (value >> 16) & 0xFF;
            g = (value >> 8) & 0xFF;
            b = value & 0xFF;
        } catch (NumberFormatException ignored) {
        }
    }

    private void syncHex() {
        if (hexBox == null || hexBox.isFocused()) {
            return;
        }
        String next = toHex(rgb());
        if (!next.equalsIgnoreCase(hexBox.getValue())) {
            hexBox.setValue(next);
        }
    }

    private int rgb() {
        return (r << 16) | (g << 8) | b;
    }

    private static String toHex(int rgb) {
        return String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, panelTop - 14, 0xF0F0F0);

        int panelW = Math.min(280, width - 24);
        int panelH = 176;
        graphics.fill(panelL, panelTop, panelL + panelW, panelTop + panelH, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelTop, panelW, panelH, ACCENT, 0xFF0A0A0E);

        int previewX = panelL + panelW - 52;
        int previewY = panelTop + 14;
        graphics.fill(previewX, previewY, previewX + 36, previewY + 36, 0xFF000000 | rgb());
        FeaturesScreen.drawGradientBorder(graphics, previewX, previewY, 36, 36, 0xFFE0E0E0, 0xFF404040);
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".color_picker.preview"),
                panelL + 12, previewY + 4, 0xA0A0AA, false);
        graphics.drawString(font, "#" + toHex(rgb()), panelL + 12, previewY + 16, 0xE0E0E8, false);

        drawSlider(graphics, 0, panelTop + 60, r, 0xFFE06060, mouseX, mouseY);
        drawSlider(graphics, 1, panelTop + 78, g, 0xFF60E060, mouseX, mouseY);
        drawSlider(graphics, 2, panelTop + 96, b, 0xFF6090E0, mouseX, mouseY);

        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".color_picker.hex"),
                panelL + 12, panelTop + 108, 0xA0A0AA, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSlider(GuiGraphics g, int channel, int y, int value, int tint, int mx, int my) {
        String label = channel == 0 ? "R" : channel == 1 ? "G" : "B";
        g.drawString(font, label, panelL + 12, y + 1, 0xE0E0E8, false);
        g.fill(sliderL, y + 4, sliderL + sliderW, y + 8, 0xFF2A2A32);
        g.fill(sliderL, y + 4, sliderL + Math.round(sliderW * (value / 255f)), y + 8, tint);
        int knobX = sliderL + Math.round(sliderW * (value / 255f)) - 3;
        g.fill(knobX, y + 1, knobX + 6, y + 11, 0xFFF0F0F0);
        FeaturesScreen.drawGradientBorder(g, knobX, y + 1, 6, 10, ACCENT, 0xFF303038);
        g.drawString(font, Integer.toString(value), sliderL + sliderW + 6, y + 1, 0xC8C8D0, false);
    }

    private void setChannel(int channel, int value) {
        value = Mth.clamp(value, 0, 255);
        if (channel == 0) {
            r = value;
        } else if (channel == 1) {
            g = value;
        } else {
            b = value;
        }
        syncHex();
    }

    private int hitSlider(double mouseX, double mouseY) {
        for (int i = 0; i < 3; i++) {
            int y = panelTop + 60 + i * 18;
            if (mouseX >= sliderL - 4 && mouseX <= sliderL + sliderW + 4
                    && mouseY >= y && mouseY <= y + 12) {
                return i;
            }
        }
        return -1;
    }

    private void applySliderDrag(double mouseX, int channel) {
        float t = (float) ((mouseX - sliderL) / (double) sliderW);
        setChannel(channel, Math.round(Mth.clamp(t, 0f, 1f) * 255f));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int hit = hitSlider(mouseX, mouseY);
            if (hit >= 0) {
                dragging = hit;
                applySliderDrag(mouseX, hit);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging >= 0 && button == 0) {
            applySliderDrag(mouseX, dragging);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = -1;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // Skip vanilla gradient / dirt - keep the world sharp with a light dim only.
        FeatureSettingsBackdrop.render(graphics, width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private class DarkButton extends AbstractWidget {
        private final Runnable action;

        private DarkButton(int x, int y, int w, int h, Component message, Runnable action) {
            super(x, y, w, h, message);
            this.action = action;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hover = isHoveredOrFocused();
            graphics.fill(getX(), getY(), getX() + width, getY() + height, hover ? 0xFF2A3A38 : 0xFF1A1A22);
            FeaturesScreen.drawGradientBorder(graphics, getX(), getY(), width, height, ACCENT, 0xFF0A0A0E);
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
}
