package yblk.features.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.Features;
import yblk.features.client.ui.BaseSettingsScreen;
import yblk.features.client.ui.Checks;
import yblk.features.client.ui.ScrollBar;
import yblk.features.client.ui.TextField;
import yblk.features.config.FeaturesConfig;
import yblk.features.mobs.MobDropRules;
import yblk.features.network.MobDropsPacket;
import yblk.features.network.ModNetwork;
import yblk.features.restrict.Restrictions;
import yblk.features.spawn.MobSpawnCatalog;

import java.util.ArrayList;
import java.util.List;

/** Pick a mob, see its loot, turn drops off, or add new ones. */
public class MobDropsSettingsScreen extends BaseSettingsScreen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFFC4A15A;
    private static final int ROW = 20;
    private static final int CHECK = 9;

    private final List<Hit> hits = new ArrayList<>();
    private List<MobSpawnCatalog.Entry> entries = List.of();
    private List<LootPreview.Drop> drops = List.of();
    private List<RegistrySuggest.Row> itemHits = List.of();
    private String search = "";
    private String itemQuery = "";
    private int selected;
    private int mobScroll;
    private int lootScroll;
    private int listLeft;
    private int listTop;
    private int listWidth;
    private int listHeight;
    private int previewLeft;
    private int previewRight;
    private int lootTop;
    private EditBox searchBox;
    private EditBox itemBox;
    private EditBox chanceBox;
    private EditBox minBox;
    private EditBox maxBox;
    private LivingEntity preview;
    private boolean canEdit;
    private String selectedItem = "";

    public MobDropsSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".mob_drops"), parent);
    }

    @Override
    protected void init() {
        clearWidgets();
        canEdit = Restrictions.canEdit(minecraft != null ? minecraft.player : null);
        int contentTop = 32;
        int contentBottom = height - 32;
        listLeft = 12;
        listTop = contentTop + 22;
        listWidth = Math.min(168, width / 2 - 24);
        listHeight = contentBottom - listTop;
        previewLeft = listLeft + listWidth + 12;
        previewRight = width - 12;
        lootTop = listTop + 102;

        searchBox = new TextField(font, listLeft, contentTop, listWidth, 16, Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setValue(search);
        searchBox.setHint(Component.translatable("screen." + Features.MODID + ".mob_drops.search"));
        searchBox.setResponder(value -> {
            search = value == null ? "" : value;
            selected = 0;
            mobScroll = 0;
            refresh();
        });
        addRenderableWidget(searchBox);

        int inner = previewRight - previewLeft - 8;
        int editorY = listTop + 78;
        chanceBox = smallBox(previewLeft + 4, editorY, 42, "100", "screen." + Features.MODID + ".mob_drops.chance");
        minBox = smallBox(previewLeft + 50, editorY, 28, "1", "screen." + Features.MODID + ".mob_drops.min");
        maxBox = smallBox(previewLeft + 82, editorY, 28, "1", "screen." + Features.MODID + ".mob_drops.max");
        addRenderableWidget(chanceBox);
        addRenderableWidget(minBox);
        addRenderableWidget(maxBox);
        addRenderableWidget(darkButton(previewLeft + 114, editorY, 46, 14,
                Component.translatable("screen." + Features.MODID + ".mob_drops.apply"), this::applyEditor));
        itemBox = new TextField(font, previewLeft + 28, listTop + listHeight - 16, Math.max(40, inner - 28), 14, Component.empty());
        itemBox.setMaxLength(80);
        itemBox.setValue(itemQuery);
        itemBox.setHint(Component.translatable("screen." + Features.MODID + ".mob_drops.add_item"));
        itemBox.setResponder(value -> itemQuery = value == null ? "" : value);
        itemBox.setEditable(false);
        itemBox.visible = false;

        addRenderableWidget(darkButton(width / 2 - 45, height - 26, 90, 18,
                Component.translatable("gui.done"), this::onClose));
        refresh();
    }

    private EditBox smallBox(int x, int y, int w, String value, String hint) {
        EditBox box = new TextField(font, x, y, w, 14, Component.empty());
        box.setMaxLength(8);
        box.setValue(value);
        box.setHint(Component.translatable(hint));
        box.setEditable(canEdit);
        return box;
    }

    private void refresh() {
        List<MobSpawnCatalog.Entry> all = MobSpawnCatalog.filtered(MobSpawnCatalog.MobTab.ALL);
        if (search.isBlank()) {
            entries = all;
        } else {
            List<MobSpawnCatalog.Entry> found = new ArrayList<>();
            for (MobSpawnCatalog.Entry entry : all) {
                if (CatalogSearch.matchesEntity(entry.type(), entry.id(), search)) {
                    found.add(entry);
                }
            }
            entries = found;
        }
        selected = Mth.clamp(selected, 0, Math.max(0, entries.size() - 1));
        mobScroll = Mth.clamp(mobScroll, 0, mobMax());
        rebuildPreview();
        drops = entries.isEmpty() ? List.of() : LootPreview.of(entries.get(selected).type());
    }

    private int mobMax() {
        return Math.max(0, entries.size() - Math.max(1, listHeight / ROW));
    }

    private void rebuildPreview() {
        if (preview != null) {
            preview.discard();
            preview = null;
        }
        if (minecraft == null || minecraft.level == null || entries.isEmpty()) {
            return;
        }
        Entity created = entries.get(selected).type().create(minecraft.level);
        if (created instanceof LivingEntity living) {
            preview = living;
            preview.yBodyRot = 210f;
            preview.setYRot(210f);
            preview.yHeadRot = 210f;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xF0F0F0);
        graphics.fill(listLeft - 2, listTop - 2, listLeft + listWidth + 2, listTop + listHeight + 2, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, listLeft - 2, listTop - 2, listWidth + 4, listHeight + 4, ACCENT, 0xFF0A0A0E);

        int visible = Math.max(1, listHeight / ROW);
        for (int row = 0; row < visible; row++) {
            int index = mobScroll + row;
            if (index >= entries.size()) {
                break;
            }
            MobSpawnCatalog.Entry entry = entries.get(index);
            int y = listTop + row * ROW;
            if (index == selected) {
                graphics.fill(listLeft, y, listLeft + listWidth, y + ROW, 0xFF3A3020);
            }
            graphics.drawString(font, font.plainSubstrByWidth(entry.type().getDescription().getString(), listWidth - 8),
                    listLeft + 4, y + 6, index == selected ? 0xF0F0F0 : 0xC8C8D0, false);
        }
        int mobMax = Math.max(0, entries.size() - visible);
        ScrollBar.draw(graphics, listLeft + listWidth + 8, listTop, listTop + listHeight, mobScroll, mobMax, ACCENT,
                v -> mobScroll = v);

        graphics.fill(previewLeft, listTop - 2, previewRight, listTop + listHeight + 2, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, previewLeft, listTop - 2, previewRight - previewLeft, listHeight + 4, ACCENT, 0xFF0A0A0E);

        if (!entries.isEmpty()) {
            MobSpawnCatalog.Entry entry = entries.get(selected);
            graphics.drawCenteredString(font, entry.type().getDescription(),
                    (previewLeft + previewRight) / 2, listTop + 4, 0xF0F0F0);
            Checks.box(graphics, previewLeft + 6, listTop + 18, CHECK, FeaturesConfig.mobDropsClearVanilla, ACCENT);
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".mob_drops.clear"),
                    previewLeft + 18, listTop + 18, 0xFFD0D0D8, false);
            tipKey(previewLeft + 4, listTop + 16, previewRight - previewLeft - 8, 14,
                    "screen." + Features.MODID + ".mob_drops.clear");
            if (canEdit) {
                hits.add(new Hit(previewLeft + 4, listTop + 16, previewRight - previewLeft - 8, 14, this::toggleClear));
            }
        }

        renderLoot(graphics, mouseX, mouseY);
        if (!entries.isEmpty()) {
            int sx = previewLeft + 6;
            int sy = listTop + listHeight - 20;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF101014);
            graphics.drawString(font, "+", sx + 5, sy + 5, 0xFFC4A15A, false);
            if (canEdit) {
                String entityId = entries.get(selected).id().toString();
                hits.add(new Hit(sx, sy, 18, 18, () -> openAdd(entityId)));
            }
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".mob_drops.add_item"),
                    sx + 22, sy + 5, 0xFF9A9AA4, false);
            tipKey(sx, sy, 140, 18, "screen." + Features.MODID + ".mob_drops.add_item");
        }
        super.render(graphics, mouseX, mouseY, partialTick);

        if (preview != null && !entries.isEmpty()) {
            int px = previewRight - 36;
            int py = listTop + 74;
            graphics.enableScissor(previewRight - 78, listTop + 2, previewRight - 4, listTop + 76);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 200.0F);
            RenderSystem.enableDepthTest();
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics, px, py, 28, px - mouseX, py - 36 - mouseY, preview);
            graphics.pose().popPose();
            graphics.disableScissor();
        }
    }

    private void renderLoot(GuiGraphics graphics, int mouseX, int mouseY) {
        if (entries.isEmpty()) {
            return;
        }
        String entityId = entries.get(selected).id().toString();
        List<Row> rows = rowsFor(entityId);
        itemHits = List.of();
        int itemRows = 0;
        int y0 = lootTop;
        int y1 = listTop + listHeight - 20;
        int view = Math.max(1, y1 - y0);
        int content = rows.size() * ROW + itemRows * 14;
        int max = Math.max(0, content - view);
        lootScroll = Mth.clamp(lootScroll, 0, max);
        graphics.enableScissor(previewLeft + 2, y0, previewRight - 2, y1);
        int y = y0 - lootScroll;
        int x = previewLeft + 6;
        int w = previewRight - previewLeft - 16;
        if (rows.isEmpty() && itemRows == 0) {
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".mob_drops.no_loot"),
                    x, y + 4, 0x6A6A72, false);
        }
        for (Row row : rows) {
            if (y + ROW > y0 && y < y1) {
                boolean picked = row.itemId.equals(selectedItem);
                if (picked) {
                    graphics.fill(x, y, x + w, y + ROW - 1, 0xFF2A2418);
                }
                if (!row.icon.isEmpty()) {
                    graphics.renderItem(row.icon, x, y + 2);
                }
                int nameW = Math.max(24, w - 132);
                graphics.drawString(font, font.plainSubstrByWidth(row.label, nameW), x + 20, y + 6,
                        row.on ? 0xF0F0F0 : 0x808088, false);
                graphics.drawString(font, LootPreview.percent(row.chance), x + w - 108, y + 6, 0xFFC4A15A, false);
                graphics.drawString(font, countText(row.min, row.max), x + w - 64, y + 6, 0xFFD0D0D8, false);
                Checks.box(graphics, x + w - 24, y + 6, CHECK, row.on, ACCENT);
                if (!row.vanilla || row.custom) {
                    graphics.drawString(font, "x", x + w - 8, y + 6, 0xC96060, false);
                }
                if (canEdit && y >= y0 && y + ROW <= y1) {
                    Row chosen = row;
                    hits.add(new Hit(x, y, w - 32, ROW, () -> select(chosen)));
                    hits.add(new Hit(x + w - 28, y, 16, ROW, () -> toggleDrop(entityId, chosen)));
                    if (!chosen.vanilla || chosen.custom) {
                        hits.add(new Hit(x + w - 12, y, 12, ROW, () -> deleteDrop(entityId, chosen)));
                    }
                }
            }
            y += ROW;
        }
        for (int i = 0; i < itemRows; i++) {
            RegistrySuggest.Row suggest = itemHits.get(i);
            if (y + 14 > y0 && y < y1) {
                boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 14;
                if (hover) {
                    graphics.fill(x, y, x + w, y + 13, 0xFF2A2418);
                }
                graphics.drawString(font, font.plainSubstrByWidth("+ " + suggest.label(), w - 4), x + 2, y + 3, 0xFFD8C090, false);
                if (canEdit && y >= y0 && y + 14 <= y1) {
                    String id = suggest.id();
                    hits.add(new Hit(x, y, w, 14, () -> addDrop(entityId, id)));
                }
            }
            y += 14;
        }
        graphics.disableScissor();
        ScrollBar.draw(graphics, previewRight, y0, y1, lootScroll, max, ACCENT, v -> lootScroll = v);
    }

    private List<Row> rowsFor(String entityId) {
        List<Row> rows = new ArrayList<>();
        for (LootPreview.Drop drop : drops) {
            MobDropRules.Rule add = find(entityId, drop.itemId(), MobDropRules.Kind.ADD);
            boolean on = add != null ? add.enabled() : !removed(entityId, drop.itemId());
            float chance = add != null ? add.chance() : drop.chance();
            int min = add != null ? add.min() : drop.min();
            int max = add != null ? add.max() : drop.max();
            rows.add(new Row(drop.itemId(), drop.icon(), drop.label().getString(), chance, min, max, on, true, add != null));
        }
        for (MobDropRules.Rule rule : MobDropRules.compile(FeaturesConfig.mobDropRules)) {
            if (rule.kind() != MobDropRules.Kind.ADD || !entityId.equals(rule.target())) {
                continue;
            }
            boolean listed = false;
            for (Row row : rows) {
                if (row.itemId.equals(rule.item())) {
                    listed = true;
                    break;
                }
            }
            if (listed) {
                continue;
            }
            ItemStack icon = stack(rule.item());
            String name = icon.isEmpty() ? rule.item() : icon.getHoverName().getString();
            rows.add(new Row(rule.item(), icon, name, rule.chance(), rule.min(), rule.max(), rule.enabled(), false, true));
        }
        return rows;
    }

    private void select(Row row) {
        selectedItem = row.itemId;
        chanceBox.setValue(percentValue(row.chance));
        minBox.setValue(Integer.toString(row.min));
        maxBox.setValue(Integer.toString(row.max));
    }

    private void applyEditor() {
        if (!canEdit || entries.isEmpty() || selectedItem.isEmpty()) {
            return;
        }
        String entityId = entries.get(selected).id().toString();
        float chance = parseChance(chanceBox.getValue());
        int min = parseCount(minBox.getValue(), 1);
        int max = parseCount(maxBox.getValue(), min);
        boolean vanilla = false;
        for (LootPreview.Drop drop : drops) {
            if (selectedItem.equals(drop.itemId())) {
                vanilla = true;
                break;
            }
        }
        MobDropRules.Rule add = MobDropRules.add(entityId, selectedItem, chance, min, max, 0, false, true);
        MobDropRules.Rule remove = vanilla ? MobDropRules.remove(entityId, selectedItem, false) : null;
        put(entityId, selectedItem, remove, add);
    }

    private void toggleDrop(String entityId, Row row) {
        if (row.vanilla) {
            if (row.on) {
                MobDropRules.Rule add = find(entityId, row.itemId, MobDropRules.Kind.ADD);
                MobDropRules.Rule kept = add == null ? null
                        : MobDropRules.add(entityId, row.itemId, add.chance(), add.min(), add.max(), add.looting(), add.playerOnly(), false);
                put(entityId, row.itemId, MobDropRules.remove(entityId, row.itemId, false), kept);
            } else {
                MobDropRules.Rule add = find(entityId, row.itemId, MobDropRules.Kind.ADD);
                if (add == null) {
                    put(entityId, row.itemId, null, null);
                } else {
                    MobDropRules.Rule kept = MobDropRules.add(entityId, row.itemId, add.chance(), add.min(), add.max(), add.looting(), add.playerOnly(), true);
                    put(entityId, row.itemId, MobDropRules.remove(entityId, row.itemId, false), kept);
                }
            }
            return;
        }
        MobDropRules.Rule add = find(entityId, row.itemId, MobDropRules.Kind.ADD);
        if (add == null) {
            return;
        }
        put(entityId, row.itemId, null, MobDropRules.add(entityId, row.itemId, add.chance(), add.min(), add.max(),
                add.looting(), add.playerOnly(), !add.enabled()));
    }

    private void deleteDrop(String entityId, Row row) {
        put(entityId, row.itemId, null, null);
        if (row.itemId.equals(selectedItem)) {
            selectedItem = "";
        }
    }

    private void put(String entityId, String itemId, MobDropRules.Rule remove, MobDropRules.Rule add) {
        List<String> next = new ArrayList<>();
        for (String line : FeaturesConfig.mobDropRules) {
            MobDropRules.Rule rule = MobDropRules.parse(line);
            if (rule != null && entityId.equals(rule.target()) && itemId.equals(rule.item())
                    && (rule.kind() == MobDropRules.Kind.ADD || rule.kind() == MobDropRules.Kind.REMOVE)) {
                continue;
            }
            next.add(line);
        }
        if (remove != null) {
            next.add(remove.stored());
        }
        if (add != null) {
            next.add(add.stored());
        }
        FeaturesConfig.setMobDropRules(next);
        push();
    }

    private MobDropRules.Rule find(String entityId, String itemId, MobDropRules.Kind kind) {
        for (MobDropRules.Rule rule : MobDropRules.compile(FeaturesConfig.mobDropRules)) {
            if (rule.kind() == kind && entityId.equals(rule.target()) && itemId.equals(rule.item())) {
                return rule;
            }
        }
        return null;
    }

    private void openAdd(String entityId) {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new ItemPickScreen(this, parseCount(maxBox.getValue(), 1), ACCENT, stack -> {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null) {
                return;
            }
            int amount = stack.getCount();
            minBox.setValue(Integer.toString(amount));
            maxBox.setValue(Integer.toString(amount));
            selectedItem = id.toString();
            addDrop(entityId, id.toString());
        }));
    }

    private void addDrop(String entityId, String itemId) {
        selectedItem = itemId;
        float chance = parseChance(chanceBox.getValue());
        int min = parseCount(minBox.getValue(), 1);
        int max = parseCount(maxBox.getValue(), min);
        boolean vanilla = false;
        for (LootPreview.Drop drop : drops) {
            if (itemId.equals(drop.itemId())) {
                vanilla = true;
                break;
            }
        }
        MobDropRules.Rule add = MobDropRules.add(entityId, itemId, chance, min, max, 0, false, true);
        if (add == null) {
            return;
        }
        put(entityId, add.item(), vanilla ? MobDropRules.remove(entityId, add.item(), false) : null, add);
        itemBox.setValue("");
        itemQuery = "";
        chanceBox.setValue(percentValue(chance));
        minBox.setValue(Integer.toString(min));
        maxBox.setValue(Integer.toString(max));
    }

    private void toggleClear() {
        FeaturesConfig.setMobDropsClearVanilla(!FeaturesConfig.mobDropsClearVanilla);
        push();
    }

    private boolean removed(String entityId, String itemId) {
        for (MobDropRules.Rule rule : MobDropRules.compile(FeaturesConfig.mobDropRules)) {
            if (rule.kind() == MobDropRules.Kind.REMOVE && entityId.equals(rule.target()) && itemId.equals(rule.item())) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack stack(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            return ItemStack.EMPTY;
        }
        var item = ForgeRegistries.ITEMS.getValue(parsed);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void push() {
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetwork.sendToServer(MobDropsPacket.current());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= listLeft && mouseX < listLeft + listWidth
                && mouseY >= listTop && mouseY < listTop + listHeight) {
            int index = mobScroll + (int) ((mouseY - listTop) / ROW);
            if (index >= 0 && index < entries.size()) {
                selected = index;
                lootScroll = 0;
                selectedItem = "";
                refresh();
                return true;
            }
        }
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= listTop && mouseY < listTop + listHeight) {
            mobScroll = Mth.clamp(mobScroll - (int) Math.signum(delta), 0, mobMax());
            return true;
        }
        if (mouseX >= previewLeft && mouseX < previewRight && mouseY >= lootTop && mouseY < listTop + listHeight) {
            lootScroll = Math.max(0, lootScroll - (int) Math.signum(delta) * ROW);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        discardPreview();
        super.removed();
    }

    @Override
    public void onClose() {
        discardPreview();
        super.onClose();
    }

    private void discardPreview() {
        if (preview != null) {
            preview.discard();
            preview = null;
        }
    }

    @Override
    protected int buttonAccent() {
        return ACCENT;
    }

    @Override
    protected int buttonHover() {
        return 0xFF3A3020;
    }

    private static String countText(int min, int max) {
        return min == max ? Integer.toString(min) : min + "-" + max;
    }

    private static String percentValue(float chance) {
        float shown = Math.max(0f, Math.min(1f, chance)) * 100f;
        if (Math.abs(shown - Math.round(shown)) < 0.05f) {
            return Integer.toString(Math.round(shown));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", shown);
    }

    private static float parseChance(String raw) {
        try {
            return Math.max(0f, Math.min(100f, Float.parseFloat(raw.trim()))) / 100f;
        } catch (NumberFormatException ignored) {
            return 1f;
        }
    }

    private static int parseCount(String raw, int fallback) {
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private record Row(String itemId, ItemStack icon, String label, float chance, int min, int max, boolean on,
                       boolean vanilla, boolean custom) {
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }
}
