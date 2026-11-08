package yblk.features.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import yblk.features.config.FeaturesConfig;

/**
 * Extra storage slot (buffer 0..17). Right wing 0..8, left wing 9..17.
 */
public class SideInventorySlot extends SlotItemHandler {
    private final Player player;
    private final int bufferIndex;

    public SideInventorySlot(Player player, IItemHandler handler, int bufferIndex, int x, int y) {
        super(handler, bufferIndex, x, y);
        this.player = player;
        this.bufferIndex = bufferIndex;
    }

    public boolean isUnlocked() {
        if (!SlotCalculator.isActive()) {
            return false;
        }
        int unlocked = InventoryRevampEvents.clientUnlocked(player);
        int right = SlotCalculator.unlockedRight(unlocked);
        int left = SlotCalculator.unlockedLeft(unlocked);
        if (bufferIndex < SlotCalculator.SIDE_PER_COLUMN) {
            return bufferIndex < right;
        }
        return (bufferIndex - SlotCalculator.SIDE_PER_COLUMN) < left;
    }

    @Override
    public boolean isActive() {
        if (!SlotCalculator.isActive()) {
            return false;
        }
        if (isUnlocked()) {
            return true;
        }
        // Keep-mode: still render leftover stacks with a cross overlay.
        return FeaturesConfig.keepItemsWhenSlotsShrink && !getItem().isEmpty();
    }

    @Override
    public boolean mayPickup(Player player) {
        return isUnlocked() && super.mayPickup(player);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return isUnlocked() && super.mayPlace(stack);
    }

    public int getBufferIndex() {
        return bufferIndex;
    }

    public static boolean isSideSlot(Slot slot) {
        return slot instanceof SideInventorySlot;
    }
}
