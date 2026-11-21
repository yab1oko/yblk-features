package yblk.features.mixin;

import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.remover.ItemRemover;

/** Creative slot packets cannot put a removed item into the inventory. */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void features$blockBannedCreativeSlot(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        if (ItemRemover.removesFromInventory(packet.getItem())) {
            ci.cancel();
        }
    }
}
