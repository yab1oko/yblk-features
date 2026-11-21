package yblk.features.mobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** id, #tag, or @mod for entities and items. */
public final class TargetMatch {
    private TargetMatch() {
    }

    public static String entity(String raw) {
        return normalize(raw, true);
    }

    public static String item(String raw) {
        return normalize(raw, true);
    }

    public static boolean entity(EntityType<?> type, String target) {
        if (type == null || target == null || target.isBlank() || "*".equals(target)) {
            return "*".equals(target);
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null) {
            return false;
        }
        if (target.startsWith("#")) {
            ResourceLocation tag = location(target.substring(1));
            return tag != null && type.is(TagKey.create(Registries.ENTITY_TYPE, tag));
        }
        if (target.startsWith("@")) {
            return id.getNamespace().equals(namespace(target));
        }
        return id.toString().equals(target);
    }

    public static boolean item(ItemStack stack, String target) {
        if (stack == null || stack.isEmpty() || target == null || target.isBlank()) {
            return false;
        }
        if ("*".equals(target)) {
            return true;
        }
        Item item = stack.getItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null) {
            return false;
        }
        if (target.startsWith("#")) {
            ResourceLocation tag = location(target.substring(1));
            return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
        }
        if (target.startsWith("@")) {
            return id.getNamespace().equals(namespace(target));
        }
        return id.toString().equals(target);
    }

    public static boolean id(ResourceLocation id, String target) {
        if (id == null || target == null || target.isBlank()) {
            return false;
        }
        if (target.startsWith("@")) {
            return id.getNamespace().equals(namespace(target));
        }
        return id.toString().equals(target);
    }

    private static String normalize(String raw, boolean allowWild) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase();
        if (s.isEmpty()) {
            return "";
        }
        if ("*".equals(s) && allowWild) {
            return s;
        }
        if (s.startsWith("#") || s.startsWith("@")) {
            return s;
        }
        if (!s.contains(":")) {
            s = "minecraft:" + s;
        }
        return ResourceLocation.tryParse(s) == null ? "" : s;
    }

    private static ResourceLocation location(String raw) {
        String s = raw == null ? "" : raw.trim().toLowerCase();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (!s.contains(":")) {
            s = "minecraft:" + s;
        }
        return ResourceLocation.tryParse(s);
    }

    private static String namespace(String target) {
        String ns = target.substring(1);
        int colon = ns.indexOf(':');
        if (colon >= 0) {
            ns = ns.substring(0, colon);
        }
        return ns;
    }
}
