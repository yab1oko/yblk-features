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
import yblk.features.client.ui.NumberField;
import yblk.features.client.ui.ScrollBar;
import yblk.features.client.ui.SectionHeader;
import yblk.features.config.FeaturesConfig;
import yblk.features.network.ModNetwork;
import yblk.features.network.WetnessConfigPacket;
import yblk.features.wetness.WetnessAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WetnessSettingsScreen extends BaseSettingsScreen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFF4A9EBF;
    private static final int ROW = 16;
    private static final int PANEL_W = 360;

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
        super(Component.translatable("screen." + Features.MODID + ".wetness"), parent);
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

        addRenderableWidget(darkButton(width / 2 - 96, height - bottomPad - btnH, 90, btnH,
                Component.translatable("screen." + Features.MODID + ".reset_section"), this::resetSection));
        addRenderableWidget(darkButton(width / 2 + 6, height - bottomPad - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    private void addRateBox(float value, Consumer<Float> setter) {
        boxes.add(NumberField.decimal(font, value, canEdit, setter));
    }

    private void addPctBox(float value, Consumer<Float> setter) {
        boxes.add(NumberField.percent(font, value, canEdit, setter));
    }

    private void addIntBox(int value, int min, int max, Consumer<Integer> setter) {
        boxes.add(NumberField.integer(font, value, min, max, canEdit, setter));
    }

    private boolean nearDefault(int index) {
        float[] defaults = {1f, 1f, 1.5f, 2f, 3f, 4f, 240f, 0.20f};
        float value = switch (index) {
            case 0 -> FeaturesConfig.wetnessRainRate;
            case 1 -> FeaturesConfig.wetnessDryRate;
            case 2 -> FeaturesConfig.wetnessHeatTorch;
            case 3 -> FeaturesConfig.wetnessHeatFire;
            case 4 -> FeaturesConfig.wetnessHeatLava;
            case 5 -> FeaturesConfig.wetnessHeatRadius;
            case 6 -> FeaturesConfig.wetnessFreezeSeconds;
            default -> FeaturesConfig.wetnessArmorSlow;
        };
        return Math.abs(value - defaults[index]) < 0.011f;
    }

    private void resetSection() {
        if (!canEdit) {
            return;
        }
        FeaturesConfig.setWetnessRainRate(1f);
        FeaturesConfig.setWetnessDryRate(1f);
        FeaturesConfig.setWetnessHeatTorch(1.5f);
        FeaturesConfig.setWetnessHeatFire(2f);
        FeaturesConfig.setWetnessHeatLava(3f);
        FeaturesConfig.setWetnessHeatRadius(4);
        FeaturesConfig.setWetnessFreezeSeconds(240);
        FeaturesConfig.setWetnessArmorSlow(0.20f);
        push();
        rebuildWidgets();
    }

    private void push() {
        if (canEdit) {
            ModNetwork.sendToServer(WetnessConfigPacket.current());
        }
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
            dirtyDot(graphics, x - 6, y + 4, !nearDefault(i));
            tipKey(x, y, panelR - 12 - x, ROW, keys[i]);
            box.setX(panelR - 12 - box.getWidth());
            box.setY(y);
            boolean visible = y + 14 > panelTop + 2 && y < panelBottom - 2;
            box.visible = visible;
            box.active = visible && canEdit;
            box.setEditable(canEdit);
            y += ROW;
        }

        graphics.disableScissor();
        ScrollBar.draw(graphics, panelR, panelTop, panelBottom, scroll, maxScroll, ACCENT, v -> scroll = v);

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
    protected int buttonAccent() {
        return ACCENT;
    }

    @Override
    protected int buttonHover() {
        return 0xFF2A3A48;
    }
}
