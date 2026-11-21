package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import yblk.features.Features;
import yblk.features.client.ui.BaseSettingsScreen;
import yblk.features.client.ui.ScrollBar;
import yblk.features.client.ui.SearchableEntryList;
import yblk.features.client.ui.TextField;
import yblk.features.remover.ItemRemoverCatalog;
import yblk.features.remover.ItemRemoverCatalog.Entry;

import java.util.List;
import java.util.function.Consumer;

/** One centered card: search, icon list, count, then hand the stack back. */
public class ItemPickScreen extends BaseSettingsScreen {
    private static final int PANEL = 0xFF16161C;
    private static final int ROW = 22;

    private final Consumer<ItemStack> onPick;
    private final int accent;
    private final SearchableEntryList<Entry> catalog = new SearchableEntryList<>();
    private List<Entry> entries = List.of();
    private String query = "";
    private int selected;
    private int scroll;
    private int count;
    private int panelL;
    private int panelT;
    private int panelW;
    private int panelH;
    private int listLeft;
    private int listTop;
    private int listWidth;
    private int listHeight;
    private int previewLeft;
    private int previewRight;
    private EditBox searchBox;
    private EditBox countBox;

    public ItemPickScreen(Screen parent, int count, int accent, Consumer<ItemStack> onPick) {
        super(Component.translatable("screen." + Features.MODID + ".item_pick"), parent);
        this.onPick = onPick;
        this.accent = accent;
        this.count = Math.max(1, count);
    }

    @Override
    protected void init() {
        clearWidgets();
        int bottomGap = 36;
        panelW = Math.min(460, width - 24);
        panelH = Math.min(250, Math.max(160, height - bottomGap - 12));
        panelL = (width - panelW) / 2;
        panelT = Math.max(8, (height - bottomGap - panelH) / 2);

        listWidth = Math.min(200, panelW / 2 - 12);
        listLeft = panelL + 8;
        listTop = panelT + 40;
        listHeight = panelH - 48;
        previewLeft = listLeft + listWidth + 10;
        previewRight = panelL + panelW - 8;

        searchBox = new TextField(font, listLeft, panelT + 20, listWidth, 16, Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setValue(query);
        searchBox.setHint(Component.translatable("screen." + Features.MODID + ".item_pick.search"));
        searchBox.setResponder(value -> {
            query = value == null ? "" : value;
            selected = 0;
            scroll = 0;
            refresh();
        });
        addRenderableWidget(searchBox);

        int countY = panelT + 108;
        countBox = new TextField(font, previewLeft + 8, countY, 42, 16, Component.empty());
        countBox.setMaxLength(4);
        countBox.setValue(Integer.toString(count));
        countBox.setResponder(value -> count = parseCount(value, count));
        addRenderableWidget(countBox);

        int buttonsY = countY + 24;
        int buttonW = Math.max(64, (previewRight - previewLeft - 6) / 2);
        addRenderableWidget(darkButton(previewLeft, buttonsY, buttonW, 18,
                Component.translatable("screen." + Features.MODID + ".item_pick.use"), this::confirm));
        addRenderableWidget(darkButton(previewLeft + buttonW + 6, buttonsY, buttonW, 18,
                Component.translatable("gui.cancel"), this::onClose));
        refresh();
    }

    private void refresh() {
        entries = ItemRemoverCatalog.filtered(query);
        selected = Mth.clamp(selected, 0, Math.max(0, entries.size() - 1));
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, entries.size() - Math.max(1, listHeight / ROW));
    }

    private void confirm() {
        if (entries.isEmpty() || onPick == null) {
            onClose();
            return;
        }
        count = parseCount(countBox.getValue(), count);
        ItemStack stack = entries.get(selected).stack().copy();
        stack.setCount(Mth.clamp(count, 1, stack.getMaxStackSize()));
        onPick.accept(stack);
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(panelL, panelT, panelL + panelW, panelT + panelH, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelT, panelW, panelH, accent, 0xFF0A0A0E);
        graphics.drawCenteredString(font, title, panelL + panelW / 2, panelT + 6, 0xF0F0F0);

        graphics.fill(listLeft, listTop, listLeft + listWidth, listTop + listHeight, 0xFF101014);
        catalog.left = listLeft;
        catalog.top = listTop;
        catalog.width = listWidth;
        catalog.height = listHeight;
        catalog.rowHeight = ROW;
        catalog.scroll = scroll;
        catalog.selectedIndex = selected;
        catalog.selectedColor = 0xFF2A2418;
        catalog.setEntries(entries);
        catalog.render(graphics, mouseX, mouseY, (row, g) -> {
            g.renderItem(row.entry().stack(), row.x() + 2, row.y() + 3);
            g.drawString(font, font.plainSubstrByWidth(row.entry().stack().getHoverName().getString(), listWidth - 24),
                    row.x() + 22, row.y() + 7, row.selected() ? 0xF0F0F0 : 0xC8C8D0, false);
        });
        ScrollBar.draw(graphics, listLeft + listWidth + 8, listTop, listTop + listHeight, scroll, maxScroll(), accent,
                v -> scroll = v);

        if (!entries.isEmpty()) {
            Entry entry = entries.get(selected);
            int cx = (previewLeft + previewRight) / 2;
            graphics.pose().pushPose();
            graphics.pose().translate(cx - 16, panelT + 28, 0);
            graphics.pose().scale(2f, 2f, 1f);
            graphics.renderItem(entry.stack(), 0, 0);
            graphics.pose().popPose();
            graphics.drawCenteredString(font, font.plainSubstrByWidth(entry.stack().getHoverName().getString(), previewRight - previewLeft),
                    cx, panelT + 68, 0xF0F0F0);
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".item_pick.count"),
                    previewLeft + 56, panelT + 112, 0xC8C8D0, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int index = catalog.indexAt(mouseX, mouseY);
            if (index >= 0) {
                if (index == selected) {
                    confirm();
                } else {
                    selected = index;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (catalog.mouseScrolled(mouseX, mouseY, delta)) {
            scroll = catalog.scroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static int parseCount(String raw, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return Math.max(1, fallback);
        }
    }

    @Override
    protected int buttonAccent() {
        return accent;
    }

    @Override
    protected int buttonHover() {
        return 0xFF2A2418;
    }
}
