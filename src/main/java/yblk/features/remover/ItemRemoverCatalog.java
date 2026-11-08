package yblk.features.remover;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.config.FeaturesConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ItemRemoverCatalog {
    private ItemRemoverCatalog() {
    }

    public static List<Entry> allEntries() {
        List<Entry> list = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) {
                continue;
            }
            list.add(new Entry(id, item, stack));
        }
        list.sort(Comparator.comparing(e -> e.id.toString()));
        return list;
    }

    public static List<Entry> filtered(String query) {
        List<Entry> all = allEntries();
        if (query == null || query.isBlank()) {
            return all;
        }
        List<Entry> filtered = new ArrayList<>();
        for (Entry entry : all) {
            if (yblk.features.client.CatalogSearch.matchesItem(entry.stack, entry.id, query)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    public static void setBanned(ResourceLocation id, boolean banned) {
        Set<String> next = new HashSet<>(FeaturesConfig.bannedItemIds);
        if (banned) {
            next.add(id.toString());
        } else {
            next.remove(id.toString());
        }
        FeaturesConfig.setBannedItemIds(next);
    }

    public static void setAllBanned(List<Entry> entries, boolean banned) {
        Set<String> next = new HashSet<>(FeaturesConfig.bannedItemIds);
        for (Entry entry : entries) {
            if (banned) {
                next.add(entry.id.toString());
            } else {
                next.remove(entry.id.toString());
            }
        }
        FeaturesConfig.setBannedItemIds(next);
    }

    public record Entry(ResourceLocation id, Item item, ItemStack stack) {
        public boolean isBanned() {
            return FeaturesConfig.isItemBanned(id);
        }
    }
}
