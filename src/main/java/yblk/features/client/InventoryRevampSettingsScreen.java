package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.Features;
import yblk.features.client.ui.BaseSettingsScreen;
import yblk.features.client.ui.TextField;
import yblk.features.client.ui.Checks;
import yblk.features.client.ui.ScrollBar;
import yblk.features.client.ui.SectionHeader;
import yblk.features.config.FeaturesConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InventoryRevampSettingsScreen extends BaseSettingsScreen {
        private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFF7EB6D9;
    private static final int CHECK = 9;
    private static final int ROW = 15;
    private static final int PANEL_W = 380;

    private final List<Hit> hits = new ArrayList<>();
    private EditBox itemBox;
    private String pickedBonus = "";
    private EditBox slotsBox;
    private int scroll;
    private int maxScroll;
    private int panelL;
    private int panelR;
    private int panelTop;
    private int panelBottom;
    private int contentTop;
    private int contentH;

    public InventoryRevampSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".inventory_revamp"), parent);
    }

    @Override
    protected void init() {
        clearWidgets();
        hits.clear();

        int btnH = 18;
        int bottomPad = 6;
        panelL = (width - Math.min(PANEL_W, width - 16)) / 2;
        panelR = width - panelL;
        panelTop = 22;
        panelBottom = height - bottomPad - btnH - 6;
        contentTop = panelTop + 8;

        itemBox = new TextField(font, panelL + 12, 0, 180, 16, Component.empty());
        itemBox.setMaxLength(128);
        itemBox.setHint(Component.translatable("screen." + Features.MODID + ".inventory_revamp.item_hint"));
        if (!pickedBonus.isEmpty()) {
            itemBox.setValue(pickedBonus);
        }
        addRenderableWidget(itemBox);

        slotsBox = new TextField(font, panelL + 12, 0, 36, 16, Component.empty());
        slotsBox.setMaxLength(3);
        slotsBox.setHint(Component.literal("slots"));
        slotsBox.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        addRenderableWidget(slotsBox);

        contentH = measureContentHeight();
        maxScroll = Math.max(0, contentH - Math.max(1, panelBottom - contentTop - 4));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        syncBoxes();

        addRenderableWidget(darkButton(width / 2 - 45, height - bottomPad - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    private int measureContentHeight() {
        int y = 0;
        y += 12; // hint
        y += 12 + ROW; // options + keep items
        y += 8 + 12 + 12; // bonuses section + format hint
        y += 18; // add row
        y += FeaturesConfig.armorBonuses.size() * ROW;
        y += 8 + 16; // reset
        y += 8;
        return y;
    }

    private void syncBoxes() {
        // Must match render(): hint + options section + keep row + gap + bonuses section + format hint
        int y = contentTop - scroll + 12 + 12 + ROW + 8 + 12 + 12;
        itemBox.setX(panelL + 34);
        itemBox.setY(y);
        itemBox.setWidth(Math.max(60, panelR - panelL - 34 - 36 - 8 - 54 - 12));
        slotsBox.setX(itemBox.getX() + itemBox.getWidth() + 4);
        slotsBox.setY(y);
        boolean visible = y + 16 > panelTop + 2 && y < panelBottom - 2;
        itemBox.visible = visible;
        itemBox.active = visible;
        slotsBox.visible = visible;
        slotsBox.active = visible;
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

        int x = panelL + 12;
        int y = contentTop - scroll;

        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".inventory_revamp.hint"),
                x, y, 0x808088, false);
        y += 12;

        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".inventory_revamp.options");
        y = drawCheckRow(graphics, x, y, mouseX, mouseY, FeaturesConfig.keepItemsWhenSlotsShrink,
                () -> FeaturesConfig.setKeepItemsWhenSlotsShrink(!FeaturesConfig.keepItemsWhenSlotsShrink),
                FeaturesConfig.keepItemsWhenSlotsShrink
                        ? "screen." + Features.MODID + ".inventory_revamp.keep_items.on"
                        : "screen." + Features.MODID + ".inventory_revamp.keep_items.off");

        y += 8;
        y = drawSection(graphics, x, y, "screen." + Features.MODID + ".inventory_revamp.bonuses");
        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".inventory_revamp.bonuses.hint"),
                x, y, 0x808088, false);
        y += 12;

        int addY = y;
        graphics.fill(x, addY, x + 18, addY + 16, 0xFF101014);
        ItemStack picked = pickedBonus();
        if (picked.isEmpty()) {
            graphics.drawString(font, "+", x + 5, addY + 4, 0xFF7EB6D9, false);
        } else {
            graphics.renderItem(picked, x + 1, addY);
        }
        hits.add(new Hit(x, addY, 18, 16, this::openBonusPick));
        y += 18;
        int addBtnX = panelR - 12 - 50;
        boolean addHover = mouseX >= addBtnX && mouseX < addBtnX + 50
                && mouseY >= addY && mouseY < addY + 16 && insidePanel(mouseX, mouseY);
        graphics.fill(addBtnX, addY, addBtnX + 50, addY + 16, addHover ? 0xFF2A3A48 : 0xFF1A1A22);
        graphics.drawCenteredString(font,
                Component.translatable("screen." + Features.MODID + ".inventory_revamp.add"),
                addBtnX + 25, addY + 4, addHover ? 0xF0F0F0 : 0xC0C0C8);
        tipKey(addBtnX, addY, 50, 16, "screen." + Features.MODID + ".inventory_revamp.add");
        hits.add(new Hit(addBtnX, addY, 50, 16, this::tryAddBonus));

        List<Map.Entry<ResourceLocation, Integer>> entries = new ArrayList<>(FeaturesConfig.armorBonuses.entrySet());
        entries.sort((a, b) -> a.getKey().toString().compareToIgnoreCase(b.getKey().toString()));
        for (Map.Entry<ResourceLocation, Integer> entry : entries) {
            y = drawBonusRow(graphics, x, y, mouseX, mouseY, entry.getKey().toString(), entry.getValue());
        }

        y += 8;
        int resetW = 120;
        int resetX = x;
        boolean resetHover = mouseX >= resetX && mouseX < resetX + resetW
                && mouseY >= y && mouseY < y + 16 && insidePanel(mouseX, mouseY);
        graphics.fill(resetX, y, resetX + resetW, y + 16, resetHover ? 0xFF3A2A20 : 0xFF1A1A22);
        graphics.drawCenteredString(font,
                Component.translatable("screen." + Features.MODID + ".inventory_revamp.reset"),
                resetX + resetW / 2, y + 4, resetHover ? 0xF0F0F0 : 0xC0A080);
        tipKey(resetX, y, resetW, 16, "screen." + Features.MODID + ".inventory_revamp.reset");
        hits.add(new Hit(resetX, y, resetW, 16, FeaturesConfig::resetArmorBonusesToDefault));

        // Keep add-row EditBoxes inside the scissor so they don't float out when scrolled.
        if (itemBox.visible) {
            itemBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (slotsBox.visible) {
            slotsBox.render(graphics, mouseX, mouseY, partialTick);
        }
        graphics.disableScissor();

        ScrollBar.draw(graphics, panelR, panelTop, panelBottom, scroll, maxScroll, ACCENT, v -> scroll = v);

        for (var child : children()) {
            if (child == itemBox || child == slotsBox) {
                continue;
            }
            if (child instanceof AbstractWidget widget) {
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private ItemStack pickedBonus() {
        ResourceLocation id = ResourceLocation.tryParse(itemBox.getValue().trim());
        if (id == null) {
            return ItemStack.EMPTY;
        }
        var item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void openBonusPick() {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new ItemPickScreen(this, 1, ACCENT, stack -> {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null) {
                pickedBonus = id.toString();
                itemBox.setValue(pickedBonus);
            }
        }));
    }

    private void tryAddBonus() {
        String raw = itemBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (raw.isEmpty()) {
            return;
        }
        if (!raw.contains(":")) {
            raw = "minecraft:" + raw;
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) {
            return;
        }
        int slots = 1;
        try {
            String slotsRaw = slotsBox.getValue().trim();
            if (!slotsRaw.isEmpty()) {
                slots = Integer.parseInt(slotsRaw);
            }
        } catch (NumberFormatException ignored) {
            return;
        }
        FeaturesConfig.setArmorBonus(id.toString(), slots);
        pickedBonus = "";
        itemBox.setValue("");
        slotsBox.setValue("");
    }

    private int drawBonusRow(GuiGraphics g, int x, int y, int mx, int my, String id, int slots) {
        String label = id + "  +" + slots;
        g.drawString(font, label, x, y + 3, 0xC8C8D0, false);
        int bx = panelR - 12 - 44;
        boolean hover = mx >= bx && mx < bx + 44 && my >= y && my < y + ROW - 2 && insidePanel(mx, my);
        g.fill(bx, y + 1, bx + 44, y + ROW - 3, hover ? 0xFF3A2020 : 0xFF1A1A22);
        g.drawCenteredString(font,
                Component.translatable("screen." + Features.MODID + ".inventory_revamp.remove"),
                bx + 22, y + 3, hover ? 0xF0F0F0 : 0xC07070);
        tipKey(bx, y, 44, ROW, "screen." + Features.MODID + ".inventory_revamp.remove");
        hits.add(new Hit(bx, y + 1, 44, ROW - 4, () -> FeaturesConfig.removeArmorBonus(id)));
        return y + ROW;
    }

    private int drawSection(GuiGraphics g, int x, int y, String key) {
        return SectionHeader.draw(g, font, x, y, Component.translatable(key));
    }

    private int drawCheckRow(GuiGraphics g, int x, int y, int mx, int my, boolean on,
                             Runnable action, String labelKey) {
        drawCheck(g, x, y + 3, on);
        g.drawString(font, Component.translatable(labelKey), x + CHECK + 6, y + 3, on ? 0xF0F0F0 : 0x9A9AA8, false);
        dirtyDot(g, x - 6, y + 4, on);
        tipKey(x, y, panelR - 12 - x, ROW, labelKey);
        hits.add(new Hit(x, y, panelR - 12 - x, ROW - 2, action));
        return y + ROW;
    }

    private static void drawCheck(GuiGraphics g, int x, int y, boolean on) {
        Checks.box(g, x, y, CHECK, on, ACCENT);
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
        syncBoxes();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((itemBox.isFocused() || slotsBox.isFocused()) && keyCode == 257 /* Enter */) {
            tryAddBonus();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
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
