package yblk.features.client;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Shared search for item/mob catalogs.
 * <ul>
 *   <li>{@code #namespace:path} — tag membership</li>
 *   <li>{@code @namespace:path} — registry id (exact or contains after {@code @})</li>
 *   <li>plain text — display name or id contains</li>
 * </ul>
 */
public final class CatalogSearch {
    private CatalogSearch() {
    }

    public static boolean matchesItem(ItemStack stack, ResourceLocation id, String rawQuery) {
        String q = normalize(rawQuery);
        if (q.isEmpty()) {
            return true;
        }
        if (q.startsWith("#")) {
            return itemHasTag(stack.getItem(), q.substring(1));
        }
        if (q.startsWith("@")) {
            return idMatches(id, q.substring(1));
        }
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        String idStr = id.toString().toLowerCase(Locale.ROOT);
        return name.contains(q) || idStr.contains(q);
    }

    public static boolean matchesEntity(EntityType<?> type, ResourceLocation id, String rawQuery) {
        String q = normalize(rawQuery);
        if (q.isEmpty()) {
            return true;
        }
        if (q.startsWith("#")) {
            return entityHasTag(type, q.substring(1));
        }
        if (q.startsWith("@")) {
            return idMatches(id, q.substring(1));
        }
        String name = type.getDescription().getString().toLowerCase(Locale.ROOT);
        String idStr = id.toString().toLowerCase(Locale.ROOT);
        return name.contains(q) || idStr.contains(q);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean idMatches(ResourceLocation id, String query) {
        if (query.isEmpty() || id == null) {
            return false;
        }
        String full = id.toString().toLowerCase(Locale.ROOT);
        if (full.equals(query) || full.contains(query)) {
            return true;
        }
        // Allow "@minecraft" to list a whole namespace.
        return !query.contains(":") && id.getNamespace().equals(query);
    }

    private static boolean itemHasTag(Item item, String tagQuery) {
        ResourceLocation tagId = parseTagId(tagQuery);
        if (tagId == null) {
            return false;
        }
        TagKey<Item> key = TagKey.create(Registries.ITEM, tagId);
        return new ItemStack(item).is(key);
    }

    private static boolean entityHasTag(EntityType<?> type, String tagQuery) {
        ResourceLocation tagId = parseTagId(tagQuery);
        if (tagId == null) {
            return false;
        }
        TagKey<EntityType<?>> key = TagKey.create(Registries.ENTITY_TYPE, tagId);
        return type.is(key);
    }

    private static ResourceLocation parseTagId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (!s.contains(":")) {
            s = "minecraft:" + s;
        }
        return ResourceLocation.tryParse(s);
    }
}
