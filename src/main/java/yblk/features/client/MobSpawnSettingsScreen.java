package yblk.features.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.spawn.MobSpawnCatalog;
import yblk.features.spawn.MobSpawnCatalog.Entry;
import yblk.features.spawn.MobSpawnCatalog.MobTab;

import java.util.ArrayList;
import java.util.List;

public class MobSpawnSettingsScreen extends Screen {
        private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFF4EC9B0;
    private static final int ROW_H = 22;
    private static final int LIST_PAD = 4;
    private static final int CHECK_SIZE = 10;
    private static final int CHECK_FRAME = 0xFFE8E8F0;
    private static final int CHECK_CROSS = 0xFFE05050;

    private final Screen parent;
    private MobTab tab = MobTab.ALL;
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
    private int previewX;
    private int previewY;
    private LivingEntity previewEntity;
    private EditBox searchBox;
    private final List<TabButton> tabButtons = new ArrayList<>();

    public MobSpawnSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".mob_spawn"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        tabButtons.clear();

        int contentTop = 36;
        int contentBottom = height - 36;
        listLeft = 16;
        listTop = contentTop + 52;
        listWidth = Math.min(260, width / 2 - 20);
        listHeight = contentBottom - listTop;
        previewLeft = listLeft + listWidth + 16;
        previewRight = width - 16;
        previewX = (previewLeft + previewRight) / 2;
        previewY = listTop + listHeight - 36;

        int tabX = listLeft;
        int tabY = contentTop;
        MobTab[] tabs = MobTab.values();
        int tabGap = 4;
        int[] ideal = {40, 88, 78, 88};
        int idealSum = 0;
        for (int w : ideal) {
            idealSum += w;
        }
        int tabsBudget = Math.max(80, listWidth - tabGap * (tabs.length - 1));
        int used = 0;
        for (int i = 0; i < tabs.length; i++) {
            int tw = i == tabs.length - 1
                    ? Math.max(28, tabsBudget - used)
                    : Math.max(28, Math.round(ideal[i] * (tabsBudget / (float) idealSum)));
            used += tw;
            TabButton btn = new TabButton(tabX, tabY, tw, 20, tabs[i]);
            tabButtons.add(btn);
            addRenderableWidget(btn);
            tabX += tw + tabGap;
        }

        searchBox = new EditBox(font, listLeft, contentTop + 26, listWidth, 18,
                Component.translatable("screen." + Features.MODID + ".mob_spawn.search"));
        searchBox.setMaxLength(64);
        searchBox.setValue(searchQuery);
        searchBox.setHint(Component.translatable("screen." + Features.MODID + ".mob_spawn.search"));
        searchBox.setTextColor(0xE0E0E8);
        searchBox.setTextColorUneditable(0x808088);
        searchBox.setResponder(value -> {
            searchQuery = value == null ? "" : value;
            selectedIndex = 0;
            scrollOffset = 0;
            refreshEntries();
        });
        addRenderableWidget(searchBox);

        // Align both button rows exactly to the preview panel width.
        int previewW = previewRight - previewLeft;
        int gap = 6;
        int btnW = (previewW - gap) / 2;
        int row1Y = contentTop;
        int row2Y = contentTop + 24;
        addRenderableWidget(new DarkButton(previewLeft, row1Y, btnW, 20,
                Component.translatable("screen." + Features.MODID + ".mob_spawn.enable_all"),
                () -> MobSpawnCatalog.setAllBlocked(entries, false)));
        addRenderableWidget(new DarkButton(previewLeft + btnW + gap, row1Y, btnW, 20,
                Component.translatable("screen." + Features.MODID + ".mob_spawn.disable_all"),
                () -> MobSpawnCatalog.setAllBlocked(entries, true)));
        addRenderableWidget(new ModeButton(previewLeft, row2Y, btnW, 20, false));
        addRenderableWidget(new ModeButton(previewLeft + btnW + gap, row2Y, btnW, 20, true));

        addRenderableWidget(new DarkButton(width / 2 - 50, height - 28, 100, 20,
                Component.translatable("gui.done"), this::onClose));

