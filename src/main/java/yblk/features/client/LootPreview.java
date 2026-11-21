package yblk.features.client;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithLootingCondition;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.mixin.LootBinomialAccessor;
import yblk.features.mixin.LootChanceAccessor;
import yblk.features.mixin.LootCompositeAccessor;
import yblk.features.mixin.LootConstantAccessor;
import yblk.features.mixin.LootCountAccessor;
import yblk.features.mixin.LootEntryAccessor;
import yblk.features.mixin.LootItemAccessor;
import yblk.features.mixin.LootLootingAccessor;
import yblk.features.mixin.LootPoolAccessor;
import yblk.features.mixin.LootRefAccessor;
import yblk.features.mixin.LootSingletonAccessor;
import yblk.features.mixin.LootTableAccessor;
import yblk.features.mixin.LootTagAccessor;
import yblk.features.mixin.LootUniformAccessor;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** What a mob's loot table can drop, with the chance of at least one and the count range. */
public final class LootPreview {
    public record Drop(String itemId, ItemStack icon, Component label, float chance, int min, int max) {
    }

    private LootPreview() {
    }

    public static List<Drop> of(EntityType<?> type) {
        if (type == null) {
            return List.of();
        }
        ResourceLocation tableId = type.getDefaultLootTable();
        if (tableId == null) {
            return List.of();
        }
        Map<String, Drop> merged = new LinkedHashMap<>();
        Set<ResourceLocation> seen = new HashSet<>();
        walk(table(tableId), 1f, 0, seen, merged);
        List<Drop> drops = new ArrayList<>(merged.values());
        drops.sort(Comparator.comparing(Drop::chance).reversed());
        return drops;
    }

    public static String percent(float chance) {
        float shown = Math.max(0f, Math.min(1f, chance)) * 100f;
        if (shown >= 99.5f) {
            return "100%";
        }
        if (shown >= 10f) {
            return Math.round(shown) + "%";
        }
        return String.format(java.util.Locale.ROOT, "%.1f%%", shown);
    }

    private static void walk(LootTable table, float scale, int depth, Set<ResourceLocation> seen, Map<String, Drop> out) {
        if (table == null || table == LootTable.EMPTY || depth > 4 || scale <= 0.0001f) {
            return;
        }
        ResourceLocation id = table.getLootTableId();
        if (id != null && !seen.add(id)) {
            return;
        }
        List<LootPool> pools = ((LootTableAccessor) table).features$pools();
        if (pools == null) {
            return;
        }
        for (LootPool pool : pools) {
            float rolls = Math.max(0f, average(pool.getRolls()));
            expandAll(((LootPoolAccessor) pool).features$entries(), scale, rolls, depth, seen, out);
        }
    }

    private static void expandAll(LootPoolEntryContainer[] entries, float scale, float rolls, int depth,
                                  Set<ResourceLocation> seen, Map<String, Drop> out) {
        if (entries == null || entries.length == 0 || rolls <= 0f) {
            return;
        }
        float[] once = oneRoll(entries);
        for (int i = 0; i < entries.length; i++) {
            expand(entries[i], atLeastOnce(scale * once[i], rolls), depth, seen, out);
        }
    }

    /** Chance this entry is the one picked on a single roll. Conditions gate the entry, they do not split its weight. */
    private static float[] oneRoll(LootPoolEntryContainer[] entries) {
        int n = entries.length;
        float[] gate = new float[n];
        int[] weights = new int[n];
        for (int i = 0; i < n; i++) {
            gate[i] = conditionScale(entries[i]);
            weights[i] = weight(entries[i]);
        }
        float[] pick = new float[n];
        for (int i = 0; i < n; i++) {
            float others = 0f;
            for (int j = 0; j < n; j++) {
                if (j != i) {
                    others += weights[j] * gate[j];
                }
            }
            float denom = weights[i] + others;
            pick[i] = denom <= 0f ? 0f : gate[i] * weights[i] / denom;
        }
        return pick;
    }

    private static float atLeastOnce(float once, float rolls) {
        if (once <= 0f || rolls <= 0f) {
            return 0f;
        }
        if (rolls <= 1.001f) {
            return Math.min(1f, once);
        }
        double miss = Math.pow(1.0 - Math.min(1.0, once), rolls);
        return (float) Math.min(1.0, 1.0 - miss);
    }

