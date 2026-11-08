package yblk.features.mixin;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yblk.features.config.FeaturesConfig;
import yblk.features.inventory.LockedSlots;
import yblk.features.inventory.SideInventorySlot;

@Mixin(Slot.class)
public abstract class SlotMixin {
    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void features$lockStorageSlots(CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (SideInventorySlot.isSideSlot(self)) {
            return;
        }
        if (!LockedSlots.isCenterLocked(self)) {
            return;
        }
        if (FeaturesConfig.keepItemsWhenSlotsShrink && !self.getItem().isEmpty()) {
            // Keep rendering the item; interaction still blocked in mayPlace/mayPickup.
            return;
        }
        cir.setReturnValue(false);
    }

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void features$blockPlaceLocked(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (LockedSlots.isInteractionLocked(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void features$blockPickupLocked(net.minecraft.world.entity.player.Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (LockedSlots.isInteractionLocked(self)) {
            cir.setReturnValue(false);
        }
    }
}
