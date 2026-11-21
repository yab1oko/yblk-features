package yblk.features.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Names the player can pick instead of guessing a registry id. */
public final class RegistrySuggest {
    public record Row(String id, String label) {
    }

    private RegistrySuggest() {
    }

    public static List<Row> dimensions(String query) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        ids.add(Level.OVERWORLD.location());
        ids.add(Level.NETHER.location());
        ids.add(Level.END.location());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.getConnection() != null) {
            for (ResourceKey<Level> key : minecraft.getConnection().levels()) {
                ids.add(key.location());
            }
        }
        List<Row> rows = new ArrayList<>();
        for (ResourceLocation id : ids) {
            String name = dimensionName(id);
            rows.add(new Row(id.toString(), name + "  " + id));
        }
        rows.sort(Comparator.comparing(Row::label));
        return filter(rows, query, 8);
    }

    public static List<Row> entities(String query) {
        List<Row> rows = new ArrayList<>();
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (id == null) {
                continue;
            }
            rows.add(new Row(id.toString(), type.getDescription().getString() + "  " + id));
        }
        return filter(rows, query, 6);
    }

    public static List<Row> items(String query) {
        return items(query, 6);
    }

    public static List<Row> items(String query, int limit) {
        List<Row> rows = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) {
                continue;
            }
            rows.add(new Row(id.toString(), item.getDescription().getString() + "  " + id));
        }
        return filter(rows, query, limit);
    }

    public static List<Row> professions(String query) {
        List<Row> rows = new ArrayList<>();
        for (VillagerProfession profession : ForgeRegistries.VILLAGER_PROFESSIONS) {
            ResourceLocation id = ForgeRegistries.VILLAGER_PROFESSIONS.getKey(profession);
            if (id == null || "none".equals(id.getPath())) {
                continue;
            }
            String key = "minecraft".equals(id.getNamespace())
                    ? "entity.minecraft.villager." + id.getPath()
                    : "entity." + id.getNamespace() + ".villager." + id.getPath();
            String name = Component.translatable(key).getString();
            if (name.equals(key)) {
                name = id.getPath();
            }
            rows.add(new Row(id.toString(), name + "  " + id));
        }
        return filter(rows, query, 6);
    }

    public static String dimensionName(ResourceLocation id) {
        String key = Util.makeDescriptionId("dimension", id);
        String name = Component.translatable(key).getString();
        if (name.equals(key)) {
            return id.toString();
        }
        return name;
    }

    private static List<Row> filter(List<Row> rows, String query, int limit) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Row> out = new ArrayList<>();
        for (Row row : rows) {
            if (!q.isEmpty() && !row.label.toLowerCase(Locale.ROOT).contains(q) && !row.id.contains(q)) {
                continue;
            }
            out.add(row);
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }
}
