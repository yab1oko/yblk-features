package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import yblk.features.Features;
import yblk.features.network.ItemRemoverConfigPacket;
import yblk.features.network.ModNetwork;
import yblk.features.remover.ItemRemover;
import yblk.features.remover.ItemRemoverCatalog;
import yblk.features.remover.ItemRemoverCatalog.Entry;

import java.util.ArrayList;
import java.util.List;

public class ItemRemoverSettingsScreen extends Screen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFFC96060;
    private static final int ROW_H = 22;
    private static final int LIST_PAD = 4;
    private static final int CHECK_SIZE = 10;
    private static final int CHECK_FRAME = 0xFFE8E8F0;
    private static final int CHECK_CROSS = 0xFFE05050;

    private final Screen parent;
    private String searchQuery = "";
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

    public ItemRemoverSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".item_remover"));
        this.parent = parent;
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

        searchBox = new EditBox(font, listLeft, contentTop, listWidth, 18,
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
            addRenderableWidget(new DarkButton(previewLeft, contentTop, btnW, 20,
                    Component.translatable("screen." + Features.MODID + ".item_remover.allow_all"),
                    () -> {
                        ItemRemoverCatalog.setAllBanned(entries, false);
                        push();
                    }));
            addRenderableWidget(new DarkButton(previewLeft + btnW + gap, contentTop, btnW, 20,
                    Component.translatable("screen." + Features.MODID + ".item_remover.ban_all"),
                    () -> {
                        ItemRemoverCatalog.setAllBanned(entries, true);
                        push();
                    }));
        }

        addRenderableWidget(new DarkButton(width / 2 - 50, height - 28, 100, 20,
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
        graphics.fill(x, y, x + CHECK_SIZE, y + 1, CHECK_FRAME);
        graphics.fill(x, y + CHECK_SIZE - 1, x + CHECK_SIZE, y + CHECK_SIZE, CHECK_FRAME);
        graphics.fill(x, y, x + 1, y + CHECK_SIZE, CHECK_FRAME);
        graphics.fill(x + CHECK_SIZE - 1, y, x + CHECK_SIZE, y + CHECK_SIZE, CHECK_FRAME);
        if (banned) {
            for (int i = 2; i < CHECK_SIZE - 2; i++) {
                graphics.fill(x + i, y + i, x + i + 1, y + i + 1, CHECK_CROSS);
                graphics.fill(x + CHECK_SIZE - 1 - i, y + i, x + CHECK_SIZE - i, y + i + 1, CHECK_CROSS);
            }
        }
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

        int visible = Math.max(1, listHeight / ROW_H);
        for (int i = 0; i < visible; i++) {
            int index = scrollOffset + i;
            if (index >= entries.size()) {
                break;
            }
            Entry entry = entries.get(index);
            int rowY = listTop + i * ROW_H;
            boolean selected = index == selectedIndex;
            boolean hovered = mouseX >= listLeft && mouseX < listLeft + listWidth
                    && mouseY >= rowY && mouseY < rowY + ROW_H;
            if (selected) {
                graphics.fill(listLeft, rowY, listLeft + listWidth, rowY + ROW_H, 0xFF3A2A2A);
            } else if (hovered) {
                graphics.fill(listLeft, rowY, listLeft + listWidth, rowY + ROW_H, 0xFF1E1E26);
            }

            int checkX = listLeft + LIST_PAD;
            int checkY = rowY + (ROW_H - CHECK_SIZE) / 2;
            drawCheckBox(graphics, checkX, checkY, entry.isBanned());

            int iconX = checkX + CHECK_SIZE + 4;
            graphics.renderItem(entry.stack(), iconX, rowY + 3);
            graphics.drawString(font, entry.stack().getHoverName(), iconX + 18, rowY + 7,
                    selected ? 0xF0F0F0 : 0xB8B8C0, false);
        }

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
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
            graphics.fill(getX(), getY(), getX() + width, getY() + height, hover ? 0xFF3A2A2A : 0xFF1A1A22);
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
