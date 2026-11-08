package yblk.features.wetness;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WetnessProvider implements ICapabilitySerializable<CompoundTag> {
    private final Wetness wetness = new Wetness();
    private final LazyOptional<Wetness> optional = LazyOptional.of(() -> wetness);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == WetnessCapability.WETNESS) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return wetness.save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        wetness.load(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
