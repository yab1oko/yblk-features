package yblk.features.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import yblk.features.config.FeaturesConfig;

public final class LockedSlots {
    private LockedSlots() {
    }

    public static boolean isCenterLocked(Slot slot) {
        if (SideInventorySlot.isSideSlot(slot)) {
            return false;
        }
        if (!(slot.container instanceof Inventory inventory)) {
            return false;
        }
        if (!SlotCalculator.isActive()) {
            return false;
        }
        int invSlot = slot.getContainerSlot();
        if (invSlot < 9 || invSlot >= 36) {
            return false;
        }
        int unlocked = InventoryRevampEvents.clientUnlocked(inventory.player);
        return (invSlot - 9) >= SlotCalculator.unlockedCenter(unlocked);
    }

    public static boolean isSideLocked(SideInventorySlot slot) {
        if (!SlotCalculator.isActive()) {
            return true;
        }
        return !slot.isUnlocked();
    }

    /** Locked for interaction, but still drawn (keep-mode item left behind). */
    public static boolean showLockedItem(Slot slot) {
        if (!FeaturesConfig.keepItemsWhenSlotsShrink || slot.getItem().isEmpty()) {
            return false;
        }
        if (slot instanceof SideInventorySlot side) {
            return isSideLocked(side);
        }
        return isCenterLocked(slot);
    }

    public static boolean isInteractionLocked(Slot slot) {
        if (slot instanceof SideInventorySlot side) {
            return isSideLocked(side);
        }
        return isCenterLocked(slot);
    }

    public static int unlockedFor(Player player) {
        return InventoryRevampEvents.clientUnlocked(player);
    }
}
