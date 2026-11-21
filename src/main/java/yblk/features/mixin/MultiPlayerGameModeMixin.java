package yblk.features.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.remover.ItemRemover;

/** Creative tab clicks and drops cannot grant a removed item. */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(method = "handleCreativeModeItemAdd", at = @At("HEAD"), cancellable = true)
    private void features$blockCreativeAdd(ItemStack stack, int slot, CallbackInfo ci) {
        if (ItemRemover.removesFromInventory(stack)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleCreativeModeItemDrop", at = @At("HEAD"), cancellable = true)
    private void features$blockCreativeDrop(ItemStack stack, CallbackInfo ci) {
        if (ItemRemover.removesFromInventory(stack)) {
            ci.cancel();
        }
    }
}
