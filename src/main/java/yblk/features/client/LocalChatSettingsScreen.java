package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import yblk.features.Features;
import yblk.features.chat.LocalChat;
import yblk.features.config.FeaturesConfig;
import yblk.features.network.LocalChatConfigPacket;
import yblk.features.network.ModNetwork;

import java.util.ArrayList;
import java.util.List;

public class LocalChatSettingsScreen extends Screen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFF4A9EBF;
    private static final int CHECK = 9;
    private static final int ROW = 16;
    private static final int PANEL_W = 340;

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private EditBox rangeBox;
    private boolean canEdit;

    public LocalChatSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".local_chat"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        hits.clear();
        canEdit = LocalChat.canEdit(minecraft != null ? minecraft.player : null);

        int btnH = 18;
        int bottomPad = 6;
        int panelL = (width - Math.min(PANEL_W, width - 16)) / 2;
        int panelR = width - panelL;

        int boxW = 48;
        rangeBox = new EditBox(font, panelR - 12 - boxW, 0, boxW, 14, Component.empty());
        rangeBox.setMaxLength(4);
        rangeBox.setValue(Integer.toString(FeaturesConfig.localChatRange));
        rangeBox.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        rangeBox.setEditable(canEdit);
        rangeBox.setResponder(raw -> {
            if (!canEdit || raw == null || raw.isEmpty()) {
                return;
            }
            try {
                FeaturesConfig.setLocalChatRange(Integer.parseInt(raw));
                ModNetwork.sendToServer(LocalChatConfigPacket.current());
            } catch (NumberFormatException ignored) {
            }
        });
        addRenderableWidget(rangeBox);

        addRenderableWidget(new DarkButton(width / 2 - 45, height - bottomPad - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        this.renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xF0F0F0);

        int panelL = (width - Math.min(PANEL_W, width - 16)) / 2;
        int panelR = width - panelL;
        int panelTop = 22;
        int panelBottom = height - 30;

        graphics.fill(panelL, panelTop, panelR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelTop, panelR - panelL,
                panelBottom - panelTop, ACCENT, 0xFF0A0A0E);

        int x = panelL + 12;
        int y = panelTop + 12;

        if (!canEdit) {
            graphics.drawString(font,
                    Component.translatable("screen." + Features.MODID + ".local_chat.locked"),
                    x, y, 0xE05050, false);
            y += 18;
        }

        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".local_chat.hint"),
                x, y, 0x808088, false);
        y += 14;
        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".local_chat.hint.global"),
                x, y, 0x808088, false);
        y += 18;

        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".local_chat.range");
        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".local_chat.range.value"),
                x, y + 3, canEdit ? 0xC8C8D0 : 0x606068, false);
        rangeBox.setX(panelR - 12 - rangeBox.getWidth());
        rangeBox.setY(y);
        rangeBox.active = canEdit;
        rangeBox.setEditable(canEdit);
        y += ROW + 6;

        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".local_chat.admin");
        y = drawCheckRow(graphics, x, y, mouseX, mouseY, FeaturesConfig.localChatAdminBypass,
                () -> {
                    if (canEdit) {
                        FeaturesConfig.setLocalChatAdminBypass(!FeaturesConfig.localChatAdminBypass);
                        ModNetwork.sendToServer(LocalChatConfigPacket.current());
                    }
                },
                FeaturesConfig.localChatAdminBypass
                        ? "screen." + Features.MODID + ".local_chat.bypass.on"
                        : "screen." + Features.MODID + ".local_chat.bypass.off",
                canEdit);

        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".local_chat.bypass.hint"),
                x, y, 0x606068, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int drawSection(GuiGraphics g, int x, int y, String key) {
        g.drawString(font, Component.translatable(key), x, y, 0xC8C8D0, false);
        return y + 12;
    }

    private int drawCheckRow(GuiGraphics g, int x, int y, int mx, int my, boolean on,
                             Runnable action, String labelKey, boolean editable) {
        drawCheck(g, x, y + 3, on && editable);
        g.drawString(font, Component.translatable(labelKey), x + CHECK + 6, y + 3,
                editable ? (on ? 0xF0F0F0 : 0x9A9AA8) : 0x606068, false);
        if (editable) {
            hits.add(new Hit(x, y, Math.max(40, width - x - 24), ROW - 2, action));
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
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
