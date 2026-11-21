package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.Features;
import yblk.features.client.ui.BaseSettingsScreen;
import yblk.features.client.ui.ScrollBar;
import yblk.features.client.ui.TextField;
import yblk.features.config.FeaturesConfig;
import yblk.features.network.ItemWeightPacket;
import yblk.features.network.ModNetwork;
import yblk.features.restrict.Restrictions;
import yblk.features.weight.ItemWeights;

import java.util.ArrayList;
import java.util.List;

public class ItemWeightSettingsScreen extends BaseSettingsScreen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFFC4A15A;
    private static final int ROW = 16;

    private final List<Hit> hits = new ArrayList<>();
    private EditBox capacityBox;
    private EditBox slowBox;
    private EditBox sprintBox;
    private EditBox hungerBox;
    private EditBox strengthBox;
    private EditBox wetBox;
    private EditBox slotBox;
    private EditBox itemBox;
    private EditBox weightBox;
    private String pickedItem = "";
    private boolean canEdit;
    private int panelL;
    private int panelR;
    private int panelTop;
    private int panelBottom;
    private int listTop;
    private int listBottom;
    private int scroll;
    private int maxScroll;
    private List<RegistrySuggest.Row> found = List.of();

    public ItemWeightSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".item_weight"), parent);
    }

    @Override
    protected void init() {
        clearWidgets();
        canEdit = Restrictions.canEdit(minecraft != null ? minecraft.player : null);
        panelL = (width - Math.min(430, width - 16)) / 2;
        panelR = width - panelL;
        panelTop = 22;
        panelBottom = height - 32;
        int x = panelL + 92;
        int col = (panelR - panelL) / 2;
        capacityBox = num(x, panelTop + 28, 48, FeaturesConfig.itemWeightCapacity);
        slowBox = num(x, panelTop + 46, 48, FeaturesConfig.itemWeightSlow);
        sprintBox = num(x, panelTop + 64, 48, FeaturesConfig.itemWeightSprint);
        hungerBox = num(x, panelTop + 82, 48, FeaturesConfig.itemWeightHunger);
        strengthBox = num(x + col, panelTop + 28, 48, FeaturesConfig.itemWeightStrength);
        wetBox = num(x + col, panelTop + 46, 48, FeaturesConfig.itemWeightWet);
        slotBox = num(x + col, panelTop + 64, 48, FeaturesConfig.itemWeightSlotBonus);
        addRenderableWidget(capacityBox);
        addRenderableWidget(slowBox);
        addRenderableWidget(sprintBox);
        addRenderableWidget(hungerBox);
        addRenderableWidget(strengthBox);
        addRenderableWidget(wetBox);
        addRenderableWidget(slotBox);
        itemBox = field(panelL + 12, panelTop + 128, panelR - panelL - 90, pickedItem, "screen." + Features.MODID + ".item_weight.item");
        weightBox = field(panelR - 74, panelTop + 128, 50, "1", "screen." + Features.MODID + ".item_weight.amount");
        addRenderableWidget(itemBox);
        addRenderableWidget(weightBox);
        listTop = panelTop + 148;
        listBottom = panelBottom - 4;
        addRenderableWidget(darkButton(width / 2 - 96, height - 26, 90, 18,
                Component.translatable("screen." + Features.MODID + ".reset_section"), this::resetNumbers));
        addRenderableWidget(darkButton(width / 2 + 6, height - 26, 90, 18, Component.translatable("gui.done"), this::saveAndClose));
    }

    private EditBox num(int x, int y, int w, float value) {
        EditBox box = field(x, y, w, trim(value), "");
        box.setResponder(text -> markDirty());
        return box;
    }

    private EditBox field(int x, int y, int w, String value, String hint) {
        EditBox box = new TextField(font, x, y, Math.max(28, w), 14, Component.empty());
        box.setMaxLength(64);
        box.setValue(value);
        if (hint != null && !hint.isEmpty()) {
            box.setHint(Component.translatable(hint));
        }
        box.setEditable(canEdit);
        return box;
    }

    @Override
    protected void saveUnsaved() {
        commitNumbers();
    }

    private void saveAndClose() {
        commitNumbers();
        clearDirty();
        onClose();
    }

    private void commitNumbers() {
        if (canEdit) {
            FeaturesConfig.applyItemWeight(
                    FeaturesConfig.itemWeightEnabled,
                    parse(capacityBox.getValue(), FeaturesConfig.itemWeightCapacity),
                    parse(slowBox.getValue(), FeaturesConfig.itemWeightSlow),
                    parse(sprintBox.getValue(), FeaturesConfig.itemWeightSprint),
                    parse(hungerBox.getValue(), FeaturesConfig.itemWeightHunger),
                    parse(wetBox.getValue(), FeaturesConfig.itemWeightWet),
                    parse(slotBox.getValue(), FeaturesConfig.itemWeightSlotBonus),
                    parse(strengthBox.getValue(), FeaturesConfig.itemWeightStrength),
                    FeaturesConfig.itemWeightOverrides,
                    true);
            if (minecraft != null && minecraft.getConnection() != null) {
                ModNetwork.sendToServer(ItemWeightPacket.current());
            }
        }
    }

    private void resetNumbers() {
        capacityBox.setValue(trim(60f));
        slowBox.setValue(trim(0.45f));
        sprintBox.setValue(trim(0.70f));
        hungerBox.setValue(trim(0.85f));
        strengthBox.setValue(trim(0.5f));
        wetBox.setValue(trim(1.75f));
        slotBox.setValue(trim(4f));
        markDirty();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        found = List.of();
        int content = 14 + Math.max(1, FeaturesConfig.itemWeightOverrides.size()) * ROW;
        maxScroll = Math.max(0, content - Math.max(1, listBottom - listTop));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xF0F0F0);
        graphics.fill(panelL, panelTop, panelR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelTop, panelR - panelL, panelBottom - panelTop, ACCENT, 0xFF0A0A0E);
        int x = panelL + 12;
        int col = (panelR - panelL) / 2;
        label(graphics, "screen." + Features.MODID + ".item_weight.capacity", x, panelTop + 32);
        dirtyDot(graphics, x + 78, panelTop + 28, changed(capacityBox, 60f));
        dirtyDot(graphics, x + 78, panelTop + 46, changed(slowBox, 0.45f));
        dirtyDot(graphics, x + 78, panelTop + 64, changed(sprintBox, 0.70f));
        dirtyDot(graphics, x + 78, panelTop + 82, changed(hungerBox, 0.85f));
        dirtyDot(graphics, x + col + 78, panelTop + 28, changed(strengthBox, 0.5f));
        dirtyDot(graphics, x + col + 78, panelTop + 46, changed(wetBox, 1.75f));
        dirtyDot(graphics, x + col + 78, panelTop + 64, changed(slotBox, 4f));
        tip(x, panelTop + 28, 140, 16, Component.translatable("screen." + Features.MODID + ".item_weight.capacity.tip"));
        tip(x, panelTop + 46, 140, 16, Component.translatable("screen." + Features.MODID + ".item_weight.slow.tip"));
        tip(x, panelTop + 64, 140, 16, Component.translatable("screen." + Features.MODID + ".item_weight.sprint.tip"));
        tip(x, panelTop + 82, 140, 16, Component.translatable("screen." + Features.MODID + ".item_weight.hunger.tip"));
        tip(x + col, panelTop + 28, 140, 16, Component.translatable("screen." + Features.MODID + ".item_weight.strength.tip"));
        tip(x + col, panelTop + 46, 140, 16, Component.translatable("screen." + Features.MODID + ".item_weight.wet.tip"));
        tip(x + col, panelTop + 64, 140, 16, Component.translatable("screen." + Features.MODID + ".item_weight.slots.tip"));
        label(graphics, "screen." + Features.MODID + ".item_weight.slow", x, panelTop + 50);
        label(graphics, "screen." + Features.MODID + ".item_weight.sprint", x, panelTop + 68);
        label(graphics, "screen." + Features.MODID + ".item_weight.hunger", x, panelTop + 86);
        label(graphics, "screen." + Features.MODID + ".item_weight.strength", x + col, panelTop + 32);
        label(graphics, "screen." + Features.MODID + ".item_weight.wet", x + col, panelTop + 50);
        label(graphics, "screen." + Features.MODID + ".item_weight.slots", x + col, panelTop + 68);
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".item_weight.preset_armor"),
                x, panelTop + 104, 0x9A9AA4, false);
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".item_weight.preset_other"),
                x, panelTop + 114, 0x9A9AA4, false);

        graphics.enableScissor(panelL + 4, listTop, panelR - 4, listBottom);
        int y = listTop - scroll;
        int rowW = panelR - panelL - 28;
        if (y + 18 > listTop && y < listBottom) {
            graphics.fill(x, y, x + 18, y + 18, 0xFF101014);
            ItemStack icon = icon(itemBox.getValue());
            if (icon.isEmpty()) {
                graphics.drawString(font, "+", x + 5, y + 5, 0xFFC4A15A, false);
            } else {
                graphics.renderItem(icon, x + 1, y + 1);
            }
            if (canEdit) {
                hits.add(new Hit(x, y, 18, 18, this::openPick));
            }
            String chosen = icon.isEmpty()
                    ? Component.translatable("screen." + Features.MODID + ".item_weight.item").getString()
                    : icon.getHoverName().getString() + "  " + trim(ItemWeights.per(icon));
            graphics.drawString(font, font.plainSubstrByWidth(chosen, rowW - 24), x + 22, y + 5, 0xFFD0D0D8, false);
        }
        y += 22;
        if (FeaturesConfig.itemWeightOverrides.isEmpty() && y + ROW > listTop && y < listBottom) {
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".item_weight.empty"),
                    x, y + 3, 0x6A6A72, false);
        }
        for (String line : FeaturesConfig.itemWeightOverrides) {
            if (y + ROW > listTop && y < listBottom) {
                graphics.drawString(font, font.plainSubstrByWidth(line, rowW - 12), x, y + 4, 0xF0F0F0, false);
                graphics.drawString(font, "x", x + rowW - 8, y + 4, 0xC96060, false);
                if (canEdit && y >= listTop && y + ROW <= listBottom) {
                    String stored = line;
                    hits.add(new Hit(x, y, rowW, ROW, () -> remove(stored)));
                }
            }
            y += ROW;
        }
        graphics.disableScissor();
        ScrollBar.draw(graphics, panelR, listTop, listBottom, scroll, maxScroll, ACCENT, v -> scroll = v);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void openPick() {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new ItemPickScreen(this, 1, ACCENT, stack -> {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null) {
                pickedItem = id.toString();
                itemBox.setValue(pickedItem);
            }
        }));
    }

    private static ItemStack icon(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id == null ? "" : id.trim());
        if (parsed == null) {
            return ItemStack.EMPTY;
        }
        var item = ForgeRegistries.ITEMS.getValue(parsed);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(key).getString(), 76), x, y, 0xFFD0D0D8, false);
    }

    private static float weightOf(String id) {
        if (id == null || id.startsWith("#")) {
            return 0f;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            return 0f;
        }
        var item = ForgeRegistries.ITEMS.getValue(parsed);
        return item == null ? 0f : ItemWeights.per(new ItemStack(item));
    }

    private void remove(String stored) {
        List<String> next = new ArrayList<>();
        for (String line : FeaturesConfig.itemWeightOverrides) {
            if (!line.equals(stored)) {
                next.add(line);
            }
        }
        FeaturesConfig.setItemWeightOverrides(next);
        push();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && itemBox.isFocused()) {
            String stored = ItemWeights.line(itemBox.getValue().trim(), parse(weightBox.getValue(), 1f));
            if (stored != null && canEdit) {
                List<String> next = new ArrayList<>(FeaturesConfig.itemWeightOverrides);
                next.removeIf(line -> {
                    ItemWeights.Parsed parsed = ItemWeights.parse(line);
                    ItemWeights.Parsed added = ItemWeights.parse(stored);
                    return parsed != null && added != null && parsed.tag() == added.tag() && parsed.id().equals(added.id());
                });
                next.add(stored);
                FeaturesConfig.setItemWeightOverrides(next);
                itemBox.setValue("");
                pickedItem = "";
                push();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void push() {
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetwork.sendToServer(ItemWeightPacket.current());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (asking) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0) {
            for (Hit hit : hits) {
                if (mouseX >= hit.x && mouseX < hit.x + hit.w && mouseY >= hit.y && mouseY < hit.y + hit.h
                        && mouseY >= listTop && mouseY < listBottom) {
                    hit.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= listTop && mouseY < listBottom) {
            scroll = Mth.clamp(scroll - (int) (delta * ROW), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static boolean changed(EditBox box, float expected) {
        return box != null && Math.abs(parse(box.getValue(), expected) - expected) > 0.001f;
    }

    private static float parse(String raw, float fallback) {
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String trim(float value) {
        if (value == (int) value) {
            return Integer.toString((int) value);
        }
        return Float.toString(value);
    }

    @Override
    protected int buttonAccent() {
        return ACCENT;
    }

    @Override
    protected int buttonHover() {
        return 0xFF3A3020;
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }
}
