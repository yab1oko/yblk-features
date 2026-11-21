package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import yblk.features.Features;
import yblk.features.client.ui.BaseSettingsScreen;
import yblk.features.client.ui.TextField;
import yblk.features.client.ui.SectionHeader;
import yblk.features.border.BorderWarningTexts;
import yblk.features.config.FeaturesConfig;

public class BorderWarningTextScreen extends BaseSettingsScreen {
        private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFFE88C40;

    private EditBox approachBox;
    private EditBox dangerBox;

    public BorderWarningTextScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".border_danger.warn_text"), parent);
    }

    @Override
    protected void init() {
        clearWidgets();
        int panelW = Math.min(340, width - 24);
        int left = (width - panelW) / 2;
        int boxW = panelW - 24;
        int y = 48;

        approachBox = new TextField(font, left + 12, y, boxW, 18, Component.empty());
        approachBox.setMaxLength(120);
        approachBox.setValue(BorderWarningTexts.effectiveApproachRaw());
        addRenderableWidget(approachBox);
        y += 40;

        dangerBox = new TextField(font, left + 12, y, boxW, 18, Component.empty());
        dangerBox.setMaxLength(120);
        dangerBox.setValue(BorderWarningTexts.effectiveDangerRaw());
        addRenderableWidget(dangerBox);

        int btnY = height - 28;
        int btnW = 88;
        addRenderableWidget(darkButton(width / 2 - btnW - 48, btnY, btnW, 18,
                Component.translatable("screen." + Features.MODID + ".border_danger.warn_text.reset"), () -> {
                    FeaturesConfig.setBorderWarnTextApproach("");
                    FeaturesConfig.setBorderWarnTextDanger("");
                    approachBox.setValue(BorderWarningTexts.effectiveApproachRaw());
                    dangerBox.setValue(BorderWarningTexts.effectiveDangerRaw());
                }));
        addRenderableWidget(darkButton(width / 2 - btnW / 2, btnY, btnW, 18,
                Component.translatable("gui.done"), this::saveAndClose));
        addRenderableWidget(darkButton(width / 2 + 48, btnY, btnW, 18,
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
    protected int buttonAccent() {
        return ACCENT;
    }

    @Override
    protected int buttonHover() {
        return 0xFF2A3A38;
    }
}
