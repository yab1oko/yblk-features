package yblk.features.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @Invoker("addSlot")
    Slot features$addSlot(Slot slot);

    @Invoker("moveItemStackTo")
    boolean features$moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverse);
}
