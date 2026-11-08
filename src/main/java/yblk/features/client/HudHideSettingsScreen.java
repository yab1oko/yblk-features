package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.hud.HudElement;
import yblk.features.hud.HudHide;
import yblk.features.hud.HudHideStyle;

import java.util.ArrayList;
import java.util.List;

public class HudHideSettingsScreen extends Screen {
        private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFFE05050;
    private static final int CHECK = 9;
    private static final int ROW = 15;
    private static final int CHOICE_H = 16;
    private static final int PANEL_W = 340;

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private int scroll;
    private int maxScroll;
    private int panelL;
    private int panelR;
    private int panelTop;
    private int panelBottom;
    private int contentTop;
    private int contentH;
    private boolean canEdit;

    public HudHideSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".hud_hide"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        hits.clear();
        canEdit = HudHide.canEdit(minecraft != null ? minecraft.player : null);

        int btnH = 18;
        int bottomPad = 6;
        panelL = (width - Math.min(PANEL_W, width - 16)) / 2;
        panelR = width - panelL;
        panelTop = 22;
        panelBottom = height - bottomPad - btnH - 6;
        contentTop = panelTop + 8;

        contentH = measureContentHeight();
        maxScroll = Math.max(0, contentH - Math.max(1, panelBottom - contentTop - 4));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        addRenderableWidget(new DarkButton(width / 2 - 45, height - bottomPad - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    private int measureContentHeight() {
        int y = 0;
        if (!canEdit) {
            y += 18;
        }
        y += 12; // hide section
        y += HudElement.values().length * ROW;
        y += 6 + 12 + CHOICE_H + 6; // style
        y += 4 + 12 + 12; // show-on-change section + hint
        for (HudElement element : HudElement.values()) {
            if (element.supportsShowOnChange()) {
                y += ROW;
            }
        }
        if (canSeeAdminLock()) {
            y += 8 + 12 + ROW; // admin
        }
        y += 6;
        return y;
    }

    private boolean canSeeAdminLock() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        contentH = measureContentHeight();
        maxScroll = Math.max(0, contentH - Math.max(1, panelBottom - contentTop - 4));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        this.renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xF0F0F0);

        graphics.fill(panelL, panelTop, panelR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelTop, panelR - panelL,
                panelBottom - panelTop, ACCENT, 0xFF0A0A0E);

        graphics.enableScissor(panelL + 2, panelTop + 2, panelR - 2, panelBottom - 2);

        int x = panelL + 12;
        int right = panelR - 12;
        int y = contentTop - scroll;

        if (!canEdit) {
            graphics.drawString(font,
                    Component.translatable("screen." + Features.MODID + ".hud_hide.locked"),
                    x, y, 0xE05050, false);
            y += 18;
        }

        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".hud_hide.hide");
        for (HudElement element : HudElement.values()) {
            boolean hidden = FeaturesConfig.isHudElementHidden(element);
            y = drawCheckRow(graphics, x, y, mouseX, mouseY, hidden,
                    () -> {
                        if (canEdit) {
                            FeaturesConfig.setHudElementHidden(element, !hidden);
                        }
                    },
                    element.langKey(), canEdit);
        }

