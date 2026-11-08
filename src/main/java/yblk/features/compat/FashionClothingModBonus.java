package yblk.features.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Soft Fashion hook: clothing NBT mods (e.g. pouch) add inventory-revamp slots
 * on top of the static item-id bonus map.
 * <p>
 * Reads {@code FashionMods.pouch} directly — no Fashion class/reflection dependency.
 */
public final class FashionClothingModBonus {
    private static final String TAG_ROOT = "FashionMods";
    private static final String TAG_POUCH = "pouch";

    private FashionClothingModBonus() {
    }

    public static int bonus(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ROOT)) {
            return 0;
        }
        CompoundTag mods = tag.getCompound(TAG_ROOT);
        return mods.getBoolean(TAG_POUCH) ? 1 : 0;
    }
}