    private static void expand(LootPoolEntryContainer entry, float chance, int depth, Set<ResourceLocation> seen, Map<String, Drop> out) {
        if (entry instanceof LootItem lootItem) {
            Item item = ((LootItemAccessor) lootItem).features$item();
            if (item == null || item == Items.AIR) {
                return;
            }
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id != null) {
                int[] count = countOf(lootItem);
                add(out, id.toString(), new ItemStack(item), item.getDescription(), chance, count[0], count[1]);
            }
            return;
        }
        if (entry instanceof TagEntry tagEntry) {
            TagKey<Item> tag = ((LootTagAccessor) tagEntry).features$tag();
            if (tag == null) {
                return;
            }
            ItemStack icon = ItemStack.EMPTY;
            var holders = BuiltInRegistries.ITEM.getTag(tag);
            if (holders.isPresent()) {
                var first = holders.get().stream().findFirst();
                if (first.isPresent()) {
                    icon = new ItemStack(first.get().value());
                }
            }
            add(out, "#" + tag.location(), icon, Component.literal("#" + tag.location()), chance, 1, 1);
            return;
        }
        if (entry instanceof LootTableReference reference) {
            ResourceLocation name = ((LootRefAccessor) reference).features$name();
            if (name != null) {
                walk(table(name), chance, depth + 1, seen, out);
            }
            return;
        }
        if (entry instanceof CompositeEntryBase composite) {
            expandAll(((LootCompositeAccessor) composite).features$children(), chance, 1f, depth, seen, out);
        }
    }

    private static void add(Map<String, Drop> out, String itemId, ItemStack icon, Component label, float chance, int min, int max) {
        Drop existing = out.get(itemId);
        if (existing == null) {
            out.put(itemId, new Drop(itemId, icon, label, Math.min(1f, chance), min, max));
            return;
        }
        float next = Math.min(1f, existing.chance + chance);
        int lo = chance >= existing.chance ? min : existing.min;
        int hi = chance >= existing.chance ? max : existing.max;
        out.put(itemId, new Drop(itemId, existing.icon.isEmpty() ? icon : existing.icon, existing.label, next, lo, hi));
    }

    private static int[] countOf(LootItem item) {
        int[] range = new int[] {1, 1};
        LootItemFunction[] functions = ((LootSingletonAccessor) item).features$functions();
        if (functions == null) {
            return range;
        }
        for (LootItemFunction function : functions) {
            if (!(function instanceof SetItemCountFunction set)) {
                continue;
            }
            float[] span = span(((LootCountAccessor) (Object) set).features$value());
            int min = Math.max(0, Math.round(span[0]));
            int max = Math.max(min, Math.round(span[1]));
            if (((LootCountAccessor) (Object) set).features$add()) {
                min += 1;
                max += 1;
            }
            range[0] = min;
            range[1] = max;
        }
        return range;
    }

    private static float average(NumberProvider provider) {
        float[] span = span(provider);
        return (span[0] + span[1]) / 2f;
    }

    private static float[] span(NumberProvider provider) {
        if (provider instanceof ConstantValue) {
            float value = ((LootConstantAccessor) (Object) provider).features$value();
            return new float[] {value, value};
        }
        if (provider instanceof UniformGenerator) {
            float min = average(((LootUniformAccessor) (Object) provider).features$min());
            float max = average(((LootUniformAccessor) (Object) provider).features$max());
            return new float[] {Math.min(min, max), Math.max(min, max)};
        }
        if (provider instanceof BinomialDistributionGenerator) {
            float n = average(((LootBinomialAccessor) (Object) provider).features$n());
            float p = average(((LootBinomialAccessor) (Object) provider).features$p());
            float mean = Math.max(0f, n * p);
            return new float[] {0f, Math.max(mean, n)};
        }
        return new float[] {1f, 1f};
    }

    private static int weight(LootPoolEntryContainer entry) {
        if (entry instanceof LootPoolSingletonContainer) {
            return Math.max(1, ((LootSingletonAccessor) entry).features$weight());
        }
        return 1;
    }

    private static float conditionScale(LootPoolEntryContainer entry) {
        LootItemCondition[] conditions = ((LootEntryAccessor) entry).features$conditions();
        if (conditions == null) {
            return 1f;
        }
        float scale = 1f;
        for (LootItemCondition condition : conditions) {
            if (condition instanceof LootItemRandomChanceCondition) {
                scale *= ((LootChanceAccessor) condition).features$probability();
            } else if (condition instanceof LootItemRandomChanceWithLootingCondition) {
                scale *= ((LootLootingAccessor) (Object) condition).features$percent();
            }
        }
        return scale;
    }

    private static LootTable table(ResourceLocation id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() != null) {
            LootTable live = minecraft.getSingleplayerServer().getLootData().getLootTable(id);
            if (live != null && live != LootTable.EMPTY) {
                return live;
            }
        }
        if (minecraft.getResourceManager() == null) {
            return LootTable.EMPTY;
        }
        ResourceLocation file = new ResourceLocation(id.getNamespace(), "loot_tables/" + id.getPath() + ".json");
        var resource = minecraft.getResourceManager().getResource(file);
        if (resource.isEmpty()) {
            return LootTable.EMPTY;
        }
        try (Reader reader = resource.get().openAsReader()) {
            return LootDataType.TABLE.deserialize(id, JsonParser.parseReader(reader), minecraft.getResourceManager())
                    .orElse(LootTable.EMPTY);
        } catch (Exception ignored) {
            return LootTable.EMPTY;
        }
    }
}
