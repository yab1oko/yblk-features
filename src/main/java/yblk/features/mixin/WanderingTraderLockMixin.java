package yblk.features.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yblk.features.mobs.TradeLock;

@Mixin(WanderingTrader.class)
public class WanderingTraderLockMixin {
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void features$blockTrader(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (TradeLock.blocksTrader((WanderingTrader) (Object) this)) {
            TradeLock.tell(player);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
