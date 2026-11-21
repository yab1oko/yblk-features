package yblk.features.mixin;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.mobs.TradeLock;
import yblk.features.remover.ItemRemoverEvents;

/** Newly rolled trades are filtered before the villager can offer them. */
@Mixin({Villager.class, WanderingTrader.class})
public class VillagerTradesMixin {
    @Inject(method = "updateTrades", at = @At("RETURN"))
    private void features$filterNewTrades(CallbackInfo ci) {
        AbstractVillager villager = (AbstractVillager) (Object) this;
        ItemRemoverEvents.syncVillagerTrades(villager);
        TradeLock.decorate(villager);
    }
}
