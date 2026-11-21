package yblk.features.mixin;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.mobs.TradeLock;

@Mixin(Villager.class)
public class VillagerTradeLockMixin {
    @Inject(method = "startTrading", at = @At("HEAD"), cancellable = true)
    private void features$blockTrading(Player player, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        if (TradeLock.blocks(villager)) {
            TradeLock.tell(player);
            ci.cancel();
            return;
        }
        TradeLock.decorate(villager);
        if (TradeLock.emptied(villager)) {
            TradeLock.tell(player);
            ci.cancel();
        }
    }

    @Inject(method = "updateSpecialPrices", at = @At("HEAD"), cancellable = true)
    private void features$freezePrices(Player player, CallbackInfo ci) {
        if (TradeLock.freeze((Villager) (Object) this)) {
            ci.cancel();
        }
    }
}
