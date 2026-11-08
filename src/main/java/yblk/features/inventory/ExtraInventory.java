package yblk.features.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class ExtraInventory {
    public static final int SIZE = SlotCalculator.SIDE_STORAGE;

    private final ItemStackHandler handler = new ItemStackHandler(SIZE);
    private int unlockedCache;

    public ItemStackHandler getHandler() {
        return handler;
    }

    public int getUnlockedCache() {
        return unlockedCache;
    }

    public void setUnlockedCache(int unlockedCache) {
        this.unlockedCache = unlockedCache;
    }

    public ItemStack getStackInSlot(int slot) {
        return handler.getStackInSlot(slot);
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        handler.setStackInSlot(slot, stack);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = handler.serializeNBT();
        tag.putInt("Unlocked", unlockedCache);
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        handler.deserializeNBT(nbt);
        unlockedCache = nbt.getInt("Unlocked");
    }
}