        y += 6;
        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".hud_hide.style");
        y = drawStyleRow(graphics, x, right, y, mouseX, mouseY);

        y += 4;
        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".hud_hide.show_on_change");
        String hintKey = FeaturesConfig.hudHideStyle == HudHideStyle.SLIDE
                ? "screen." + Features.MODID + ".hud_hide.show_on_change.hint_slide"
                : "screen." + Features.MODID + ".hud_hide.show_on_change.hint";
        graphics.drawString(font, Component.translatable(hintKey), x, y, 0x808088, false);
        y += 12;
        for (HudElement element : HudElement.values()) {
            if (!element.supportsShowOnChange()) {
                continue;
            }
            boolean on = FeaturesConfig.isHudShowOnChange(element);
            boolean enabledRow = canEdit && FeaturesConfig.isHudElementHidden(element);
            y = drawCheckRow(graphics, x, y, mouseX, mouseY, on,
                    () -> {
                        if (enabledRow) {
                            FeaturesConfig.setHudShowOnChange(element, !on);
                        }
                    },
                    element.showOnChangeLangKey(), enabledRow);
        }

        if (canSeeAdminLock()) {
            y += 8;
            y = drawSection(graphics, x, y, "screen." + Features.MODID + ".hud_hide.admin");
            drawCheckRow(graphics, x, y, mouseX, mouseY, FeaturesConfig.hudHideAdminLocked,
                    () -> {
                        FeaturesConfig.setHudHideAdminLocked(!FeaturesConfig.hudHideAdminLocked);
                        canEdit = HudHide.canEdit(minecraft != null ? minecraft.player : null);
                    },
                    FeaturesConfig.hudHideAdminLocked
                            ? "screen." + Features.MODID + ".hud_hide.admin.on"
                            : "screen." + Features.MODID + ".hud_hide.admin.off",
                    true);
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackH = panelBottom - panelTop - 8;
            int thumbH = Math.max(12, trackH * trackH / (trackH + maxScroll));
            int thumbY = panelTop + 4 + (trackH - thumbH) * scroll / maxScroll;
            graphics.fill(panelR - 5, panelTop + 4, panelR - 3, panelBottom - 4, 0xFF2A2A32);
            graphics.fill(panelR - 5, thumbY, panelR - 3, thumbY + thumbH, ACCENT);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int drawSection(GuiGraphics g, int x, int y, String key) {
        g.drawString(font, Component.translatable(key), x, y, 0xC8C8D0, false);
        return y + 12;
    }

    private int drawStyleRow(GuiGraphics g, int left, int right, int y, int mx, int my) {
        HudHideStyle[] values = HudHideStyle.values();
        int gap = 3;
        int w = Math.max(40, ((right - left) - gap * (values.length - 1)) / values.length);
        int x = left;
        for (HudHideStyle style : values) {
            boolean active = FeaturesConfig.hudHideStyle == style;
            boolean hover = canEdit && insidePanel(mx, my)
                    && mx >= x && mx < x + w && my >= y && my < y + CHOICE_H;
            g.fill(x, y, x + w, y + CHOICE_H, active ? 0xFF3A2020 : (hover ? 0xFF2A2A32 : 0xFF1A1A22));
            if (active) {
                g.fill(x, y + CHOICE_H - 2, x + w, y + CHOICE_H, ACCENT);
            }
            g.drawCenteredString(font, Component.translatable(style.langKey()),
                    x + w / 2, y + (CHOICE_H - 8) / 2, active ? 0xF0F0F0 : 0x9A9AA8);
            if (canEdit) {
                hits.add(new Hit(x, y, w, CHOICE_H, () -> FeaturesConfig.setHudHideStyle(style)));
            }
            x += w + gap;
        }
        return y + CHOICE_H + 6;
    }

    private int drawCheckRow(GuiGraphics g, int x, int y, int mx, int my, boolean on,
                             Runnable action, String labelKey, boolean interactive) {
        int color = interactive ? (on ? 0xF0F0F0 : 0x9A9AA8) : 0x606068;
        drawCheck(g, x, y + 3, on && interactive || on && !interactive);
        if (!interactive) {
            g.fill(x, y + 3, x + CHECK, y + 3 + CHECK, 0x66000000);
        }
        g.drawString(font, Component.translatable(labelKey), x + CHECK + 6, y + 3, color, false);
        if (interactive) {
            hits.add(new Hit(x, y, panelR - 12 - x, ROW - 2, action));
        }
        return y + ROW;
    }

    private static void drawCheck(GuiGraphics g, int x, int y, boolean on) {
        g.fill(x, y, x + CHECK, y + 1, 0xFFE8E8F0);
        g.fill(x, y + CHECK - 1, x + CHECK, y + CHECK, 0xFFE8E8F0);
        g.fill(x, y, x + 1, y + CHECK, 0xFFE8E8F0);
        g.fill(x + CHECK - 1, y, x + CHECK, y + CHECK, 0xFFE8E8F0);
        if (on) {
            g.fill(x + 2, y + 2, x + CHECK - 2, y + CHECK - 2, ACCENT);
        }
    }

    private boolean insidePanel(double mx, double my) {
        return mx >= panelL && mx < panelR && my >= panelTop && my < panelBottom;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && insidePanel(mouseX, mouseY)) {
            for (Hit hit : hits) {
                if (mouseX >= hit.x && mouseX < hit.x + hit.w && mouseY >= hit.y && mouseY < hit.y + hit.h) {
                    hit.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!insidePanel(mouseX, mouseY) && maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        scroll = Mth.clamp(scroll - (int) Math.signum(delta) * 16, 0, maxScroll);
        return true;
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

    private record Hit(int x, int y, int w, int h, Runnable action) {
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
