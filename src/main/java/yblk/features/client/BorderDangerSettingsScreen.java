package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import yblk.features.Features;
import yblk.features.border.BorderBarrierVisual;
import yblk.features.border.BorderEffectOptions;
import yblk.features.border.BorderHarmMode;
import yblk.features.border.BorderWarningStyle;
import yblk.features.border.BorderZoneFx;
import yblk.features.config.FeaturesConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class BorderDangerSettingsScreen extends Screen {
        private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFFE88C40;
    private static final int CHECK = 9;
    private static final int ROW = 16;
    private static final int CHOICE_H = 16;
    private static final int SECTION_GAP = 3;
    private static final int PANEL_W = 360;
    private static final int DROP_H = 52;

    private final Screen parent;
    private EditBox dangerBox;
    private EditBox visualBox;
    private EditBox approachBox;
    private int scroll;
    private int maxScroll;
    private int panelL;
    private int panelR;
    private int panelTop;
    private int panelBottom;
    private int contentTop;
    private int contentH;
    private int dropL;
    private int dropT;
    private int dropR;
    private int dropB;
    private final List<Hit> hits = new ArrayList<>();

    public BorderDangerSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".border_danger"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        hits.clear();

        CustomBarrierTexture.hookFileDrop();

        boolean customBarrier = FeaturesConfig.borderBarrierVisual == BorderBarrierVisual.CUSTOM;
        int titleH = 22;
        int btnH = 18;
        int bottomPad = 6;
        panelL = (width - Math.min(PANEL_W, width - 16)) / 2;
        panelR = width - panelL;
        panelTop = titleH;
        int footerTop = customBarrier
                ? height - bottomPad - btnH - 4 - DROP_H
                : height - bottomPad - btnH - 2;
        panelBottom = footerTop - 4;
        contentTop = panelTop + 6;

        dropL = panelL;
        dropR = panelR;
        dropT = footerTop;
        dropB = footerTop + DROP_H;

        int boxW = 44;
        int boxX = panelR - 12 - boxW;
        dangerBox = numberBox(boxX, 0, FeaturesConfig.borderDangerDistance, FeaturesConfig::setBorderDangerDistance);
        visualBox = numberBox(boxX, 0, FeaturesConfig.borderVisualDistance, FeaturesConfig::setBorderVisualDistance);
        approachBox = numberBox(boxX, 0, FeaturesConfig.borderApproachDistance, FeaturesConfig::setBorderApproachDistance);
        addRenderableWidget(dangerBox);
        addRenderableWidget(visualBox);
        addRenderableWidget(approachBox);

        contentH = measureContentHeight();
        maxScroll = Math.max(0, contentH - Math.max(1, panelBottom - contentTop - 4));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        syncBoxes();

        int btnY = height - bottomPad - btnH;
        if (customBarrier) {
            int btnW = 72;
            int gap = 4;
            int rowW = btnW * 3 + gap * 2;
            int rowX = width / 2 - rowW / 2;
            addRenderableWidget(new DarkButton(rowX, btnY, btnW, btnH,
                    Component.translatable("screen." + Features.MODID + ".border_danger.barrier.folder"),
                    CustomBarrierTexture::openStorageFolder));
            addRenderableWidget(new DarkButton(rowX + btnW + gap, btnY, btnW, btnH,
                    Component.translatable("screen." + Features.MODID + ".border_danger.barrier.clear"),
                    () -> {
                        CustomBarrierTexture.clear();
                        rebuildKeepingScroll();
                    }));
            addRenderableWidget(new DarkButton(rowX + (btnW + gap) * 2, btnY, btnW, btnH,
                    Component.translatable("gui.done"), this::onClose));
        } else {
            int btnW = 90;
            addRenderableWidget(new DarkButton(width / 2 - btnW / 2, btnY, btnW, btnH,
                    Component.translatable("gui.done"), this::onClose));
        }
    }

    private void rebuildKeepingScroll() {
        int saved = scroll;
        init();
        scroll = Mth.clamp(saved, 0, maxScroll);
    }

    private EditBox numberBox(int x, int y, int value, IntConsumer setter) {
        EditBox box = new EditBox(font, x, y, 44, 14, Component.empty());
        box.setValue(Integer.toString(value));
        box.setMaxLength(4);
        box.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        box.setResponder(raw -> {
            if (raw == null || raw.isEmpty()) {
                return;
            }
            try {
                setter.accept(Integer.parseInt(raw));
            } catch (NumberFormatException ignored) {
            }
        });
        return box;
    }

    private int measureContentHeight() {
        int y = 0;
        y += ROW * 3 + 4; // distances
        y += 12 + CHOICE_H + SECTION_GAP; // harm
        if (FeaturesConfig.borderHarmMode == BorderHarmMode.EFFECTS) {
            int rows = (BorderEffectOptions.ALL.size() + 1) / 2;
            y += 12 + rows * ROW + SECTION_GAP;
        }
        y += 12 + ROW; // warning checkbox
        y += 12 + CHOICE_H + SECTION_GAP; // style
        if (FeaturesConfig.borderShowWarning) {
            y += 12 + ROW * 2 + SECTION_GAP; // colors
            y += CHOICE_H + 4; // edit texts button
        }
        y += 12 + CHOICE_H + SECTION_GAP; // barrier modes
        y += ROW + 2; // hide vanilla red tint
        int fxRows = (BorderZoneFx.values().length + 1) / 2;
        y += 12 + fxRows * ROW + 4; // fx
        return y;
    }

    private void syncBoxes() {
        int y0 = contentTop - scroll;
        placeBox(dangerBox, y0);
        placeBox(visualBox, y0 + ROW);
        placeBox(approachBox, y0 + ROW * 2);
    }

    private void placeBox(EditBox box, int y) {
        box.setX(panelR - 12 - box.getWidth());
        box.setY(y);
        boolean visible = y + box.getHeight() > panelTop + 2 && y < panelBottom - 2;
        box.visible = visible;
        box.active = visible;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        contentH = measureContentHeight();
        maxScroll = Math.max(0, contentH - Math.max(1, panelBottom - contentTop - 4));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        syncBoxes();

        this.renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xF0F0F0);

        graphics.fill(panelL, panelTop, panelR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelTop, panelR - panelL,
                panelBottom - panelTop, ACCENT, 0xFF0A0A0E);

        graphics.enableScissor(panelL + 2, panelTop + 2, panelR - 2, panelBottom - 2);

        int left = panelL + 10;
        int right = panelR - 10;
        int y = contentTop - scroll;

        y = drawDistanceRow(graphics, left, y, "screen." + Features.MODID + ".border_danger.distance.harm");
        y = drawDistanceRow(graphics, left, y, "screen." + Features.MODID + ".border_danger.distance.visual");
        y = drawDistanceRow(graphics, left, y, "screen." + Features.MODID + ".border_danger.distance.approach");
        y += 2;

        y = drawSectionLabel(graphics, left, y, "screen." + Features.MODID + ".border_danger.harm");
        y = drawModeRow(graphics, left, right, y, mouseX, mouseY);

        if (FeaturesConfig.borderHarmMode == BorderHarmMode.EFFECTS) {
            y = drawSectionLabel(graphics, left, y + 1, "screen." + Features.MODID + ".border_danger.effects");
            y = drawEffectToggles(graphics, left, right, y, mouseX, mouseY);
        }

        y = drawSectionLabel(graphics, left, y + SECTION_GAP, "screen." + Features.MODID + ".border_danger.warning");
        y = drawToggleRow(graphics, left, y, mouseX, mouseY,
                FeaturesConfig.borderShowWarning,
                () -> FeaturesConfig.setBorderShowWarning(!FeaturesConfig.borderShowWarning),
                FeaturesConfig.borderShowWarning
                        ? "screen." + Features.MODID + ".border_danger.warning.on"
                        : "screen." + Features.MODID + ".border_danger.warning.off");

        y = drawSectionLabel(graphics, left, y + 1, "screen." + Features.MODID + ".border_danger.warn_style");
        y = drawEnumRow(graphics, left, right, y, mouseX, mouseY,
                BorderWarningStyle.values(), FeaturesConfig.borderWarningStyle,
                FeaturesConfig::setBorderWarningStyle, BorderWarningStyle::langKey);

        if (FeaturesConfig.borderShowWarning) {
            y = drawSectionLabel(graphics, left, y + 1, "screen." + Features.MODID + ".border_danger.warn_colors");
            y = drawColorRow(graphics, left, y, mouseX, mouseY,
                    "screen." + Features.MODID + ".border_danger.warn_colors.approach",
                    FeaturesConfig.borderWarnColorApproachA, FeaturesConfig.borderWarnColorApproachB,
                    FeaturesConfig::setBorderWarnColorApproachA, FeaturesConfig::setBorderWarnColorApproachB);
            y = drawColorRow(graphics, left, y, mouseX, mouseY,
                    "screen." + Features.MODID + ".border_danger.warn_colors.danger",
                    FeaturesConfig.borderWarnColorDangerA, FeaturesConfig.borderWarnColorDangerB,
                    FeaturesConfig::setBorderWarnColorDangerA, FeaturesConfig::setBorderWarnColorDangerB);
            y = drawActionButton(graphics, left, right, y, mouseX, mouseY,
                    "screen." + Features.MODID + ".border_danger.warn_text.button",
                    () -> {
                        if (minecraft != null) {
                            minecraft.setScreen(new BorderWarningTextScreen(this));
                        }
                    });
        }

        y = drawSectionLabel(graphics, left, y + SECTION_GAP, "screen." + Features.MODID + ".border_danger.barrier");
        y = drawEnumRow(graphics, left, right, y, mouseX, mouseY,
                BorderBarrierVisual.values(), FeaturesConfig.borderBarrierVisual,
                visual -> {
                    FeaturesConfig.setBorderBarrierVisual(visual);
                    if (visual == BorderBarrierVisual.CUSTOM && CustomBarrierTexture.hasFile()) {
                        CustomBarrierTexture.reload();
                    }
                    rebuildKeepingScroll();
                }, BorderBarrierVisual::langKey);
        y = drawToggleRow(graphics, left, y, mouseX, mouseY,
                FeaturesConfig.borderHideVanillaRedTint,
                () -> FeaturesConfig.setBorderHideVanillaRedTint(!FeaturesConfig.borderHideVanillaRedTint),
                FeaturesConfig.borderHideVanillaRedTint
                        ? "screen." + Features.MODID + ".border_danger.vanilla_red.off"
                        : "screen." + Features.MODID + ".border_danger.vanilla_red.on");

        y = drawSectionLabel(graphics, left, y + SECTION_GAP, "screen." + Features.MODID + ".border_danger.fx");
        drawFxToggles(graphics, left, right, y, mouseX, mouseY);

        // EditBoxes must draw under the same scissor or they float above the panel when scrolled.
        renderDistanceBoxes(graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackH = panelBottom - panelTop - 8;
            int thumbH = Math.max(12, trackH * trackH / (trackH + maxScroll));
            int thumbY = panelTop + 4 + (trackH - thumbH) * scroll / maxScroll;
            graphics.fill(panelR - 5, panelTop + 4, panelR - 3, panelBottom - 4, 0xFF2A2A32);
            graphics.fill(panelR - 5, thumbY, panelR - 3, thumbY + thumbH, ACCENT);
        }

        if (FeaturesConfig.borderBarrierVisual == BorderBarrierVisual.CUSTOM) {
            drawDropZone(graphics, mouseX, mouseY);
        }

        // Footer buttons only — distance EditBoxes already drawn inside the panel scissor.
        for (var child : children()) {
            if (child instanceof EditBox) {
                continue;
            }
            if (child instanceof AbstractWidget widget) {
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private void renderDistanceBoxes(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (dangerBox.visible) {
            dangerBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (visualBox.visible) {
            visualBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (approachBox.visible) {
            approachBox.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawDropZone(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean hover = mouseX >= dropL && mouseX < dropR && mouseY >= dropT && mouseY < dropB;
        graphics.fill(dropL, dropT, dropR, dropB, hover ? 0xFF1E2430 : 0xFF14181F);
        FeaturesScreen.drawGradientBorder(graphics, dropL, dropT, dropR - dropL, dropB - dropT,
                hover ? 0xFF7EB6D9 : ACCENT, 0xFF0A0A0E);

        int preview = 40;
        int px = dropL + 8;
        int py = dropT + (DROP_H - preview) / 2;
        graphics.fill(px, py, px + preview, py + preview, 0xFF0A0A0E);
        if (CustomBarrierTexture.isReady()) {
            graphics.blit(CustomBarrierTexture.ID, px + 2, py + 2, preview - 4, preview - 4,
                    0f, 0f, 64, 64, 64, 64);
        } else {
            graphics.drawCenteredString(font, "?", px + preview / 2, py + preview / 2 - 4, 0x606068);
        }
        FeaturesScreen.drawGradientBorder(graphics, px, py, preview, preview, 0xFFE0E0E0, 0xFF404040);

        int textX = px + preview + 10;
        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".border_danger.barrier.drop_title"),
                textX, dropT + 10, 0xF0F0F0, false);
        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".border_danger.barrier.drop_hint"),
                textX, dropT + 24, 0x9A9AA8, false);
        graphics.drawString(font,
                Component.literal(CustomBarrierTexture.storageDir().toAbsolutePath().toString()),
                textX, dropT + 36, 0x606068, false);

        addHit(dropL, dropT, dropR - dropL, DROP_H, mouseX, mouseY, CustomBarrierTexture::openStorageFolder);
    }

    private int drawDistanceRow(GuiGraphics g, int x, int y, String key) {
        g.drawString(font, Component.translatable(key), x, y + 3, 0xC8C8D0, false);
        return y + ROW;
    }

    private int drawActionButton(GuiGraphics g, int left, int right, int y, int mx, int my,
                                 String labelKey, Runnable action) {
        int w = Math.min(200, right - left);
        drawChoice(g, left, y, w, CHOICE_H, Component.translatable(labelKey), false, mx, my);
        addHit(left, y, w, CHOICE_H, mx, my, action);
        return y + CHOICE_H + 4;
    }

    private int drawToggleRow(GuiGraphics g, int x, int y, int mx, int my, boolean on, Runnable toggle, String labelKey) {
        drawCheck(g, x, y + 3, on);
        g.drawString(font, Component.translatable(labelKey), x + CHECK + 5, y + 3, 0xE0E0E8, false);
        addHit(x, y, Math.max(40, panelR - 10 - x), ROW - 2, mx, my, toggle);
        return y + ROW;
    }

    private int drawColorRow(GuiGraphics g, int x, int y, int mx, int my, String labelKey,
                             int colorA, int colorB, IntConsumer setA, IntConsumer setB) {
        g.drawString(font, Component.translatable(labelKey), x, y + 3, 0xA0A0AA, false);
        int swatchX = Math.min(x + 120, panelR - 70);
        drawSwatch(g, swatchX, y + 1, colorA, mx, my,
                Component.translatable(labelKey).getString() + " A", setA);
        drawSwatch(g, swatchX + 24, y + 1, colorB, mx, my,
                Component.translatable(labelKey).getString() + " B", setB);
        return y + ROW;
    }

    private void drawSwatch(GuiGraphics g, int x, int y, int color, int mx, int my,
                            String title, IntConsumer setter) {
        g.fill(x, y, x + 18, y + 12, 0xFF000000 | color);
        FeaturesScreen.drawGradientBorder(g, x, y, 18, 12, 0xFFE0E0E0, 0xFF404040);
        addHit(x, y, 18, 12, mx, my, () -> {
            if (minecraft != null) {
                minecraft.setScreen(new ColorPickerScreen(this, Component.literal(title), color, setter));
            }
        });
    }

    private int drawSectionLabel(GuiGraphics graphics, int x, int y, String key) {
        graphics.drawString(font, Component.translatable(key), x, y, 0xC8C8D0, false);
        return y + 11;
    }

    private int drawModeRow(GuiGraphics g, int left, int right, int y, int mx, int my) {
        int gap = 4;
        int w = ((right - left) - gap) / 2;
        int x = left;
        for (BorderHarmMode mode : BorderHarmMode.values()) {
            boolean active = FeaturesConfig.borderHarmMode == mode;
            drawChoice(g, x, y, w, CHOICE_H, Component.translatable(mode.langKey()), active, mx, my);
            int fx = x;
            addHit(fx, y, w, CHOICE_H, mx, my, () -> FeaturesConfig.setBorderHarmMode(mode));
            x += w + gap;
        }
        return y + CHOICE_H + 4;
    }

    private int drawEffectToggles(GuiGraphics g, int left, int right, int y, int mx, int my) {
        int colW = (right - left) / 2;
        int i = 0;
        int rowY = y;
        for (String key : BorderEffectOptions.ALL.keySet()) {
            int col = i % 2;
            int row = i / 2;
            int x = left + col * colW;
            int yy = y + row * ROW;
            rowY = Math.max(rowY, yy + ROW);
            boolean on = FeaturesConfig.borderEffects.contains(key);
            drawCheck(g, x, yy + 3, on);
            g.drawString(font, Component.translatable("screen." + Features.MODID + ".border_danger.effect." + key),
                    x + CHECK + 5, yy + 3, on ? 0xF0F0F0 : 0x9A9AA8, false);
            addHit(x, yy, colW - 4, ROW - 2, mx, my, () -> FeaturesConfig.toggleBorderEffect(key));
            i++;
        }
        return rowY + 2;
    }

    private int drawFxToggles(GuiGraphics g, int left, int right, int y, int mx, int my) {
        int colW = (right - left) / 2;
        int i = 0;
        int rowY = y;
        for (BorderZoneFx fx : BorderZoneFx.values()) {
            int col = i % 2;
            int row = i / 2;
            int x = left + col * colW;
            int yy = y + row * ROW;
            rowY = Math.max(rowY, yy + ROW);
            boolean on = FeaturesConfig.borderZoneFx.contains(fx.configKey());
            drawCheck(g, x, yy + 3, on);
            g.drawString(font, Component.translatable(fx.langKey()),
                    x + CHECK + 5, yy + 3, on ? 0xF0F0F0 : 0x9A9AA8, false);
            addHit(x, yy, colW - 4, ROW - 2, mx, my, () -> FeaturesConfig.toggleBorderZoneFx(fx.configKey()));
            i++;
        }
        return rowY + 2;
    }

    private <T extends Enum<T>> int drawEnumRow(GuiGraphics g, int left, int right, int y, int mx, int my,
                                                T[] values, T selected, java.util.function.Consumer<T> setter,
                                                java.util.function.Function<T, String> lang) {
        int gap = 3;
        int count = values.length;
        int w = Math.max(40, ((right - left) - gap * (count - 1)) / count);
        int x = left;
        for (T value : values) {
            boolean active = selected == value;
            drawChoice(g, x, y, w, CHOICE_H, Component.translatable(lang.apply(value)), active, mx, my);
            int fx = x;
            addHit(fx, y, w, CHOICE_H, mx, my, () -> setter.accept(value));
            x += w + gap;
        }
        return y + CHOICE_H + 4;
    }

    private void drawChoice(GuiGraphics g, int x, int y, int w, int h, Component label, boolean active, int mx, int my) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h && insidePanel(mx, my);
        g.fill(x, y, x + w, y + h, active ? 0xFF3A2A1C : (hover ? 0xFF2A2A32 : 0xFF1A1A22));
        if (active) {
            g.fill(x, y + h - 2, x + w, y + h, ACCENT);
        }
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, active ? 0xF0F0F0 : 0x9A9AA8);
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

    private void addHit(int x, int y, int w, int h, int mx, int my, Runnable action) {
        // mx/my unused here — stored rects are tested against the actual click later
        hits.add(new Hit(x, y, w, h, action));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && insidePanel(mouseX, mouseY)) {
            boolean hit = false;
            for (Hit h : new ArrayList<>(hits)) {
                if (mouseX >= h.x && mouseX < h.x + h.w && mouseY >= h.y && mouseY < h.y + h.h) {
                    h.action.run();
                    hit = true;
                    break;
                }
            }
            if (hit) {
                maxScroll = Math.max(0, measureContentHeight() - Math.max(1, panelBottom - contentTop - 4));
                scroll = Mth.clamp(scroll, 0, maxScroll);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!insidePanel(mouseX, mouseY) && maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        scroll = Mth.clamp(scroll - (int) Math.signum(delta) * 14, 0, maxScroll);
        syncBoxes();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (FeaturesConfig.borderBarrierVisual == BorderBarrierVisual.CUSTOM
                && Screen.hasControlDown() && keyCode == 86 /* V */) {
            CustomBarrierTexture.pasteFromClipboard();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (FeaturesConfig.borderBarrierVisual == BorderBarrierVisual.CUSTOM) {
            CustomBarrierTexture.pollExternalChanges();
        }
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
