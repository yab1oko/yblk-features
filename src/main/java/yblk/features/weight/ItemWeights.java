package yblk.features.weight;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.config.FeaturesConfig;
import yblk.features.inventory.ExtraInventory;
import yblk.features.inventory.ExtraInventoryCapability;
import yblk.features.inventory.SlotCalculator;
import yblk.features.wetness.WetnessCapability;

import java.util.Locale;

/** Per-item weight, inventory total, and how much the player can carry. */
public final class ItemWeights {
    private ItemWeights() {
    }

    public static float total(Player player) {
        if (player == null) {
            return 0f;
        }
        float sum = 0f;
        float wet = wetFactor(player);
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size(); i++) {
            sum += of(inventory.items.get(i));
        }
        sum += of(inventory.offhand.get(0));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) {
                continue;
            }
            sum += of(player.getItemBySlot(slot)) * wet;
        }
        ExtraInventory extra = ExtraInventoryCapability.get(player);
        if (extra != null) {
            for (int i = 0; i < ExtraInventory.SIZE; i++) {
                sum += of(extra.getStackInSlot(i));
            }
        }
        return sum;
    }

    public static float capacity(Player player) {
        float base = Math.max(1f, FeaturesConfig.itemWeightCapacity);
        if (player != null) {
            MobEffectInstance strength = player.getEffect(MobEffects.DAMAGE_BOOST);
            if (strength != null) {
                base *= 1f + Math.max(0f, FeaturesConfig.itemWeightStrength) * (strength.getAmplifier() + 1);
            }
        }
        if (!SlotCalculator.isActive() || player == null) {
            return base;
        }
        int extraSlots = Math.max(0, SlotCalculator.unlockedStorage(player) - SlotCalculator.CENTER_STORAGE);
        return base + extraSlots * Math.max(0f, FeaturesConfig.itemWeightSlotBonus);
    }

    /** 0 is full speed. 0.5 is half. Past a full load the player is pulled down toward a crawl. */
    public static double slowAmount(float ratio) {
        float slowAt = FeaturesConfig.itemWeightSlow;
        if (ratio <= slowAt) {
            return 0d;
        }
        if (ratio < 1f) {
            float span = Math.max(0.05f, 1f - slowAt);
            return (ratio - slowAt) / span * 0.55d;
        }
        return Math.min(0.93d, 0.55d + (ratio - 1d) * 0.28d);
    }

    public static float ratio(Player player) {
        return total(player) / capacity(player);
    }

    public static float of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0f;
        }
        return per(stack) * stack.getCount();
    }

    public static float per(ItemStack stack) {
        Float exact = override(stack, false);
        if (exact != null) {
            return exact;
        }
        Float tagged = override(stack, true);
        if (tagged != null) {
            return tagged;
        }
        return fallback(stack.getItem());
    }

    private static Float override(ItemStack stack, boolean tags) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        for (String line : FeaturesConfig.itemWeightOverrides) {
            Parsed parsed = parse(line);
            if (parsed == null) {
                continue;
            }
            if (!tags && !parsed.tag && id != null && id.toString().equals(parsed.id)) {
                return parsed.weight;
            }
            if (tags && parsed.tag) {
                ResourceLocation tagId = ResourceLocation.tryParse(parsed.id);
                if (tagId != null && stack.is(TagKey.create(Registries.ITEM, tagId))) {
                    return parsed.weight;
                }
            }
        }
        return null;
    }

    public static Parsed parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.trim().split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        boolean tag = parts[0].startsWith("#");
        String id = tag ? parts[0].substring(1) : parts[0];
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        if (ResourceLocation.tryParse(id) == null) {
            return null;
        }
        try {
            float weight = Float.parseFloat(parts[1]);
            if (weight < 0f) {
                return null;
            }
            return new Parsed(tag, id, weight, (tag ? "#" : "") + id + " " + trim(weight));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String line(String id, float weight) {
        Parsed parsed = parse(id + " " + weight);
        return parsed == null ? null : parsed.stored;
    }

    private static float fallback(Item item) {
        if (item instanceof ArmorItem armor) {
            var material = armor.getMaterial();
            if (material == ArmorMaterials.LEATHER) return 1.5f;
            if (material == ArmorMaterials.CHAIN) return 2.5f;
            if (material == ArmorMaterials.IRON || material == ArmorMaterials.GOLD) return 4f;
            if (material == ArmorMaterials.DIAMOND) return 5.5f;
            if (material == ArmorMaterials.NETHERITE) return 6.5f;
            if (material == ArmorMaterials.TURTLE) return 2f;
            return 3f;
        }
        if (item instanceof TieredItem tool) {
            Tier tier = tool.getTier();
            if (tier == Tiers.WOOD) return 1.2f;
            if (tier == Tiers.STONE) return 2.2f;
            if (tier == Tiers.IRON) return 3.2f;
            if (tier == Tiers.GOLD) return 3.4f;
            if (tier == Tiers.DIAMOND) return 4.2f;
            if (tier == Tiers.NETHERITE) return 5f;
            return 2f;
        }
        if (item instanceof BlockItem) {
            return 0.45f;
        }
        if (item.isEdible()) {
            return 0.5f;
        }
        return 0.15f;
    }

    private static float wetFactor(Player player) {
        if (!FeaturesConfig.wetnessEnabled) {
            return 1f;
        }
        float wet = WetnessCapability.get(player).get() / 100f;
        float extra = Math.max(1f, FeaturesConfig.itemWeightWet) - 1f;
        return 1f + wet * extra;
    }

    private static String trim(float value) {
        if (value == (int) value) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public record Parsed(boolean tag, String id, float weight, String stored) {
    }
}
