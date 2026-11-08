package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class TrueNightSettingsScreen extends Screen {
        private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFF9B3DCF;
    private static final int CHECK = 9;
    private static final int ROW = 16;
    private static final int PANEL_W = 340;

    private final Screen parent;
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

    public TrueNightSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".true_night"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        hits.clear();
        draggingDarkness = false;

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

        addRenderableWidget(new DarkButton(width / 2 - 45, height - bottomPad - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    private EditBox percentBox(int x, float value, Consumer<Float> setter) {
        EditBox box = new EditBox(font, x, 0, 48, 14, Component.empty());
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

        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".true_night.darkness");
        y = drawCheckRow(graphics, x, y, mouseX, mouseY, FeaturesConfig.trueNightMoonPhases,
                () -> FeaturesConfig.setTrueNightMoonPhases(!FeaturesConfig.trueNightMoonPhases),
                FeaturesConfig.trueNightMoonPhases
                        ? "screen." + Features.MODID + ".true_night.moon.on"
                        : "screen." + Features.MODID + ".true_night.moon.off");

        String strengthKey = FeaturesConfig.trueNightMoonPhases
                ? "screen." + Features.MODID + ".true_night.darkness.value.moon"
                : "screen." + Features.MODID + ".true_night.darkness.value.always";
        graphics.drawString(font, Component.translatable(strengthKey), x, y + 2, 0xC8C8D0, false);
        y += 12;
        y = drawDarknessSlider(graphics, x, y, mouseX, mouseY);

        y += 6;
        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".true_night.fog");
        y = drawCheckRow(graphics, x, y, mouseX, mouseY, FeaturesConfig.trueNightFogEnabled,
                () -> FeaturesConfig.setTrueNightFogEnabled(!FeaturesConfig.trueNightFogEnabled),
                FeaturesConfig.trueNightFogEnabled
                        ? "screen." + Features.MODID + ".true_night.fog.on"
                        : "screen." + Features.MODID + ".true_night.fog.off");

        boolean fogOn = FeaturesConfig.trueNightFogEnabled;
        chanceBox.active = fogOn;
        fogMinBox.active = fogOn;
        fogMaxBox.active = fogOn;
        chanceBox.setEditable(fogOn);
        fogMinBox.setEditable(fogOn);
        fogMaxBox.setEditable(fogOn);

        if (fogOn) {
            y = placeRow(graphics, x, y, chanceBox, "screen." + Features.MODID + ".true_night.fog.chance");
            y = placeRow(graphics, x, y, fogMinBox, "screen." + Features.MODID + ".true_night.fog.min");
            placeRow(graphics, x, y, fogMaxBox, "screen." + Features.MODID + ".true_night.fog.max");
        } else {
            chanceBox.setY(-100);
            fogMinBox.setY(-100);
            fogMaxBox.setY(-100);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
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

        int trackY = y + 5;
        int trackH = 4;
        g.fill(x, trackY, x + sw, trackY + trackH, 0xFF2A2A32);
        int fillR = x + Math.round(sw * value);
        if (fillR > x) {
            g.fill(x, trackY, fillR, trackY + trackH, ACCENT);
        }
        int knobW = 6;
        int knobX = Mth.clamp(fillR - knobW / 2, x, x + sw - knobW);
        boolean hover = mx >= x && mx < x + sw && my >= y && my < y + sh;
        g.fill(knobX, trackY - 2, knobX + knobW, trackY + trackH + 2,
                hover || draggingDarkness ? 0xFFF0F0F0 : 0xFFC0C0C8);
        return y + sh + 4;
    }

    private void applyDarknessFromMouse(double mouseX) {
        float v = darknessSliderW <= 0 ? 0f : (float) ((mouseX - darknessSliderX) / darknessSliderW);
        FeaturesConfig.setTrueNightDarkness(Mth.clamp(v, 0f, 1f));
    }

    private int placeRow(GuiGraphics g, int x, int y, EditBox box, String labelKey) {
        g.drawString(font, Component.translatable(labelKey), x, y + 3, 0xC8C8D0, false);
        box.setX(panelR - 12 - box.getWidth());
        box.setY(y);
        g.drawString(font, "%", box.getX() + box.getWidth() + 2, y + 3, 0x808088, false);
        return y + ROW + 2;
    }

    private int drawSection(GuiGraphics g, int x, int y, String key) {
        g.drawString(font, Component.translatable(key), x, y, 0xC8C8D0, false);
        return y + 12;
    }

    private int drawCheckRow(GuiGraphics g, int x, int y, int mx, int my, boolean on,
                             Runnable action, String labelKey) {
        drawCheck(g, x, y + 3, on);
        g.drawString(font, Component.translatable(labelKey), x + CHECK + 6, y + 3, on ? 0xF0F0F0 : 0x9A9AA8, false);
        hits.add(new Hit(x, y, panelR - 12 - x, ROW - 2, action));
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseX >= darknessSliderX && mouseX < darknessSliderX + darknessSliderW
                    && mouseY >= darknessSliderY && mouseY < darknessSliderY + 14) {
                draggingDarkness = true;
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
        if (draggingDarkness && button == 0) {
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
            graphics.fill(getX(), getY(), getX() + width, getY() + height, hover ? 0xFF2A2038 : 0xFF1A1A22);
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
