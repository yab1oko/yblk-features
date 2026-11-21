package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import yblk.features.Features;
import yblk.features.client.ui.BaseSettingsScreen;
import yblk.features.client.ui.Checks;
import yblk.features.client.ui.DarkButton;
import yblk.features.client.ui.DimensionPick;
import yblk.features.client.ui.HintLines;
import yblk.features.client.ui.ScrollBar;
import yblk.features.client.ui.TextField;
import yblk.features.config.FeaturesConfig;
import yblk.features.network.ElytraRulesPacket;
import yblk.features.network.ModNetwork;
import yblk.features.restrict.Restrictions;
import yblk.features.world.ElytraRules;

import java.util.ArrayList;
import java.util.List;

public class ElytraRulesSettingsScreen extends BaseSettingsScreen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFF3C8F8A;
    private static final int ROW = 16;
    private static final int CHECK = 9;

    private final List<Hit> hits = new ArrayList<>();
    private EditBox messageBox;
    private EditBox dimensionBox;
    private EditBox secondsBox;
    private DarkButton elytraButton;
    private DarkButton fireworkButton;
    private DarkButton flightButton;
    private boolean elytra = true;
    private boolean firework = true;
    private boolean flight = true;
    private boolean canEdit;
    private int panelL;
    private int panelR;
    private int panelTop;
    private int panelBottom;
    private int listTop;
    private int listBottom;
    private int scroll;
    private int maxScroll;
    private int dimY;
    private int flagY;
    private List<RegistrySuggest.Row> dims = List.of();

    public ElytraRulesSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".elytra_rules"), parent);
    }

    @Override
    protected void init() {
        clearWidgets();
        hits.clear();
        canEdit = Restrictions.canEdit(minecraft != null ? minecraft.player : null);
        int btnH = 18;
        panelL = (width - Math.min(400, width - 16)) / 2;
        panelR = width - panelL;
        panelTop = 22;
        panelBottom = height - 6 - btnH - 6;
        int inner = panelR - panelL - 24;
        int x = panelL + 12;
        int addW = font.width(Component.translatable("screen." + Features.MODID + ".elytra_rules.add")) + 12;

        messageBox = field(x, panelTop + 52, inner, FeaturesConfig.elytraMessage,
                "screen." + Features.MODID + ".elytra_rules.message", value -> {
                    if (!value.equals(FeaturesConfig.elytraMessage)) {
                        FeaturesConfig.setElytraMessage(value);
                        push();
                    }
                });
        messageBox.setMaxLength(256);
        addRenderableWidget(messageBox);

        dimensionBox = field(x, panelTop + 72, inner, "",
                "screen." + Features.MODID + ".elytra_rules.search", null);
        addRenderableWidget(dimensionBox);
        dimY = panelTop + 88;
        flagY = dimY + DimensionPick.ROW * 4 + 2;

        secondsBox = field(panelR - 12 - addW - 74, flagY, 70, "0",
                "screen." + Features.MODID + ".elytra_rules.seconds", null);
        addRenderableWidget(secondsBox);
        DarkButton add = darkButton(panelR - 12 - addW, flagY, addW, 14,
                Component.translatable("screen." + Features.MODID + ".elytra_rules.add"), this::addRule);
        add.active = canEdit;
        addRenderableWidget(add);

        int buttonW = Math.max(48, (inner - addW - 82) / 3);
        elytraButton = darkButton(x, flagY, buttonW, 14, flagLabel("elytra", elytra), () -> {
            elytra = !elytra;
            elytraButton.setMessage(flagLabel("elytra", elytra));
        });
        fireworkButton = darkButton(x + buttonW + 4, flagY, buttonW, 14, flagLabel("firework", firework), () -> {
            firework = !firework;
            fireworkButton.setMessage(flagLabel("firework", firework));
        });
        flightButton = darkButton(x + (buttonW + 4) * 2, flagY, buttonW, 14, flagLabel("flight", flight), () -> {
            flight = !flight;
            flightButton.setMessage(flagLabel("flight", flight));
        });
        elytraButton.active = canEdit;
        fireworkButton.active = canEdit;
        flightButton.active = canEdit;
        addRenderableWidget(elytraButton);
        addRenderableWidget(fireworkButton);
        addRenderableWidget(flightButton);

        listTop = flagY + 18;
        listBottom = Math.max(listTop + 24, panelBottom - 4);
        addRenderableWidget(darkButton(width / 2 - 45, height - 6 - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    private EditBox field(int x, int y, int w, String value, String hint, java.util.function.Consumer<String> responder) {
        EditBox box = new TextField(font, x, y, Math.max(40, w), 14, Component.empty());
        box.setMaxLength(64);
        box.setValue(value);
        box.setHint(Component.translatable(hint));
        box.setEditable(canEdit);
        if (responder != null) {
            box.setResponder(text -> {
                if (canEdit) {
                    responder.accept(text);
                }
            });
        }
        return box;
    }

    private Component flagLabel(String kind, boolean allowed) {
        String state = allowed
                ? "screen." + Features.MODID + ".elytra_rules.on"
                : "screen." + Features.MODID + ".elytra_rules.off";
        return Component.translatable("screen." + Features.MODID + ".elytra_rules." + kind, Component.translatable(state));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        if (elytraButton != null) {
            elytraButton.setTooltip(Tooltip.create(Component.translatable("screen." + Features.MODID + ".elytra_rules.elytra.tip")));
            fireworkButton.setTooltip(Tooltip.create(Component.translatable("screen." + Features.MODID + ".elytra_rules.firework.tip")));
            flightButton.setTooltip(Tooltip.create(Component.translatable("screen." + Features.MODID + ".elytra_rules.flight.tip")));
        }
        List<String> rules = FeaturesConfig.elytraRules;
        int content = Math.max(1, rules.size()) * ROW;
        maxScroll = Math.max(0, content - Math.max(1, listBottom - listTop));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xF0F0F0);
        graphics.fill(panelL, panelTop, panelR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelTop, panelR - panelL, panelBottom - panelTop, ACCENT, 0xFF0A0A0E);

        int x = panelL + 12;
        String hintKey = canEdit
                ? "screen." + Features.MODID + ".elytra_rules.hint"
                : "screen." + Features.MODID + ".elytra_rules.locked";
        HintLines.draw(graphics, font, Component.translatable(hintKey), x, panelTop + 4, panelR - panelL - 24,
                canEdit ? 0x808088 : 0xE05050, 2);

        Checks.box(graphics, x, panelTop + 26, CHECK, FeaturesConfig.elytraOpBypass, ACCENT);
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".elytra_rules.op_bypass"),
                x + CHECK + 6, panelTop + 26, canEdit ? 0xF0F0F0 : 0x6A6A72, false);
        tipKey(x, panelTop + 24, panelR - 12 - x, 14, "screen." + Features.MODID + ".elytra_rules.op_bypass");
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".elytra_rules.message"),
                x, panelTop + 40, 0xC8C8D0, false);
        dims = DimensionPick.visible(dimensionBox.getValue(), 4);
        DimensionPick.draw(graphics, font, dims, dimensionBox.getValue().trim(), x, dimY, panelR - 12 - x);

        graphics.enableScissor(panelL + 4, listTop, panelR - 4, listBottom);
        int y = listTop - scroll;
        int rowW = panelR - 28 - x;
        if (rules.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".elytra_rules.empty"),
                    x, y + 3, 0x6A6A72, false);
        }
        for (String raw : rules) {
            if (y + ROW > listTop && y < listBottom) {
                ElytraRules.Rule rule = ElytraRules.parse(raw);
                String text = rule == null ? raw : rowText(rule);
                graphics.drawString(font, font.plainSubstrByWidth(text, rowW - 14), x, y + 4, 0xF0F0F0, false);
                graphics.drawString(font, "x", x + rowW - 8, y + 4, 0xC96060, false);
                if (canEdit && y >= listTop && y + ROW <= listBottom) {
                    String stored = raw;
                    hits.add(new Hit(x, y, rowW, ROW, () -> remove(stored)));
                }
            }
            y += ROW;
        }
        graphics.disableScissor();
        ScrollBar.draw(graphics, panelR, listTop, listBottom, scroll, maxScroll, ACCENT, v -> scroll = v);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String rowText(ElytraRules.Rule rule) {
        return Component.translatable(
                "screen." + Features.MODID + ".elytra_rules.row",
                RegistrySuggest.dimensionName(rule.dimension()),
                Component.translatable(rule.elytra()
                        ? "screen." + Features.MODID + ".elytra_rules.on"
                        : "screen." + Features.MODID + ".elytra_rules.off"),
                Component.translatable(rule.firework()
                        ? "screen." + Features.MODID + ".elytra_rules.on"
                        : "screen." + Features.MODID + ".elytra_rules.off"),
                rule.seconds(),
                Component.translatable(rule.flight()
                        ? "screen." + Features.MODID + ".elytra_rules.on"
                        : "screen." + Features.MODID + ".elytra_rules.off")
        ).getString();
    }

    private void addRule() {
        if (!canEdit) {
            return;
        }
        ElytraRules.Rule rule = ElytraRules.build(
                dimensionBox.getValue(), elytra, firework, secondsBox.getValue(), flight);
        if (rule == null) {
            return;
        }
        FeaturesConfig.setElytraRules(ElytraRules.upsert(FeaturesConfig.elytraRules, rule));
        dimensionBox.setValue("");
        push();
    }

    private void remove(String stored) {
        if (!canEdit) {
            return;
        }
        FeaturesConfig.setElytraRules(ElytraRules.removeStored(FeaturesConfig.elytraRules, stored));
        push();
    }

    private void push() {
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetwork.sendToServer(ElytraRulesPacket.current());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && canEdit) {
            int x = panelL + 12;
            int w = panelR - 12 - x;
            for (int i = 0; i < dims.size(); i++) {
                int ry = dimY + i * DimensionPick.ROW;
                if (mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + DimensionPick.ROW) {
                    dimensionBox.setValue(dims.get(i).id());
                    return true;
                }
            }
        }
        if (button == 0 && canEdit && hitCheck(mouseX, mouseY, panelL + 12, panelTop + 26)) {
            FeaturesConfig.setElytraOpBypass(!FeaturesConfig.elytraOpBypass);
            push();
            return true;
        }
        if (button == 0) {
            for (Hit hit : hits) {
                if (mouseX < hit.x || mouseX >= hit.x + hit.w || mouseY < hit.y || mouseY >= hit.y + hit.h) {
                    continue;
                }
                if (mouseY < listTop || mouseY >= listBottom) {
                    continue;
                }
                hit.action.run();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean hitCheck(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < panelR - 12 && mouseY >= y && mouseY < y + 14;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= listTop && mouseY < listBottom) {
            scroll = Mth.clamp(scroll - (int) (delta * ROW), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (dimensionBox.isFocused() || secondsBox.isFocused()) {
                addRule();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected int buttonAccent() {
        return ACCENT;
    }

    @Override
    protected int buttonHover() {
        return 0xFF1A3030;
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }
}
