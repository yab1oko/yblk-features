package yblk.features.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import yblk.features.inventory.InventoryRevampEvents;

/**
 * Curios equip/unequip → refresh Inventory Revamp unlocked slots immediately
 * (armor already uses {@code LivingEquipmentChangeEvent}; curios do not).
 * <p>
 * Only registered when Curios is loaded — see {@link yblk.features.Features}.
 */
public final class CuriosInventoryRevampEvents {
    @SubscribeEvent
    public void onCurioChange(CurioChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InventoryRevampEvents.applyUnlocked(player);
        }
    }
}