        refreshEntries();
    }

    private void refreshEntries() {
        List<Entry> base = MobSpawnCatalog.filtered(tab);
        if (searchQuery == null || searchQuery.isBlank()) {
            entries = base;
        } else {
            List<Entry> filtered = new ArrayList<>();
            for (Entry entry : base) {
                if (CatalogSearch.matchesEntity(entry.type(), entry.id(), searchQuery)) {
                    filtered.add(entry);
                }
            }
            entries = filtered;
        }
        selectedIndex = Mth.clamp(selectedIndex, 0, Math.max(0, entries.size() - 1));
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
        rebuildPreview();
    }

    private int maxScroll() {
        int visible = Math.max(1, listHeight / ROW_H);
        return Math.max(0, entries.size() - visible);
    }

    private void rebuildPreview() {
        if (previewEntity != null) {
            previewEntity.discard();
            previewEntity = null;
        }
        if (minecraft == null || minecraft.level == null || entries.isEmpty()) {
            return;
        }
        Entry entry = entries.get(selectedIndex);
        Entity created = entry.type().create(minecraft.level);
        if (created instanceof LivingEntity living) {
            previewEntity = living;
            previewEntity.yBodyRot = 180f;
            previewEntity.setYRot(180f);
            previewEntity.yHeadRot = 180f;
        }
    }

    private static void drawCheckBox(GuiGraphics graphics, int x, int y, boolean blocked) {
        // Hollow frame only
        graphics.fill(x, y, x + CHECK_SIZE, y + 1, CHECK_FRAME);
        graphics.fill(x, y + CHECK_SIZE - 1, x + CHECK_SIZE, y + CHECK_SIZE, CHECK_FRAME);
        graphics.fill(x, y, x + 1, y + CHECK_SIZE, CHECK_FRAME);
        graphics.fill(x + CHECK_SIZE - 1, y, x + CHECK_SIZE, y + CHECK_SIZE, CHECK_FRAME);
        if (blocked) {
            for (int i = 2; i < CHECK_SIZE - 2; i++) {
                graphics.fill(x + i, y + i, x + i + 1, y + i + 1, CHECK_CROSS);
                graphics.fill(x + CHECK_SIZE - 1 - i, y + i, x + CHECK_SIZE - i, y + i + 1, CHECK_CROSS);
            }
        }
    }

    private int previewScale(LivingEntity entity) {
        float maxDim = Math.max(entity.getBbWidth(), entity.getBbHeight());
        int panelW = previewRight - previewLeft - 24;
        int panelH = listHeight - 80;
        float target = Math.min(panelW, panelH) * 0.88f;
        int scale = Mth.clamp((int) (target / Math.max(0.35f, maxDim)), 40, 140);

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id != null) {
            String path = id.getPath();
            if ("ender_dragon".equals(path)) {
                scale = Math.round(scale * 0.32f);
            } else if ("elder_guardian".equals(path)) {
                scale = Math.round(scale * 0.48f);
            }
        }
        return Math.max(18, scale);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xF0F0F0);

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
                graphics.fill(listLeft, rowY, listLeft + listWidth, rowY + ROW_H, 0xFF2A3A38);
            } else if (hovered) {
                graphics.fill(listLeft, rowY, listLeft + listWidth, rowY + ROW_H, 0xFF1E1E26);
            }

            int checkX = listLeft + LIST_PAD;
            int checkY = rowY + (ROW_H - CHECK_SIZE) / 2;
            drawCheckBox(graphics, checkX, checkY, entry.isBlocked());

            Component name = entry.type().getDescription();
            graphics.drawString(font, name, checkX + CHECK_SIZE + 6, rowY + 7, selected ? 0xF0F0F0 : 0xB8B8C0, false);
        }

        graphics.fill(previewLeft, listTop - 2, previewRight, listTop + listHeight + 2, PANEL);
        FeaturesScreen.drawGradientBorder(graphics,
                previewLeft, listTop - 2,
                previewRight - previewLeft, listHeight + 4,
                ACCENT, 0xFF0A0A0E);

        if (!entries.isEmpty()) {
            Entry selected = entries.get(selectedIndex);
            graphics.drawCenteredString(font, selected.type().getDescription(),
                    (previewLeft + previewRight) / 2, listTop + 10, 0xF0F0F0);
            String statusKey = selected.isAllowed()
                    ? "screen." + Features.MODID + ".mob_spawn.allowed"
                    : "screen." + Features.MODID + ".mob_spawn.blocked";
            int statusColor = selected.isAllowed() ? 0xFF4EC9B0 : 0xFFC96060;
            graphics.drawCenteredString(font, Component.translatable(statusKey),
                    (previewLeft + previewRight) / 2, listTop + 24, statusColor);
        }

        // Widgets first, then entity on a high Z so the dark panel doesn't "eat" the model.
        super.render(graphics, mouseX, mouseY, partialTick);

        if (previewEntity != null && !entries.isEmpty()) {
            int scale = previewScale(previewEntity);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 200.0F);
            RenderSystem.enableDepthTest();
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics, previewX, previewY, scale, previewX - mouseX, previewY - 50 - mouseY, previewEntity);
            graphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= listLeft && mouseX < listLeft + listWidth
                && mouseY >= listTop && mouseY < listTop + listHeight) {
            int row = (int) ((mouseY - listTop) / ROW_H);
            int index = scrollOffset + row;
            if (index >= 0 && index < entries.size()) {
                int checkX = listLeft + LIST_PAD;
                int rowY = listTop + row * ROW_H;
                int checkY = rowY + (ROW_H - CHECK_SIZE) / 2;
                boolean onCheck = mouseX >= checkX - 2 && mouseX < checkX + CHECK_SIZE + 4
                        && mouseY >= checkY - 2 && mouseY < checkY + CHECK_SIZE + 4;
                if (onCheck) {
                    Entry entry = entries.get(index);
                    MobSpawnCatalog.setBlocked(entry.id(), !entry.isBlocked());
                    selectedIndex = index;
                    rebuildPreview();
                } else if (index != selectedIndex) {
                    selectedIndex = index;
                    rebuildPreview();
                } else {
                    Entry entry = entries.get(index);
                    MobSpawnCatalog.setBlocked(entry.id(), !entry.isBlocked());
                }
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
    public void removed() {
        if (previewEntity != null) {
            previewEntity.discard();
            previewEntity = null;
        }
        super.removed();
    }

    @Override
    public void onClose() {
        if (previewEntity != null) {
            previewEntity.discard();
            previewEntity = null;
        }
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // Skip vanilla gradient / dirt - keep the world sharp with a light dim only.
        FeatureSettingsBackdrop.render(graphics, width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private class TabButton extends AbstractWidget {
        private final MobTab tabValue;

        private TabButton(int x, int y, int w, int h, MobTab tabValue) {
            super(x, y, w, h, Component.translatable(tabValue.langKey()));
            this.tabValue = tabValue;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean active = tab == tabValue;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, active ? 0xFF2A4A44 : 0xFF1A1A22);
            if (active) {
                graphics.fill(getX(), getY() + height - 2, getX() + width, getY() + height, ACCENT);
            }
            graphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2,
                    active ? 0xF0F0F0 : 0x9A9AA8);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            tab = tabValue;
            selectedIndex = 0;
            scrollOffset = 0;
            refreshEntries();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private class ModeButton extends AbstractWidget {
        private final boolean allSources;

        private ModeButton(int x, int y, int w, int h, boolean allSources) {
            super(x, y, w, h, Component.translatable(allSources
                    ? "screen." + Features.MODID + ".mob_spawn.mode.all"
                    : "screen." + Features.MODID + ".mob_spawn.mode.natural"));
            this.allSources = allSources;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean active = FeaturesConfig.mobSpawnBlockAllSources == allSources;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, active ? 0xFF2A4A44 : 0xFF1A1A22);
            if (active) {
                graphics.fill(getX(), getY() + height - 2, getX() + width, getY() + height, ACCENT);
            }
            graphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2,
                    active ? 0xF0F0F0 : 0x9A9AA8);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            FeaturesConfig.setMobSpawnBlockAllSources(allSources);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
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
            graphics.fill(getX(), getY(), getX() + width, getY() + height, hover ? 0xFF2A3A38 : 0xFF1A1A22);
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
