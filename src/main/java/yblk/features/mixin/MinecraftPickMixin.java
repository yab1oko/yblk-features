package yblk.features.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.remover.ItemRemoverEvents;

/** Middle-click pick drops the item again if it is removed from inventories. */
@Mixin(Minecraft.class)
public class MinecraftPickMixin {
    @Inject(method = "pickBlock", at = @At("RETURN"))
    private void features$stripPickedItem(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (minecraft.player != null) {
            ItemRemoverEvents.purgeInventory(minecraft.player);
        }
    }
}
