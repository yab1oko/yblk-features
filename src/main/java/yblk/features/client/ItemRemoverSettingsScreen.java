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
import yblk.features.client.ui.TextField;
import yblk.features.client.ui.Checks;
import yblk.features.client.ui.ScrollBar;
import yblk.features.client.ui.SearchableEntryList;
import yblk.features.client.ui.SectionHeader;
import yblk.features.network.ItemRemoverConfigPacket;
import yblk.features.network.ModNetwork;
import yblk.features.config.FeaturesConfig;
import yblk.features.config.FeaturesConfig.ItemBan;
import yblk.features.remover.ItemRemover;
import yblk.features.remover.ItemRemoverCatalog;
import yblk.features.remover.ItemRemoverCatalog.Entry;

import java.util.ArrayList;
import java.util.List;

public class ItemRemoverSettingsScreen extends BaseSettingsScreen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFFC96060;
    private static final int ROW_H = 22;
    private static final int LIST_PAD = 4;
    private static final int CHECK_SIZE = 10;
    private static final int CHECK_FRAME = 0xFFE8E8F0;
    private static final int CHECK_CROSS = 0xFFE05050;

    private String searchQuery = "";
    private final SearchableEntryList<Entry> catalog = new SearchableEntryList<>();
    private List<Entry> entries = List.of();
    private int selectedIndex;
    private int scrollOffset;
    private int listLeft;
    private int listTop;
    private int listWidth;
    private int listHeight;
    private int previewLeft;
    private int previewRight;
    private EditBox searchBox;
    private boolean canEdit;
    private int flagTop;
    private boolean flagsVisible;

    public ItemRemoverSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".item_remover"), parent);
    }

    @Override
    protected void init() {
        clearWidgets();
        canEdit = ItemRemover.canEdit(minecraft != null ? minecraft.player : null);

        int contentTop = 36;
        int contentBottom = height - 36;
        listLeft = 16;
        listTop = contentTop + 28;
        listWidth = Math.min(280, width / 2 - 20);
        listHeight = contentBottom - listTop;
        previewLeft = listLeft + listWidth + 16;
        previewRight = width - 16;

        searchBox = new TextField(font, listLeft, contentTop, listWidth, 18,
                Component.translatable("screen." + Features.MODID + ".item_remover.search"));
        searchBox.setMaxLength(64);
        searchBox.setValue(searchQuery);
        searchBox.setHint(Component.translatable("screen." + Features.MODID + ".item_remover.search"));
        searchBox.setResponder(value -> {
            searchQuery = value == null ? "" : value;
            selectedIndex = 0;
            scrollOffset = 0;
            refreshEntries();
        });
        addRenderableWidget(searchBox);

        int previewW = previewRight - previewLeft;
        int gap = 6;
        int btnW = (previewW - gap) / 2;
        if (canEdit) {
            addRenderableWidget(darkButton(previewLeft, contentTop, btnW, 20,
                    Component.translatable("screen." + Features.MODID + ".item_remover.allow_all"),
                    () -> {
                        ItemRemoverCatalog.setAllBanned(entries, false);
                        push();
                    }));
            addRenderableWidget(darkButton(previewLeft + btnW + gap, contentTop, btnW, 20,
                    Component.translatable("screen." + Features.MODID + ".item_remover.ban_all"),
                    () -> {
                        ItemRemoverCatalog.setAllBanned(entries, true);
                        push();
                    }));
        }

        addRenderableWidget(darkButton(width / 2 - 50, height - 28, 100, 20,
                Component.translatable("gui.done"), this::onClose));

        refreshEntries();
    }

    private void refreshEntries() {
        entries = ItemRemoverCatalog.filtered(searchQuery);
        selectedIndex = Mth.clamp(selectedIndex, 0, Math.max(0, entries.size() - 1));
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
    }

    private int maxScroll() {
        int visible = Math.max(1, listHeight / ROW_H);
        return Math.max(0, entries.size() - visible);
    }

    private void push() {
        if (canEdit) {
            ModNetwork.sendToServer(ItemRemoverConfigPacket.current());
        }
    }

    private static void drawCheckBox(GuiGraphics graphics, int x, int y, boolean banned) {
        Checks.cross(graphics, x, y, CHECK_SIZE, banned, CHECK_CROSS);
    }

    private void drawFlag(GuiGraphics graphics, int x, int y, boolean on, String key) {
        Checks.box(graphics, x, y, CHECK_SIZE, on, ACCENT);
        String text = Component.translatable(key).getString();
        int max = Math.max(8, previewRight - x - CHECK_SIZE - 16);
        graphics.drawString(font, font.plainSubstrByWidth(text, max), x + CHECK_SIZE + 6, y + 1,
                on ? 0xF0F0F0 : 0x9A9AA8, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xF0F0F0);

        if (!canEdit) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen." + Features.MODID + ".item_remover.locked"),
                    width / 2, 24, 0xE05050);
        }

        graphics.fill(listLeft - 2, listTop - 2, listLeft + listWidth + 2, listTop + listHeight + 2, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, listLeft - 2, listTop - 2, listWidth + 4, listHeight + 4, ACCENT, 0xFF0A0A0E);

        catalog.left = listLeft;
        catalog.top = listTop;
        catalog.width = listWidth;
        catalog.height = listHeight;
        catalog.rowHeight = ROW_H;
        catalog.scroll = scrollOffset;
        catalog.selectedIndex = selectedIndex;
        catalog.selectedColor = 0xFF3A2A2A;
        catalog.hoverColor = 0xFF1E1E26;
        catalog.setEntries(entries);
        catalog.render(graphics, mouseX, mouseY, (row, g) -> {
            Entry entry = row.entry();
            int checkX = row.x() + LIST_PAD;
            int checkY = row.y() + (ROW_H - CHECK_SIZE) / 2;
            drawCheckBox(g, checkX, checkY, entry.isBanned());
            int iconX = checkX + CHECK_SIZE + 4;
            g.renderItem(entry.stack(), iconX, row.y() + 3);
            g.drawString(font, entry.stack().getHoverName(), iconX + 18, row.y() + 7,
                    row.selected() ? 0xF0F0F0 : 0xB8B8C0, false);
            tip(row.x(), row.y(), listWidth, ROW_H,
                    Component.translatable("screen." + Features.MODID + ".item_remover.row.tip"));
        });
        ScrollBar.draw(graphics, listLeft + listWidth + 8, listTop, listTop + listHeight, scrollOffset, maxScroll(), ACCENT,
                v -> scrollOffset = v);

        graphics.fill(previewLeft, listTop - 2, previewRight, listTop + listHeight + 2, PANEL);
        FeaturesScreen.drawGradientBorder(graphics,
                previewLeft, listTop - 2,
                previewRight - previewLeft, listHeight + 4,
                ACCENT, 0xFF0A0A0E);

        if (!entries.isEmpty()) {
            Entry selected = entries.get(selectedIndex);
            int cx = (previewLeft + previewRight) / 2;
            graphics.pose().pushPose();
            graphics.pose().translate(cx - 16, listTop + 36, 0);
            graphics.pose().scale(2f, 2f, 1f);
            graphics.renderItem(selected.stack(), 0, 0);
            graphics.pose().popPose();

            graphics.drawCenteredString(font, selected.stack().getHoverName(), cx, listTop + 80, 0xF0F0F0);
            graphics.drawCenteredString(font, selected.id().toString(), cx, listTop + 94, 0x808088);
            String statusKey = selected.isBanned()
                    ? "screen." + Features.MODID + ".item_remover.banned"
                    : "screen." + Features.MODID + ".item_remover.allowed";
            int statusColor = selected.isBanned() ? 0xFFC96060 : 0xFF4EC9B0;
            graphics.drawCenteredString(font, Component.translatable(statusKey), cx, listTop + 110, statusColor);
            flagsVisible = selected.isBanned();
            flagTop = listTop + 128;
            if (flagsVisible) {
                ItemBan ban = FeaturesConfig.itemBan(selected.id());
                if (ban != null) {
                    drawFlag(graphics, previewLeft + 12, flagTop, ban.inventory,
                            "screen." + Features.MODID + ".item_remover.inventory");
                    drawFlag(graphics, previewLeft + 12, flagTop + 16, ban.recipes,
                            "screen." + Features.MODID + ".item_remover.recipes");
                    drawFlag(graphics, previewLeft + 12, flagTop + 32, ban.loot,
                            "screen." + Features.MODID + ".item_remover.loot");
                    tipKey(previewLeft + 12, flagTop, previewRight - previewLeft - 24, 14,
                            "screen." + Features.MODID + ".item_remover.inventory");
                    tipKey(previewLeft + 12, flagTop + 16, previewRight - previewLeft - 24, 14,
                            "screen." + Features.MODID + ".item_remover.recipes");
                    tipKey(previewLeft + 12, flagTop + 32, previewRight - previewLeft - 24, 14,
                            "screen." + Features.MODID + ".item_remover.loot");
                }
            }
        } else {
            flagsVisible = false;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && canEdit && flagsVisible && !entries.isEmpty()
                && mouseX >= previewLeft && mouseX < previewRight
                && mouseY >= flagTop && mouseY < flagTop + 48) {
            int row = (int) ((mouseY - flagTop) / 16);
            Entry selected = entries.get(selectedIndex);
            ItemBan ban = FeaturesConfig.itemBan(selected.id());
            if (ban != null && row >= 0 && row < 3) {
                String flag = switch (row) {
                    case 0 -> "inventory";
                    case 1 -> "recipes";
                    default -> "loot";
                };
                boolean next = switch (row) {
                    case 0 -> !ban.inventory;
                    case 1 -> !ban.recipes;
                    default -> !ban.loot;
                };
                FeaturesConfig.setItemFlag(selected.id(), flag, next);
                push();
            }
            return true;
        }
        if (button == 0 && canEdit && mouseX >= listLeft && mouseX < listLeft + listWidth
                && mouseY >= listTop && mouseY < listTop + listHeight) {
            int row = (int) ((mouseY - listTop) / ROW_H);
            int index = scrollOffset + row;
            if (index >= 0 && index < entries.size()) {
                Entry entry = entries.get(index);
                int checkX = listLeft + LIST_PAD;
                int rowY = listTop + row * ROW_H;
                int checkY = rowY + (ROW_H - CHECK_SIZE) / 2;
                boolean onCheck = mouseX >= checkX - 2 && mouseX < checkX + CHECK_SIZE + 4
                        && mouseY >= checkY - 2 && mouseY < checkY + CHECK_SIZE + 4;
                if (onCheck || index == selectedIndex) {
                    ItemRemoverCatalog.setBanned(entry.id(), !entry.isBanned());
                    push();
                }
                selectedIndex = index;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listLeft && mouseX < listLeft + listWidth
                && mouseY >= listTop && mouseY < listTop + listHeight) {
            scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(delta), 0, maxScroll());
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
        return 0xFF3A2A2A;
    }
}
