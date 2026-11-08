package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.names.HideNames;
import yblk.features.names.HideNamesMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HideNamesSettingsScreen extends Screen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFFE8E8F0;
    private static final int CHECK = 9;
    private static final int ROW = 15;
    private static final int CHOICE_H = 18;
    private static final int PANEL_W = 380;

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private EditBox addBox;
    private int scroll;
    private int maxScroll;
    private int panelL;
    private int panelR;
    private int panelTop;
    private int panelBottom;
    private int contentTop;
    private int contentH;
    private boolean canEditAdmin;

    public HideNamesSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".hide_names"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        hits.clear();
        canEditAdmin = HideNames.canEditAdmin(minecraft != null ? minecraft.player : null);

        int btnH = 18;
        int bottomPad = 6;
        panelL = (width - Math.min(PANEL_W, width - 16)) / 2;
        panelR = width - panelL;
        panelTop = 22;
        panelBottom = height - bottomPad - btnH - 6;
        contentTop = panelTop + 8;

        addBox = new EditBox(font, panelL + 12, 0, Math.max(40, panelR - panelL - 24 - 54), 16,
                Component.translatable("screen." + Features.MODID + ".hide_names.add_hint"));
        addBox.setMaxLength(128);
        addBox.setHint(Component.translatable("screen." + Features.MODID + ".hide_names.add_hint"));
        addBox.setEditable(canEditAdmin);
        addRenderableWidget(addBox);

        contentH = measureContentHeight();
        maxScroll = Math.max(0, contentH - Math.max(1, panelBottom - contentTop - 4));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        syncAddBox();

        addRenderableWidget(new DarkButton(width / 2 - 45, height - bottomPad - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    private boolean showMaskItems() {
        return FeaturesConfig.hideNamesMode == HideNamesMode.MASKS;
    }

    private boolean canSeeAdminLock() {
        return HideNames.canSeeAdminLock(minecraft != null ? minecraft.player : null);
    }

    private int measureContentHeight() {
        int y = 0;
        y += 12 + 12; // player section + hint
        y += 12 + CHOICE_H + 6; // mode label + row
        y += 12; // mode description
        if (canSeeAdminLock()) {
            y += 8 + 12; // admin section
            y += ROW; // admin lock
        }
        if (showMaskItems()) {
            y += 6 + 12; // items label
            y += 18; // add row
            y += FeaturesConfig.hideNamesItems.size() * ROW;
        }
        y += 8;
        return y;
    }

    private void syncAddBox() {
        if (!showMaskItems()) {
            addBox.visible = false;
            addBox.active = false;
            return;
        }
        int y = contentTop - scroll;
        y += 12 + 12 + 12 + CHOICE_H + 6 + 12;
        if (canSeeAdminLock()) {
            y += 8 + 12 + ROW;
        }
        y += 6 + 12;
        addBox.setX(panelL + 12);
        addBox.setY(y);
        addBox.setWidth(Math.max(40, panelR - panelL - 24 - 54));
        boolean visible = y + 16 > panelTop + 2 && y < panelBottom - 2;
        addBox.visible = visible && canEditAdmin;
        addBox.active = visible && canEditAdmin;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        contentH = measureContentHeight();
        maxScroll = Math.max(0, contentH - Math.max(1, panelBottom - contentTop - 4));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        syncAddBox();

        this.renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xF0F0F0);

        graphics.fill(panelL, panelTop, panelR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelTop, panelR - panelL,
                panelBottom - panelTop, ACCENT, 0xFF0A0A0E);

        graphics.enableScissor(panelL + 2, panelTop + 2, panelR - 2, panelBottom - 2);

        int x = panelL + 12;
        int right = panelR - 12;
        int y = contentTop - scroll;

        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".hide_names.player");
        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".hide_names.player.hint"),
                x, y, 0x808088, false);
        y += 12;

        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".hide_names.mode");
        y = drawModeRow(graphics, x, right, y, mouseX, mouseY);
        graphics.drawString(font,
                Component.translatable(FeaturesConfig.hideNamesMode.langKey() + ".desc"),
                x, y, 0x808088, false);
        y += 12;

        if (canSeeAdminLock()) {
            y += 8;
            y = drawSection(graphics, x, y, "screen." + Features.MODID + ".hide_names.admin");
            y = drawCheckRow(graphics, x, y, mouseX, mouseY, FeaturesConfig.hideNamesAdminLocked,
                    () -> {
                        FeaturesConfig.setHideNamesAdminLocked(!FeaturesConfig.hideNamesAdminLocked);
                        canEditAdmin = HideNames.canEditAdmin(minecraft.player);
                        addBox.setEditable(canEditAdmin);
                    },
                    FeaturesConfig.hideNamesAdminLocked
                            ? "screen." + Features.MODID + ".hide_names.admin.on"
                            : "screen." + Features.MODID + ".hide_names.admin.off",
                    true);
        }

        if (showMaskItems()) {
            y += 6;
            y = drawSection(graphics, x, y, "screen." + Features.MODID + ".hide_names.items");
            int addY = y;
            y += 18;

            if (canEditAdmin) {
                int addBtnX = panelR - 12 - 50;
                boolean hover = mouseX >= addBtnX && mouseX < addBtnX + 50
                        && mouseY >= addY && mouseY < addY + 16 && insidePanel(mouseX, mouseY);
                graphics.fill(addBtnX, addY, addBtnX + 50, addY + 16, hover ? 0xFF2A3A48 : 0xFF1A1A22);
                graphics.drawCenteredString(font,
                        Component.translatable("screen." + Features.MODID + ".hide_names.add"),
                        addBtnX + 25, addY + 4, hover ? 0xF0F0F0 : 0xC0C0C8);
                hits.add(new Hit(addBtnX, addY, 50, 16, this::tryAddItem));
            }

            List<String> items = new ArrayList<>(FeaturesConfig.hideNamesItems);
            items.sort(String::compareToIgnoreCase);
            for (String id : items) {
                y = drawItemRow(graphics, x, y, mouseX, mouseY, id);
            }
        }

        if (addBox != null && addBox.visible) {
            addBox.render(graphics, mouseX, mouseY, partialTick);
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackH = panelBottom - panelTop - 8;
            int thumbH = Math.max(12, trackH * trackH / (trackH + maxScroll));
            int thumbY = panelTop + 4 + (trackH - thumbH) * scroll / maxScroll;
            graphics.fill(panelR - 5, panelTop + 4, panelR - 3, panelBottom - 4, 0xFF2A2A32);
            graphics.fill(panelR - 5, thumbY, panelR - 3, thumbY + thumbH, 0xFFC0C0C8);
        }

        for (var child : children()) {
            if (child == addBox) {
                continue;
            }
            if (child instanceof AbstractWidget widget) {
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private int drawModeRow(GuiGraphics g, int left, int right, int y, int mx, int my) {
        HideNamesMode[] values = HideNamesMode.values();
        int gap = 3;
        int w = Math.max(36, ((right - left) - gap * (values.length - 1)) / values.length);
        int x = left;
        for (HideNamesMode mode : values) {
            boolean active = FeaturesConfig.hideNamesMode == mode;
            boolean hover = insidePanel(mx, my)
                    && mx >= x && mx < x + w && my >= y && my < y + CHOICE_H;
            g.fill(x, y, x + w, y + CHOICE_H, active ? 0xFF2A3A48 : (hover ? 0xFF2A2A32 : 0xFF1A1A22));
            if (active) {
                g.fill(x, y + CHOICE_H - 2, x + w, y + CHOICE_H, ACCENT);
            }
            g.drawCenteredString(font, Component.translatable(mode.langKey()),
                    x + w / 2, y + (CHOICE_H - 8) / 2, active ? 0xF0F0F0 : 0x9A9AA8);
            hits.add(new Hit(x, y, w, CHOICE_H, () -> FeaturesConfig.setHideNamesMode(mode)));
            x += w + gap;
        }
        return y + CHOICE_H + 6;
    }

    private void tryAddItem() {
        if (!canEditAdmin || addBox == null) {
            return;
        }
        String raw = addBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (raw.isEmpty()) {
            return;
        }
        if (!raw.contains(":")) {
            raw = "minecraft:" + raw;
        }
        ResourceLocation loc = ResourceLocation.tryParse(raw);
        if (loc == null || !ForgeRegistries.ITEMS.containsKey(loc)) {
            return;
        }
        FeaturesConfig.addHideNamesItem(loc.toString());
        addBox.setValue("");
    }

    private int drawItemRow(GuiGraphics g, int x, int y, int mx, int my, String id) {
        g.drawString(font, id, x, y + 3, 0xC8C8D0, false);
        if (canEditAdmin) {
            int bx = panelR - 12 - 44;
            boolean hover = mx >= bx && mx < bx + 44 && my >= y && my < y + ROW - 2 && insidePanel(mx, my);
            g.fill(bx, y + 1, bx + 44, y + ROW - 3, hover ? 0xFF3A2020 : 0xFF1A1A22);
            g.drawCenteredString(font,
                    Component.translatable("screen." + Features.MODID + ".hide_names.remove"),
                    bx + 22, y + 3, hover ? 0xF0F0F0 : 0xC07070);
            hits.add(new Hit(bx, y + 1, 44, ROW - 4, () -> FeaturesConfig.removeHideNamesItem(id)));
        }
        return y + ROW;
    }

    private int drawSection(GuiGraphics g, int x, int y, String key) {
        g.drawString(font, Component.translatable(key), x, y, 0xC8C8D0, false);
        return y + 12;
    }

    private int drawCheckRow(GuiGraphics g, int x, int y, int mx, int my, boolean on,
                             Runnable action, String labelKey, boolean interactive) {
        int color = interactive ? (on ? 0xF0F0F0 : 0x9A9AA8) : 0x606068;
        drawCheck(g, x, y + 3, on);
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
            g.fill(x + 2, y + 2, x + CHECK - 2, y + CHECK - 2, 0xFFE8E8F0);
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
        if (maxScroll <= 0 || !insidePanel(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        scroll = Mth.clamp(scroll - (int) Math.signum(delta) * 16, 0, maxScroll);
        syncAddBox();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (canEditAdmin && addBox != null && addBox.isFocused() && keyCode == 257 /* Enter */) {
            tryAddItem();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
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
            graphics.fill(getX(), getY(), getX() + width, getY() + height, hover ? 0xFF2A2A32 : 0xFF1A1A22);
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
