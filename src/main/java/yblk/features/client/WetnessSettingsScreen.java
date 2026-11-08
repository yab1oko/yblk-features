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
import yblk.features.network.ModNetwork;
import yblk.features.network.WetnessConfigPacket;
import yblk.features.wetness.WetnessAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class WetnessSettingsScreen extends Screen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFF4A9EBF;
    private static final int ROW = 16;
    private static final int PANEL_W = 360;

    private final Screen parent;
    private final List<EditBox> boxes = new ArrayList<>();
    private boolean canEdit;
    private int panelL;
    private int panelR;
    private int panelTop;
    private int panelBottom;
    private int scroll;
    private int maxScroll;
    private int contentH;

    public WetnessSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".wetness"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        boxes.clear();
        canEdit = WetnessAccess.canEdit(minecraft != null ? minecraft.player : null);

        int btnH = 18;
        int bottomPad = 6;
        panelL = (width - Math.min(PANEL_W, width - 16)) / 2;
        panelR = width - panelL;
        panelTop = 22;
        panelBottom = height - bottomPad - btnH - 6;

        addRateBox(FeaturesConfig.wetnessRainRate, v -> {
            FeaturesConfig.setWetnessRainRate(v);
            push();
        });
        addRateBox(FeaturesConfig.wetnessDryRate, v -> {
            FeaturesConfig.setWetnessDryRate(v);
            push();
        });
        addRateBox(FeaturesConfig.wetnessHeatTorch, v -> {
            FeaturesConfig.setWetnessHeatTorch(v);
            push();
        });
        addRateBox(FeaturesConfig.wetnessHeatFire, v -> {
            FeaturesConfig.setWetnessHeatFire(v);
            push();
        });
        addRateBox(FeaturesConfig.wetnessHeatLava, v -> {
            FeaturesConfig.setWetnessHeatLava(v);
            push();
        });
        addIntBox(FeaturesConfig.wetnessHeatRadius, 1, 8, v -> {
            FeaturesConfig.setWetnessHeatRadius(v);
            push();
        });
        addIntBox(FeaturesConfig.wetnessFreezeSeconds, 30, 1800, v -> {
            FeaturesConfig.setWetnessFreezeSeconds(v);
            push();
        });
        addPctBox(FeaturesConfig.wetnessArmorSlow, v -> {
            FeaturesConfig.setWetnessArmorSlow(v);
            push();
        });

        for (EditBox box : boxes) {
            addRenderableWidget(box);
        }

        contentH = 14 + 12 + boxes.size() * ROW + 24;
        maxScroll = Math.max(0, contentH - Math.max(1, panelBottom - panelTop - 16));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        addRenderableWidget(new DarkButton(width / 2 - 45, height - bottomPad - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    private void addRateBox(float value, Consumer<Float> setter) {
        EditBox box = new EditBox(font, 0, 0, 48, 14, Component.empty());
        box.setMaxLength(6);
        box.setValue(fmt(value));
        box.setEditable(canEdit);
        box.setResponder(raw -> {
            if (!canEdit || raw == null || raw.isBlank()) {
                return;
            }
            try {
                setter.accept(Float.parseFloat(raw.trim()));
            } catch (NumberFormatException ignored) {
            }
        });
        boxes.add(box);
    }

    private void addPctBox(float value, Consumer<Float> setter) {
        EditBox box = new EditBox(font, 0, 0, 48, 14, Component.empty());
        box.setMaxLength(5);
        box.setValue(String.format(Locale.ROOT, "%d", Math.round(value * 100f)));
        box.setEditable(canEdit);
        box.setResponder(raw -> {
            if (!canEdit || raw == null || raw.isBlank()) {
                return;
            }
            try {
                setter.accept(Mth.clamp(Float.parseFloat(raw.trim()) / 100f, 0f, 1f));
            } catch (NumberFormatException ignored) {
            }
        });
        boxes.add(box);
    }

    private void addIntBox(int value, int min, int max, Consumer<Integer> setter) {
        EditBox box = new EditBox(font, 0, 0, 48, 14, Component.empty());
        box.setMaxLength(5);
        box.setValue(Integer.toString(value));
        box.setEditable(canEdit);
        box.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        box.setResponder(raw -> {
            if (!canEdit || raw == null || raw.isBlank()) {
                return;
            }
            try {
                setter.accept(Mth.clamp(Integer.parseInt(raw.trim()), min, max));
            } catch (NumberFormatException ignored) {
            }
        });
        boxes.add(box);
    }

    private void push() {
        if (canEdit) {
            ModNetwork.sendToServer(WetnessConfigPacket.current());
        }
    }

    private static String fmt(float v) {
        if (Math.abs(v - Math.round(v)) < 0.001f) {
            return Integer.toString(Math.round(v));
        }
        return String.format(Locale.ROOT, "%.1f", v);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xF0F0F0);

        graphics.fill(panelL, panelTop, panelR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelTop, panelR - panelL,
                panelBottom - panelTop, ACCENT, 0xFF0A0A0E);

        graphics.enableScissor(panelL + 2, panelTop + 2, panelR - 2, panelBottom - 2);

        int x = panelL + 12;
        int y = panelTop + 10 - scroll;

        if (!canEdit) {
            graphics.drawString(font,
                    Component.translatable("screen." + Features.MODID + ".wetness.locked"),
                    x, y, 0xE05050, false);
            y += 16;
        }

        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".wetness.hint"),
                x, y, 0x808088, false);
        y += 16;

        String[] keys = {
                "screen." + Features.MODID + ".wetness.rain",
                "screen." + Features.MODID + ".wetness.dry",
                "screen." + Features.MODID + ".wetness.heat_torch",
                "screen." + Features.MODID + ".wetness.heat_fire",
                "screen." + Features.MODID + ".wetness.heat_lava",
                "screen." + Features.MODID + ".wetness.heat_radius",
                "screen." + Features.MODID + ".wetness.freeze",
                "screen." + Features.MODID + ".wetness.armor_slow"
        };

        for (int i = 0; i < boxes.size(); i++) {
            EditBox box = boxes.get(i);
            graphics.drawString(font, Component.translatable(keys[i]), x, y + 3,
                    canEdit ? 0xC8C8D0 : 0x606068, false);
            box.setX(panelR - 12 - box.getWidth());
            box.setY(y);
            boolean visible = y + 14 > panelTop + 2 && y < panelBottom - 2;
            box.visible = visible;
            box.active = visible && canEdit;
            box.setEditable(canEdit);
            y += ROW;
        }

        graphics.disableScissor();

        for (var child : children()) {
            if (child instanceof AbstractWidget widget) {
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= panelL && mouseX < panelR && mouseY >= panelTop && mouseY < panelBottom) {
            maxScroll = Math.max(0, contentH - Math.max(1, panelBottom - panelTop - 16));
            scroll = Mth.clamp(scroll - (int) Math.signum(delta) * 12, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        FeatureSettingsBackdrop.render(graphics, width, height);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
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
