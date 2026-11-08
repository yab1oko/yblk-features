package yblk.features.mixin.curios;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.common.inventory.container.CuriosContainer;
import yblk.features.inventory.SideSlotInstaller;

/**
 * CuriosContainer extends InventoryMenu then clears/rebuilds every slot.
 * Re-install side wings after that rebuild so Curios keeps Inventory Revamp.
 */
@Mixin(value = CuriosContainer.class, remap = false)
public abstract class CuriosContainerMixin {
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Z)V", at = @At("RETURN"))
    private void features$reinstallSideSlots(int containerId, Inventory inventory, boolean local,
                                             CallbackInfo ci) {
        reinstall();
    }

    @Inject(method = "resetSlots", at = @At("RETURN"))
    private void features$reinstallAfterReset(CallbackInfo ci) {
        reinstall();
    }

    @Inject(method = "scrollToIndex", at = @At("RETURN"))
    private void features$reinstallAfterScroll(int index, CallbackInfo ci) {
        reinstall();
    }

    private void reinstall() {
        CuriosContainer self = (CuriosContainer) (Object) this;
        Player player = self.player;
        if (player != null) {
            SideSlotInstaller.ensureSideSlots(self, player.getInventory(), player);
        }
    }
}
