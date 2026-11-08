package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.ModList;
import yblk.features.Features;
import yblk.features.compat.BetterCombatRfpCompat;
import yblk.features.compat.PlayerAnimatorRfpCompat;
import yblk.features.compat.TaczRfpCompat;
import yblk.features.config.FeaturesConfig;
import yblk.features.rfp.RfpHandMode;
import yblk.features.rfp.RfpPoseOverride;
import yblk.features.rfp.RfpTunePose;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Settings on the right; left side stays clear so the real first-person view is visible.
 * Pose chips force live animations on the player for offset tuning.
 */
public class RealFirstPersonSettingsScreen extends Screen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFF4AA8C8;
    private static final int CHECK = 9;
    private static final int ROW = 15;
    private static final int CHOICE_H = 18;
    private static final int SLIDER_H = 14;
    private static final int SETTINGS_W = 300;
    private static final int ACTION_RESET_H = 16;
    private static final int TUNE_H = 16;

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private int scroll;
    private int maxScroll;
    private int settingsL;
    private int settingsR;
    private int panelTop;
    private int panelBottom;
    private int contentTop;
    private int contentH;
    private SliderDrag dragging;
    private RfpTunePose tunePose = RfpTunePose.STAND;

    public RealFirstPersonSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".real_first_person"));
        this.parent = parent;
    }

    public RfpTunePose getTunePose() {
        return tunePose;
    }

    @Override
    protected void init() {
        clearWidgets();
        hits.clear();
        dragging = null;

        int btnH = 18;
        int bottomPad = 6;
        int sidePad = 8;
        int usable = width - sidePad * 2;
        int settingsW = Math.min(SETTINGS_W, Math.max(220, usable * 1 / 2));
        settingsR = width - sidePad;
        settingsL = settingsR - settingsW;
        panelTop = 22;
        panelBottom = height - bottomPad - btnH - 6;
        contentTop = panelTop + 8;

        contentH = measureContentHeight();
        maxScroll = Math.max(0, contentH - Math.max(1, panelBottom - contentTop - 4));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        int doneX = settingsL + (settingsW - 90) / 2;
        addRenderableWidget(new DarkButton(doneX, height - bottomPad - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
        RfpPoseOverride.set(tunePose);
    }

    private int measureContentHeight() {
        int textW = Math.max(40, (settingsR - settingsL) - 24);
        int y = 0;
        y += wrappedHeight("screen." + Features.MODID + ".real_first_person.hint", textW);
        y += 12 + CHOICE_H + 6;
        y += wrappedHeight(FeaturesConfig.rfpHandMode.langKey() + ".desc", textW);
        y += ROW; // hide head
        y += ROW; // stuck
        y += 8 + 12 + TUNE_H + 6 + ACTION_RESET_H + 6; // offsets + tune chips + reset
        y += (12 + SLIDER_H + 4) * 5;
        y += 8 + 12; // compat
        y += wrappedHeight(taczStatusKey(), textW);
        y += wrappedHeight(playerAnimatorStatusKey(), textW);
        if (BetterCombatRfpCompat.isLoaded()) {
            y += wrappedHeight("screen." + Features.MODID + ".real_first_person.compat.bc.on", textW);
        }
        if (ModList.get().isLoaded("firstperson")) {
            y += wrappedHeight("screen." + Features.MODID + ".real_first_person.compat.firstperson", textW);
        }
        y += 8;
        return y;
    }

    private int wrappedHeight(String key, int maxWidth) {
        int lines = Math.max(1, font.split(Component.translatable(key), maxWidth).size());
        return lines * 10 + 2; // matches drawWrapped
    }

    private String taczStatusKey() {
        return TaczRfpCompat.isLoaded()
                ? "screen." + Features.MODID + ".real_first_person.compat.tacz.on"
                : "screen." + Features.MODID + ".real_first_person.compat.tacz.off";
    }

    private String playerAnimatorStatusKey() {
        return PlayerAnimatorRfpCompat.isLoaded()
                ? "screen." + Features.MODID + ".real_first_person.compat.pa.on"
                : "screen." + Features.MODID + ".real_first_person.compat.pa.off";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        contentH = measureContentHeight();
        maxScroll = Math.max(0, contentH - Math.max(1, panelBottom - contentTop - 4));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        // No full-screen dim — left side stays as live first-person view.
        this.renderBackground(graphics);
        graphics.drawString(font, title, settingsL, 6, 0xF0F0F0, false);

        graphics.fill(settingsL, panelTop, settingsR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, settingsL, panelTop, settingsR - settingsL,
                panelBottom - panelTop, ACCENT, 0xFF0A0A0E);

        graphics.enableScissor(settingsL + 2, panelTop + 2, settingsR - 2, panelBottom - 2);

        int x = settingsL + 12;
        int right = settingsR - 12;
        int y = contentTop - scroll;

        y = drawWrapped(graphics, Component.translatable("screen." + Features.MODID + ".real_first_person.hint"),
                x, y, right - x, 0x808088);

        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".real_first_person.hands");
        y = drawHandModeRow(graphics, x, right, y, mouseX, mouseY);
        y = drawWrapped(graphics, Component.translatable(FeaturesConfig.rfpHandMode.langKey() + ".desc"),
                x, y, right - x, 0x808088);

        y = drawCheckRow(graphics, x, y, mouseX, mouseY, FeaturesConfig.rfpHideHead,
                () -> FeaturesConfig.setRfpHideHead(!FeaturesConfig.rfpHideHead),
                FeaturesConfig.rfpHideHead
                        ? "screen." + Features.MODID + ".real_first_person.hide_head.on"
                        : "screen." + Features.MODID + ".real_first_person.hide_head.off");

        y = drawCheckRow(graphics, x, y, mouseX, mouseY, FeaturesConfig.rfpRenderStuck,
                () -> FeaturesConfig.setRfpRenderStuck(!FeaturesConfig.rfpRenderStuck),
                FeaturesConfig.rfpRenderStuck
                        ? "screen." + Features.MODID + ".real_first_person.stuck.on"
                        : "screen." + Features.MODID + ".real_first_person.stuck.off");

        y += 8;
        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".real_first_person.offsets");
        y = drawTunePoseRow(graphics, x, right, y, mouseX, mouseY);
        y = drawResetBtn(graphics, x, right, y, mouseX, mouseY);

        y = drawOffsetSlider(graphics, x, right, y, mouseX, mouseY,
                "screen." + Features.MODID + ".real_first_person.offset.stand",
                () -> FeaturesConfig.rfpOffsetStand, FeaturesConfig::setRfpOffsetStand, RfpTunePose.STAND);
        y = drawOffsetSlider(graphics, x, right, y, mouseX, mouseY,
                "screen." + Features.MODID + ".real_first_person.offset.sneak",
                () -> FeaturesConfig.rfpOffsetSneak, FeaturesConfig::setRfpOffsetSneak, RfpTunePose.SNEAK);
        y = drawOffsetSlider(graphics, x, right, y, mouseX, mouseY,
                "screen." + Features.MODID + ".real_first_person.offset.sit",
                () -> FeaturesConfig.rfpOffsetSit, FeaturesConfig::setRfpOffsetSit, RfpTunePose.SIT);
        y = drawOffsetSlider(graphics, x, right, y, mouseX, mouseY,
                "screen." + Features.MODID + ".real_first_person.offset.swim",
                () -> FeaturesConfig.rfpOffsetSwim, FeaturesConfig::setRfpOffsetSwim, RfpTunePose.SWIM);
        y = drawOffsetSlider(graphics, x, right, y, mouseX, mouseY,
                "screen." + Features.MODID + ".real_first_person.offset.elytra",
                () -> FeaturesConfig.rfpOffsetElytra, FeaturesConfig::setRfpOffsetElytra, RfpTunePose.ELYTRA);

        y += 8;
        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".real_first_person.compat");
        y = drawWrapped(graphics, Component.translatable(taczStatusKey()), x, y, right - x, 0xA0A0A8);
        y = drawWrapped(graphics, Component.translatable(playerAnimatorStatusKey()), x, y, right - x, 0xA0A0A8);
        if (BetterCombatRfpCompat.isLoaded()) {
            y = drawWrapped(graphics,
                    Component.translatable("screen." + Features.MODID + ".real_first_person.compat.bc.on"),
                    x, y, right - x, 0xA0A0A8);
        }
        if (ModList.get().isLoaded("firstperson")) {
            y = drawWrapped(graphics,
                    Component.translatable("screen." + Features.MODID + ".real_first_person.compat.firstperson"),
                    x, y, right - x, 0xE09050);
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackH = panelBottom - panelTop - 8;
            int thumbH = Math.max(12, trackH * trackH / (trackH + maxScroll));
            int thumbY = panelTop + 4 + (trackH - thumbH) * scroll / maxScroll;
            graphics.fill(settingsR - 5, panelTop + 4, settingsR - 3, panelBottom - 4, 0xFF2A2A32);
            graphics.fill(settingsR - 5, thumbY, settingsR - 3, thumbY + thumbH, ACCENT);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int drawTunePoseRow(GuiGraphics g, int left, int right, int y, int mx, int my) {
        RfpTunePose[] values = RfpTunePose.values();
        int gap = 3;
        int w = Math.max(36, ((right - left) - gap * (values.length - 1)) / values.length);
        int x = left;
        for (RfpTunePose pose : values) {
            boolean active = tunePose == pose;
            boolean hover = insideSettings(mx, my)
                    && mx >= x && mx < x + w && my >= y && my < y + TUNE_H;
            g.fill(x, y, x + w, y + TUNE_H, active ? 0xFF2A3A48 : (hover ? 0xFF2A2A32 : 0xFF1A1A22));
            if (active) {
                g.fill(x, y + TUNE_H - 2, x + w, y + TUNE_H, ACCENT);
            }
            g.drawCenteredString(font, Component.translatable(pose.langKey()),
                    x + w / 2, y + (TUNE_H - 8) / 2, active ? 0xF0F0F0 : 0x9A9AA8);
            final RfpTunePose select = pose;
            hits.add(new Hit(x, y, w, TUNE_H, () -> {
                tunePose = select;
                RfpPoseOverride.set(select);
            }));
            x += w + gap;
        }
        return y + TUNE_H + 6;
    }

    private int drawResetBtn(GuiGraphics g, int left, int right, int y, int mx, int my) {
        int w = Math.min(80, right - left);
        int h = ACTION_RESET_H;
        boolean hover = insideSettings(mx, my) && mx >= left && mx < left + w && my >= y && my < y + h;
        g.fill(left, y, left + w, y + h, hover ? 0xFF3A2A2A : 0xFF1A1A22);
        if (hover) {
            g.fill(left, y + h - 2, left + w, y + h, 0xFFE07070);
        }
        g.drawCenteredString(font,
                Component.translatable("screen." + Features.MODID + ".real_first_person.offsets.reset"),
                left + w / 2, y + (h - 8) / 2, hover ? 0xF0F0F0 : 0xC07070);
        hits.add(new Hit(left, y, w, h, FeaturesConfig::resetRfpOffsets));
        return y + h + 6;
    }

    private int drawWrapped(GuiGraphics g, Component text, int x, int y, int maxWidth, int color) {
        for (FormattedCharSequence line : font.split(text, Math.max(8, maxWidth))) {
            g.drawString(font, line, x, y, color, false);
            y += 10;
        }
        return y + 2;
    }

    private int drawHandModeRow(GuiGraphics g, int left, int right, int y, int mx, int my) {
        RfpHandMode[] values = RfpHandMode.values();
        int gap = 3;
        int w = Math.max(40, ((right - left) - gap * (values.length - 1)) / values.length);
        int x = left;
        for (RfpHandMode mode : values) {
            boolean active = FeaturesConfig.rfpHandMode == mode;
            boolean hover = insideSettings(mx, my)
                    && mx >= x && mx < x + w && my >= y && my < y + CHOICE_H;
            g.fill(x, y, x + w, y + CHOICE_H, active ? 0xFF2A3A48 : (hover ? 0xFF2A2A32 : 0xFF1A1A22));
            if (active) {
                g.fill(x, y + CHOICE_H - 2, x + w, y + CHOICE_H, ACCENT);
            }
            g.drawCenteredString(font, Component.translatable(mode.langKey()),
                    x + w / 2, y + (CHOICE_H - 8) / 2, active ? 0xF0F0F0 : 0x9A9AA8);
            hits.add(new Hit(x, y, w, CHOICE_H, () -> FeaturesConfig.setRfpHandMode(mode)));
            x += w + gap;
        }
        return y + CHOICE_H + 6;
    }

    private int drawOffsetSlider(GuiGraphics g, int left, int right, int y, int mx, int my,
                                 String labelKey, Supplier<Float> getter, Consumer<Float> setter,
                                 RfpTunePose pose) {
        boolean selected = tunePose == pose;
        g.drawString(font, Component.translatable(labelKey), left, y, selected ? ACCENT : 0xC8C8D0, false);
        String value = String.format(Locale.ROOT, "%.2f", getter.get());
        g.drawString(font, value, right - font.width(value), y, 0x909098, false);
        hits.add(new Hit(left, y, right - left, 11, () -> {
            tunePose = pose;
            RfpPoseOverride.set(pose);
        }));
        y += 12;

        int trackL = left;
        int trackR = right;
        int trackY = y;
        float t = offsetToSlider(getter.get());
        int thumbX = trackL + Math.round((trackR - trackL - 4) * Mth.clamp(t, 0f, 1f));

        g.fill(trackL, trackY + 5, trackR, trackY + 7, 0xFF2A2A32);
        g.fill(thumbX, trackY + 2, thumbX + 4, trackY + SLIDER_H - 2, selected ? ACCENT : 0xFF6A8A9A);

        hits.add(new Hit(trackL, trackY, trackR - trackL, SLIDER_H, () -> {
            tunePose = pose;
            RfpPoseOverride.set(pose);
            dragging = new SliderDrag(trackL, trackR, setter);
            applySlider(mx, dragging);
        }));
        return y + SLIDER_H + 4;
    }

    private static float offsetToSlider(float value) {
        float min = FeaturesConfig.RFP_OFFSET_MIN;
        float max = FeaturesConfig.RFP_OFFSET_MAX;
        return (value - min) / (max - min);
    }

    private static float sliderToOffset(float t) {
        float min = FeaturesConfig.RFP_OFFSET_MIN;
        float max = FeaturesConfig.RFP_OFFSET_MAX;
        return min + Mth.clamp(t, 0f, 1f) * (max - min);
    }

    private void applySlider(double mouseX, SliderDrag drag) {
        float t = (float) ((mouseX - drag.left) / Math.max(1, drag.right - drag.left));
        drag.setter.accept(sliderToOffset(t));
    }

    private int drawSection(GuiGraphics g, int x, int y, String key) {
        g.drawString(font, Component.translatable(key), x, y, 0xC8C8D0, false);
        return y + 12;
    }

    private int drawCheckRow(GuiGraphics g, int x, int y, int mx, int my, boolean on,
                             Runnable action, String labelKey) {
        int color = on ? 0xF0F0F0 : 0x9A9AA8;
        drawCheck(g, x, y + 3, on);
        g.drawString(font, Component.translatable(labelKey), x + CHECK + 6, y + 3, color, false);
        hits.add(new Hit(x, y, settingsR - 12 - x, ROW - 2, action));
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

    private boolean insideSettings(double mx, double my) {
        return mx >= settingsL && mx < settingsR && my >= panelTop && my < panelBottom;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && insideSettings(mouseX, mouseY)) {
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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && dragging != null) {
            applySlider(mouseX, dragging);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll <= 0 || !insideSettings(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        scroll = Mth.clamp(scroll - (int) Math.signum(delta) * 16, 0, maxScroll);
        return true;
    }

    @Override
    public void onClose() {
        RfpPoseOverride.clear();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void removed() {
        RfpPoseOverride.clear();
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // Intentionally empty — keep the live FP world on the left undimmed.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }

    private record SliderDrag(int left, int right, Consumer<Float> setter) {
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
            graphics.fill(getX(), getY(), getX() + width, getY() + height, hover ? 0xFF2A3A48 : 0xFF1A1A22);
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
