package yblk.features.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yblk.features.inventory.SideInventorySlot;
import yblk.features.inventory.SideSlotInstaller;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void features$addSideSlots(Inventory inventory, boolean active, Player player, CallbackInfo ci) {
        SideSlotInstaller.ensureSideSlots((InventoryMenu) (Object) this, inventory, player);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void features$quickMoveSide(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        InventoryMenu self = (InventoryMenu) (Object) this;
        if (index < 0 || index >= self.slots.size()) {
            return;
        }
        Slot slot = self.slots.get(index);
        if (!(slot instanceof SideInventorySlot side) || !side.isUnlocked() || !slot.hasItem()) {
            return;
        }
        ItemStack original = slot.getItem().copy();
        if (!((AbstractContainerMenuAccessor) self).features$moveItemStackTo(slot.getItem(), 9, 45, false)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (slot.getItem().isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        cir.setReturnValue(original);
    }
}
