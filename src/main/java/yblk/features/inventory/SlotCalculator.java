package yblk.features.inventory;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import yblk.features.compat.CuriosInventoryBonus;
import yblk.features.config.FeaturesConfig;

public final class SlotCalculator {
    public static final int MAX_STORAGE = 45;
    public static final int CENTER_STORAGE = 27;
    public static final int SIDE_STORAGE = 18;
    public static final int SIDE_PER_COLUMN = 9;

    private SlotCalculator() {
    }

    public static boolean isActive() {
        return FeaturesConfig.inventoryRevampEnabled;
    }

    public static int unlockedStorage(Player player) {
        if (!isActive() || player == null) {
            return CENTER_STORAGE;
        }
        int total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(slot);
            total += FeaturesConfig.bonusFor(stack);
        }
        // Fashion / other mods: curios wardrobe (mask, body, backpack, …)
        total += CuriosInventoryBonus.bonusSlots(player);
        return Math.max(0, Math.min(MAX_STORAGE, total));
    }

    /** How many of the vanilla main-inventory slots (9..35) are usable. */
    public static int unlockedCenter(int unlocked) {
        return Math.min(CENTER_STORAGE, Math.max(0, unlocked));
    }

    /** Extra slots beyond 27, filled right wing first, then left. */
    public static int unlockedRight(int unlocked) {
        return Math.min(SIDE_PER_COLUMN, Math.max(0, unlocked - CENTER_STORAGE));
    }

    public static int unlockedLeft(int unlocked) {
        return Math.min(SIDE_PER_COLUMN, Math.max(0, unlocked - CENTER_STORAGE - SIDE_PER_COLUMN));
    }
}
