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
import yblk.features.client.ui.BaseSettingsScreen;
import yblk.features.client.ui.TextField;
import yblk.features.client.ui.Checks;
import yblk.features.client.ui.ScrollBar;
import yblk.features.client.ui.SearchableEntryList;
import yblk.features.client.ui.ChoiceButton;
import yblk.features.client.ui.SectionHeader;
import yblk.features.config.FeaturesConfig;
import yblk.features.spawn.MobSpawnCatalog;
import yblk.features.spawn.MobSpawnCatalog.Entry;
import yblk.features.spawn.MobSpawnCatalog.MobTab;

import java.util.ArrayList;
import java.util.List;

public class MobSpawnSettingsScreen extends BaseSettingsScreen {
        private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFF4EC9B0;
    private static final int ROW_H = 22;
    private static final int LIST_PAD = 4;
    private static final int CHECK_SIZE = 10;
    private static final int CHECK_FRAME = 0xFFE8E8F0;
    private static final int CHECK_CROSS = 0xFFE05050;

    private MobTab tab = MobTab.ALL;
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
    private int previewX;
    private int previewY;
    private LivingEntity previewEntity;
    private EditBox searchBox;

    public MobSpawnSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".mob_spawn"), parent);
    }

    @Override
    protected void init() {
        clearWidgets();

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
            MobTab tabValue = tabs[i];
            addRenderableWidget(new ChoiceButton(font, tabX, tabY, tw, 20,
                    Component.translatable(tabValue.langKey()),
                    () -> tab == tabValue,
                    () -> {
                        tab = tabValue;
                        selectedIndex = 0;
                        scrollOffset = 0;
                        refreshEntries();
                    },
                    ACCENT, 0xFF2A4A44));
            tabX += tw + tabGap;
        }

        searchBox = new TextField(font, listLeft, contentTop + 26, listWidth, 18,
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
        addRenderableWidget(darkButton(previewLeft, row1Y, btnW, 20,
                Component.translatable("screen." + Features.MODID + ".mob_spawn.enable_all"),
                () -> MobSpawnCatalog.setAllBlocked(entries, false)));
        addRenderableWidget(darkButton(previewLeft + btnW + gap, row1Y, btnW, 20,
                Component.translatable("screen." + Features.MODID + ".mob_spawn.disable_all"),
                () -> MobSpawnCatalog.setAllBlocked(entries, true)));
        addRenderableWidget(new ChoiceButton(font, previewLeft, row2Y, btnW, 20,
                Component.translatable("screen." + Features.MODID + ".mob_spawn.mode.natural"),
                () -> !FeaturesConfig.mobSpawnBlockAllSources,
                () -> FeaturesConfig.setMobSpawnBlockAllSources(false),
                ACCENT, 0xFF2A4A44));
        addRenderableWidget(new ChoiceButton(font, previewLeft + btnW + gap, row2Y, btnW, 20,
                Component.translatable("screen." + Features.MODID + ".mob_spawn.mode.all"),
                () -> FeaturesConfig.mobSpawnBlockAllSources,
                () -> FeaturesConfig.setMobSpawnBlockAllSources(true),
                ACCENT, 0xFF2A4A44));

        addRenderableWidget(darkButton(width / 2 - 50, height - 28, 100, 20,
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
        Checks.cross(graphics, x, y, CHECK_SIZE, blocked, CHECK_CROSS);
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

        catalog.left = listLeft;
        catalog.top = listTop;
        catalog.width = listWidth;
        catalog.height = listHeight;
        catalog.rowHeight = ROW_H;
        catalog.scroll = scrollOffset;
        catalog.selectedIndex = selectedIndex;
        catalog.selectedColor = 0xFF2A3A38;
        catalog.hoverColor = 0xFF1E1E26;
        catalog.setEntries(entries);
        catalog.render(graphics, mouseX, mouseY, (row, g) -> {
            Entry entry = row.entry();
            int checkX = row.x() + LIST_PAD;
            int checkY = row.y() + (ROW_H - CHECK_SIZE) / 2;
            drawCheckBox(g, checkX, checkY, entry.isBlocked());
            g.drawString(font, entry.type().getDescription(), checkX + CHECK_SIZE + 6, row.y() + 7,
                    row.selected() ? 0xF0F0F0 : 0xB8B8C0, false);
            tip(row.x(), row.y(), listWidth, ROW_H,
                    Component.translatable("screen." + Features.MODID + ".mob_spawn.row.tip"));
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
        super.onClose();
    }

    @Override
    protected int buttonAccent() {
        return ACCENT;
    }

    @Override
    protected int buttonHover() {
        return 0xFF2A3A38;
    }
}
