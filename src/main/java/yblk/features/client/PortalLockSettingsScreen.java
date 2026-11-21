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
import yblk.features.network.ModNetwork;
import yblk.features.network.PortalLockPacket;
import yblk.features.restrict.Restrictions;
import yblk.features.world.PortalRules;

import java.util.ArrayList;
import java.util.List;

public class PortalLockSettingsScreen extends BaseSettingsScreen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFF7A4CC8;
    private static final int ROW = 16;
    private static final int CHECK = 9;

    private final List<Hit> hits = new ArrayList<>();
    private EditBox messageBox;
    private EditBox dimensionBox;
    private EditBox valueBox;
    private DarkButton modeButton;
    private int mode;
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
    private List<RegistrySuggest.Row> dims = List.of();

    public PortalLockSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".portal_lock"), parent);
    }

    @Override
    protected void init() {
        clearWidgets();
        hits.clear();
        canEdit = Restrictions.canEdit(minecraft != null ? minecraft.player : null);
        int btnH = 18;
        panelL = (width - Math.min(360, width - 16)) / 2;
        panelR = width - panelL;
        panelTop = 22;
        panelBottom = height - 6 - btnH - 6;
        int inner = panelR - panelL - 24;
        int x = panelL + 12;

        messageBox = field(x, panelTop + 72, inner, FeaturesConfig.portalMessage,
                "screen." + Features.MODID + ".portal_lock.message", value -> {
                    if (!value.equals(FeaturesConfig.portalMessage)) {
                        FeaturesConfig.setPortalMessage(value);
                        push();
                    }
                });
        addRenderableWidget(messageBox);

        dimensionBox = field(x, panelTop + 92, inner, "",
                "screen." + Features.MODID + ".portal_lock.search", null);
        addRenderableWidget(dimensionBox);
        dimY = panelTop + 108;

        int addW = font.width(Component.translatable("screen." + Features.MODID + ".portal_lock.add")) + 12;
        int rowY = dimY + DimensionPick.ROW * 4 + 2;
        modeButton = darkButton(x, rowY, 92, 14, modeLabel(), this::cycleMode);
        modeButton.active = canEdit;
        addRenderableWidget(modeButton);
        valueBox = field(x + 96, rowY, inner - 96 - addW - 4, "",
                "screen." + Features.MODID + ".portal_lock.value", null);
        valueBox.setEditable(canEdit && mode >= 2);
        addRenderableWidget(valueBox);
        DarkButton add = darkButton(panelR - 12 - addW, rowY, addW, 14,
                Component.translatable("screen." + Features.MODID + ".portal_lock.add"), this::addRule);
        add.active = canEdit;
        addRenderableWidget(add);

        listTop = rowY + 18;
        listBottom = Math.max(listTop + 24, panelBottom - 4);
        addRenderableWidget(darkButton(width / 2 - 45, height - 6 - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    private EditBox field(int x, int y, int w, String value, String hint, java.util.function.Consumer<String> responder) {
        EditBox box = new TextField(font, x, y, Math.max(40, w), 14, Component.empty());
        box.setMaxLength(256);
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

    private void cycleMode() {
        if (!canEdit) {
            return;
        }
        mode = (mode + 1) % 4;
        modeButton.setMessage(modeLabel());
        valueBox.setEditable(mode >= 2);
    }

    private Component modeLabel() {
        String key = switch (mode) {
            case 1 -> "screen." + Features.MODID + ".portal_lock.mode.block";
            case 2 -> "screen." + Features.MODID + ".portal_lock.mode.day";
            case 3 -> "screen." + Features.MODID + ".portal_lock.mode.date";
            default -> "screen." + Features.MODID + ".portal_lock.mode.allow";
        };
        return Component.translatable(key);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        if (modeButton != null) {
            String modeKey = switch (mode) {
                case 1 -> "screen." + Features.MODID + ".portal_lock.mode.block";
                case 2 -> "screen." + Features.MODID + ".portal_lock.mode.day";
                case 3 -> "screen." + Features.MODID + ".portal_lock.mode.date";
                default -> "screen." + Features.MODID + ".portal_lock.mode.allow";
            };
            modeButton.setTooltip(Tooltip.create(Component.translatable(modeKey + ".tip")));
        }
        List<String> rules = FeaturesConfig.portalRules;
        int content = Math.max(1, rules.size()) * ROW;
        maxScroll = Math.max(0, content - Math.max(1, listBottom - listTop));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xF0F0F0);
        graphics.fill(panelL, panelTop, panelR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelTop, panelR - panelL, panelBottom - panelTop, ACCENT, 0xFF0A0A0E);

        int x = panelL + 12;
        String hintKey = canEdit
                ? "screen." + Features.MODID + ".portal_lock.hint"
                : "screen." + Features.MODID + ".portal_lock.locked";
        HintLines.draw(graphics, font, Component.translatable(hintKey), x, panelTop + 4, panelR - panelL - 24,
                canEdit ? 0x808088 : 0xE05050, 2);

        Checks.box(graphics, x, panelTop + 30, CHECK, FeaturesConfig.portalOpBypass, ACCENT);
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".portal_lock.op_bypass"),
                x + CHECK + 6, panelTop + 30, canEdit ? 0xF0F0F0 : 0x6A6A72, false);
        tipKey(x, panelTop + 28, panelR - 12 - x, 14, "screen." + Features.MODID + ".portal_lock.op_bypass");
        Checks.box(graphics, x, panelTop + 46, CHECK, FeaturesConfig.portalBlockCreation, ACCENT);
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".portal_lock.creation"),
                x + CHECK + 6, panelTop + 46, canEdit ? 0xF0F0F0 : 0x6A6A72, false);
        tipKey(x, panelTop + 44, panelR - 12 - x, 14, "screen." + Features.MODID + ".portal_lock.creation");
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".portal_lock.message"),
                x, panelTop + 60, 0xC8C8D0, false);
        dims = DimensionPick.visible(dimensionBox.getValue(), 4);
        DimensionPick.draw(graphics, font, dims, dimensionBox.getValue().trim(), x, dimY, panelR - 12 - x);

        graphics.enableScissor(panelL + 4, listTop, panelR - 4, listBottom);
        int y = listTop - scroll;
        int rowW = panelR - 28 - x;
        if (rules.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".portal_lock.empty"),
                    x, y + 3, 0x6A6A72, false);
        }
        for (String raw : rules) {
            if (y + ROW > listTop && y < listBottom) {
                PortalRules.Rule rule = PortalRules.parse(raw);
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

    private String rowText(PortalRules.Rule rule) {
        String name = RegistrySuggest.dimensionName(rule.dimension());
        return switch (rule.mode()) {
            case BLOCK -> Component.translatable("screen." + Features.MODID + ".portal_lock.row.block", name).getString();
            case DAY -> Component.translatable("screen." + Features.MODID + ".portal_lock.row.day", name, rule.day()).getString();
            case DATE -> Component.translatable("screen." + Features.MODID + ".portal_lock.row.date", name, rule.date()).getString();
            case ALLOW -> Component.translatable("screen." + Features.MODID + ".portal_lock.row.allow", name).getString();
        };
    }

    private void addRule() {
        if (!canEdit) {
            return;
        }
        PortalRules.Rule rule = PortalRules.build(dimensionBox.getValue(), mode, valueBox.getValue());
        if (rule == null) {
            return;
        }
        FeaturesConfig.setPortalRules(PortalRules.upsert(FeaturesConfig.portalRules, rule));
        dimensionBox.setValue("");
        valueBox.setValue("");
        push();
    }

    private void remove(String stored) {
        if (!canEdit) {
            return;
        }
        FeaturesConfig.setPortalRules(PortalRules.removeStored(FeaturesConfig.portalRules, stored));
        push();
    }

    private void push() {
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetwork.sendToServer(PortalLockPacket.current());
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
        if (button == 0) {
            int x = panelL + 12;
            if (canEdit && hitCheck(mouseX, mouseY, x, panelTop + 30)) {
                FeaturesConfig.setPortalOpBypass(!FeaturesConfig.portalOpBypass);
                push();
                return true;
            }
            if (canEdit && hitCheck(mouseX, mouseY, x, panelTop + 46)) {
                FeaturesConfig.setPortalBlockCreation(!FeaturesConfig.portalBlockCreation);
                push();
                return true;
            }
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
            if (dimensionBox.isFocused() || valueBox.isFocused()) {
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
        return 0xFF2A2040;
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }
}
