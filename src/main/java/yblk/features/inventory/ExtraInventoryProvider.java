package yblk.features.inventory;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExtraInventoryProvider implements ICapabilitySerializable<CompoundTag> {
    private final ExtraInventory inventory = new ExtraInventory();
    private final LazyOptional<ExtraInventory> optional = LazyOptional.of(() -> inventory);

    public ExtraInventory getInventory() {
        return inventory;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ExtraInventoryCapability.EXTRA_INVENTORY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return inventory.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        inventory.deserializeNBT(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
