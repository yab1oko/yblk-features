package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import yblk.features.Features;
import yblk.features.client.ui.BaseSettingsScreen;
import yblk.features.client.ui.TextField;
import yblk.features.client.ui.Checks;
import yblk.features.client.ui.ScrollBar;
import yblk.features.client.ui.SectionHeader;
import yblk.features.client.ui.SliderPaint;
import yblk.features.config.FeaturesConfig;
import yblk.features.night.TrueNight;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class TrueNightSettingsScreen extends BaseSettingsScreen {
        private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFF9B3DCF;
    private static final int CHECK = 9;
    private static final int ROW = 16;
    private static final int PANEL_W = 340;

    private final List<Hit> hits = new ArrayList<>();
    private EditBox chanceBox;
    private EditBox fogMinBox;
    private EditBox fogMaxBox;
    private int panelL;
    private int panelR;
    private int panelTop;
    private int panelBottom;
    private int darknessSliderX;
    private int darknessSliderY;
    private int darknessSliderW;
    private boolean draggingDarkness;
    private boolean canEdit;

    public TrueNightSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".true_night"), parent);
    }

    @Override
    protected void init() {
        clearWidgets();
        hits.clear();
        draggingDarkness = false;
        canEdit = TrueNight.canEdit(minecraft != null ? minecraft.player : null);

        int btnH = 18;
        int bottomPad = 6;
        panelL = (width - Math.min(PANEL_W, width - 16)) / 2;
        panelR = width - panelL;
        panelTop = 22;
        panelBottom = height - bottomPad - btnH - 6;

        int boxW = 48;
        int boxX = panelR - 12 - boxW;
        chanceBox = percentBox(boxX, FeaturesConfig.trueNightFogChance, FeaturesConfig::setTrueNightFogChance);
        fogMinBox = percentBox(boxX, FeaturesConfig.trueNightFogMin, v -> {
            FeaturesConfig.setTrueNightFogMin(v);
            if (FeaturesConfig.trueNightFogMax < v) {
                FeaturesConfig.setTrueNightFogMax(v);
                fogMaxBox.setValue(pct(v));
            }
        });
        fogMaxBox = percentBox(boxX, FeaturesConfig.trueNightFogMax, v -> {
            FeaturesConfig.setTrueNightFogMax(v);
            if (FeaturesConfig.trueNightFogMin > v) {
                FeaturesConfig.setTrueNightFogMin(v);
                fogMinBox.setValue(pct(v));
            }
        });
        addRenderableWidget(chanceBox);
        addRenderableWidget(fogMinBox);
        addRenderableWidget(fogMaxBox);

        addRenderableWidget(darkButton(width / 2 - 100, height - bottomPad - btnH, 90, btnH,
                Component.translatable("screen." + Features.MODID + ".reset_section"), this::resetSection));
        addRenderableWidget(darkButton(width / 2 + 10, height - bottomPad - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    private EditBox percentBox(int x, float value, Consumer<Float> setter) {
        EditBox box = new TextField(font, x, 0, 48, 14, Component.empty());
        box.setMaxLength(4);
        box.setValue(pct(value));
        box.setResponder(s -> {
            try {
                float v = Float.parseFloat(s.replace('%', ' ').trim()) / 100f;
                setter.accept(Mth.clamp(v, 0f, 1f));
            } catch (NumberFormatException ignored) {
            }
        });
        return box;
    }

    private static String pct(float v) {
        return String.format(Locale.ROOT, "%d", Math.round(Mth.clamp(v, 0f, 1f) * 100f));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        this.renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xF0F0F0);

        graphics.fill(panelL, panelTop, panelR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelTop, panelR - panelL,
                panelBottom - panelTop, ACCENT, 0xFF0A0A0E);

        int x = panelL + 12;
        int y = panelTop + 12;

        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".true_night.hint"),
                x, y, 0x808088, false);
        y += 16;
        if (!canEdit) {
            graphics.drawString(font,
                    Component.translatable("screen." + Features.MODID + ".true_night.locked"),
                    x, y, 0xE05050, false);
            y += 16;
        }

        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".true_night.darkness");
        dirtyDot(graphics, x - 6, y + 4, !FeaturesConfig.trueNightMoonPhases);
        y = drawCheckRow(graphics, x, y, mouseX, mouseY, FeaturesConfig.trueNightMoonPhases,
                () -> FeaturesConfig.setTrueNightMoonPhases(!FeaturesConfig.trueNightMoonPhases),
                FeaturesConfig.trueNightMoonPhases
                        ? "screen." + Features.MODID + ".true_night.moon.on"
                        : "screen." + Features.MODID + ".true_night.moon.off",
                canEdit);

        String strengthKey = FeaturesConfig.trueNightMoonPhases
                ? "screen." + Features.MODID + ".true_night.darkness.value.moon"
                : "screen." + Features.MODID + ".true_night.darkness.value.always";
        graphics.drawString(font, Component.translatable(strengthKey), x, y + 2, 0xC8C8D0, false);
        tipKey(x, y, panelR - 12 - x, 26, strengthKey);
        y += 12;
        y = drawDarknessSlider(graphics, x, y, mouseX, mouseY);

        y += 6;
        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".true_night.fog");
        dirtyDot(graphics, x - 6, y + 4, !FeaturesConfig.trueNightFogEnabled);
        y = drawCheckRow(graphics, x, y, mouseX, mouseY, FeaturesConfig.trueNightFogEnabled,
                () -> FeaturesConfig.setTrueNightFogEnabled(!FeaturesConfig.trueNightFogEnabled),
                FeaturesConfig.trueNightFogEnabled
                        ? "screen." + Features.MODID + ".true_night.fog.on"
                        : "screen." + Features.MODID + ".true_night.fog.off",
                canEdit);

        boolean fogOn = FeaturesConfig.trueNightFogEnabled;
        boolean fogEdit = canEdit && fogOn;
        chanceBox.active = fogEdit;
        fogMinBox.active = fogEdit;
        fogMaxBox.active = fogEdit;
        chanceBox.setEditable(fogEdit);
        fogMinBox.setEditable(fogEdit);
        fogMaxBox.setEditable(fogEdit);

        if (fogOn) {
            y = placeRow(graphics, x, y, chanceBox, "screen." + Features.MODID + ".true_night.fog.chance", FeaturesConfig.trueNightFogChance, 0.22f);
            y = placeRow(graphics, x, y, fogMinBox, "screen." + Features.MODID + ".true_night.fog.min", FeaturesConfig.trueNightFogMin, 0.35f);
            y = placeRow(graphics, x, y, fogMaxBox, "screen." + Features.MODID + ".true_night.fog.max", FeaturesConfig.trueNightFogMax, 0.95f);
        } else {
            chanceBox.setY(-100);
            fogMinBox.setY(-100);
            fogMaxBox.setY(-100);
        }

        if (canSeeAdminLock()) {
            y += 8;
            y = drawSection(graphics, x, y, "screen." + Features.MODID + ".true_night.admin");
            dirtyDot(graphics, x - 6, y + 4, FeaturesConfig.trueNightAdminLocked);
            drawCheckRow(graphics, x, y, mouseX, mouseY, FeaturesConfig.trueNightAdminLocked,
                    () -> {
                        FeaturesConfig.setTrueNightAdminLocked(!FeaturesConfig.trueNightAdminLocked);
                        canEdit = TrueNight.canEdit(minecraft != null ? minecraft.player : null);
                    },
                    FeaturesConfig.trueNightAdminLocked
                            ? "screen." + Features.MODID + ".true_night.admin.on"
                            : "screen." + Features.MODID + ".true_night.admin.off",
                    true);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private boolean canSeeAdminLock() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    private int drawDarknessSlider(GuiGraphics g, int x, int y, int mx, int my) {
        int sw = panelR - 12 - x;
        int sh = 14;
        darknessSliderX = x;
        darknessSliderY = y;
        darknessSliderW = sw;

        float value = Mth.clamp(FeaturesConfig.trueNightDarkness, 0f, 1f);
        int pct = Math.round(value * 100f);
        g.drawString(font, pct + "%", x + sw - font.width(pct + "%"), y - 10, 0x808088, false);
        dirtyDot(g, x - 6, y - 8, Math.abs(value - 1f) > 0.011f);

        boolean hover = mx >= x && mx < x + sw && my >= y && my < y + sh;
        return SliderPaint.fillKnob(g, x, y, sw, value, ACCENT, hover || draggingDarkness);
    }

    private void applyDarknessFromMouse(double mouseX) {
        float v = darknessSliderW <= 0 ? 0f : (float) ((mouseX - darknessSliderX) / darknessSliderW);
        FeaturesConfig.setTrueNightDarkness(Mth.clamp(v, 0f, 1f));
    }

    private int placeRow(GuiGraphics g, int x, int y, EditBox box, String labelKey, float value, float def) {
        g.drawString(font, Component.translatable(labelKey), x, y + 3, 0xC8C8D0, false);
        dirtyDot(g, x - 6, y + 5, Math.abs(value - def) > 0.011f);
        tipKey(x, y, panelR - 12 - x, ROW, labelKey);
        box.setX(panelR - 12 - box.getWidth());
        box.setY(y);
        g.drawString(font, "%", box.getX() + box.getWidth() + 2, y + 3, 0x808088, false);
        return y + ROW + 2;
    }

    private void resetSection() {
        FeaturesConfig.setTrueNightMoonPhases(true);
        FeaturesConfig.setTrueNightDarkness(1f);
        FeaturesConfig.setTrueNightFogEnabled(true);
        FeaturesConfig.setTrueNightAdminLocked(false);
        FeaturesConfig.setTrueNightFogChance(0.22f);
        FeaturesConfig.setTrueNightFogMin(0.35f);
        FeaturesConfig.setTrueNightFogMax(0.95f);
        chanceBox.setValue(pct(0.22f));
        fogMinBox.setValue(pct(0.35f));
        fogMaxBox.setValue(pct(0.95f));
    }

    private int drawSection(GuiGraphics g, int x, int y, String key) {
        return SectionHeader.draw(g, font, x, y, Component.translatable(key));
    }

    private int drawCheckRow(GuiGraphics g, int x, int y, int mx, int my, boolean on,
                             Runnable action, String labelKey, boolean active) {
        drawCheck(g, x, y + 3, on);
        int color = active ? (on ? 0xF0F0F0 : 0x9A9AA8) : 0x6A6A72;
        g.drawString(font, Component.translatable(labelKey), x + CHECK + 6, y + 3, color, false);
        tipKey(x, y, panelR - 12 - x, ROW, labelKey);
        if (active) {
            hits.add(new Hit(x, y, panelR - 12 - x, ROW - 2, action));
        }
        return y + ROW;
    }

    private static void drawCheck(GuiGraphics g, int x, int y, boolean on) {
        Checks.box(g, x, y, CHECK, on, ACCENT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (canEdit && mouseX >= darknessSliderX && mouseX < darknessSliderX + darknessSliderW
                    && mouseY >= darknessSliderY && mouseY < darknessSliderY + 14) {
                draggingDarkness = true;
                setDragging(true);
                applyDarknessFromMouse(mouseX);
                return true;
            }
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (canEdit && draggingDarkness && button == 0) {
            applyDarknessFromMouse(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingDarkness = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }


    @Override
    protected int buttonAccent() {
        return ACCENT;
    }

    @Override
    protected int buttonHover() {
        return 0xFF2A2038;
    }
}
