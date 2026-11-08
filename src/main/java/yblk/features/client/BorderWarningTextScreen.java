package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import yblk.features.Features;
import yblk.features.border.BorderWarningTexts;
import yblk.features.config.FeaturesConfig;

public class BorderWarningTextScreen extends Screen {
        private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFFE88C40;

    private final Screen parent;
    private EditBox approachBox;
    private EditBox dangerBox;

    public BorderWarningTextScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".border_danger.warn_text"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        int panelW = Math.min(340, width - 24);
        int left = (width - panelW) / 2;
        int boxW = panelW - 24;
        int y = 48;

        approachBox = new EditBox(font, left + 12, y, boxW, 18, Component.empty());
        approachBox.setMaxLength(120);
        approachBox.setValue(BorderWarningTexts.effectiveApproachRaw());
        addRenderableWidget(approachBox);
        y += 40;

        dangerBox = new EditBox(font, left + 12, y, boxW, 18, Component.empty());
        dangerBox.setMaxLength(120);
        dangerBox.setValue(BorderWarningTexts.effectiveDangerRaw());
        addRenderableWidget(dangerBox);

        int btnY = height - 28;
        int btnW = 88;
        addRenderableWidget(new DarkButton(width / 2 - btnW - 48, btnY, btnW, 18,
                Component.translatable("screen." + Features.MODID + ".border_danger.warn_text.reset"), () -> {
                    FeaturesConfig.setBorderWarnTextApproach("");
                    FeaturesConfig.setBorderWarnTextDanger("");
                    approachBox.setValue(BorderWarningTexts.effectiveApproachRaw());
                    dangerBox.setValue(BorderWarningTexts.effectiveDangerRaw());
                }));
        addRenderableWidget(new DarkButton(width / 2 - btnW / 2, btnY, btnW, 18,
                Component.translatable("gui.done"), this::saveAndClose));
        addRenderableWidget(new DarkButton(width / 2 + 48, btnY, btnW, 18,
                Component.translatable("gui.cancel"), this::onClose));
    }

    private void saveAndClose() {
        String approach = approachBox.getValue() == null ? "" : approachBox.getValue().trim();
        String danger = dangerBox.getValue() == null ? "" : dangerBox.getValue().trim();
        String defApproach = Component.translatable(BorderWarningTexts.defaultApproachKey()).getString();
        String defDanger = Component.translatable(BorderWarningTexts.defaultDangerKey()).getString();
        FeaturesConfig.setBorderWarnTextApproach(approach.equals(defApproach) ? "" : approach);
        FeaturesConfig.setBorderWarnTextDanger(danger.equals(defDanger) ? "" : danger);
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xF0F0F0);

        int panelW = Math.min(340, width - 24);
        int left = (width - panelW) / 2;
        int top = 32;
        int bottom = height - 40;
        graphics.fill(left, top, left + panelW, bottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, left, top, panelW, bottom - top, ACCENT, 0xFF0A0A0E);

        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".border_danger.warn_text.approach"),
                left + 12, 38, 0xC8C8D0, false);
        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".border_danger.warn_text.danger"),
                left + 12, 78, 0xC8C8D0, false);
        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".border_danger.warn_text.hint"),
                left + 12, 118, 0x808088, false);

        super.render(graphics, mouseX, mouseY, partialTick);
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
