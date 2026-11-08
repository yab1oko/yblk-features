package yblk.features.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.ItemStackHandler;
import yblk.features.config.FeaturesConfig;
import yblk.features.network.ModNetwork;
import yblk.features.network.SyncInventoryRevampPacket;

public class InventoryRevampEvents {

    @SubscribeEvent
    public void onEquipChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getSlot().getType() != EquipmentSlot.Type.ARMOR) {
            return;
        }
        applyUnlocked(player);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyUnlocked(player);
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyUnlocked(player);
        }
    }

    @SubscribeEvent
    public void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyUnlocked(player);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        ExtraInventory extra = ExtraInventoryCapability.get(player);
        int unlocked = SlotCalculator.unlockedStorage(player);
        if (extra.getUnlockedCache() != unlocked) {
            applyUnlocked(player);
        }
    }

    public static void applyUnlocked(ServerPlayer player) {
        ExtraInventory extra = ExtraInventoryCapability.get(player);
        int previous = extra.getUnlockedCache();
        int unlocked = SlotCalculator.isActive()
                ? SlotCalculator.unlockedStorage(player)
                : SlotCalculator.CENTER_STORAGE;
        if (SlotCalculator.isActive() && unlocked < previous) {
            shrinkSlots(player, previous, unlocked);
        }
        extra.setUnlockedCache(unlocked);
        sync(player);
    }

    private static void shrinkSlots(ServerPlayer player, int previous, int unlocked) {
        Inventory inv = player.getInventory();
        for (int storageIndex = unlocked; storageIndex < Math.min(previous, SlotCalculator.CENTER_STORAGE); storageIndex++) {
            int invIndex = 9 + storageIndex;
            ItemStack stack = inv.items.get(invIndex);
            if (stack.isEmpty()) {
                continue;
            }
            if (FeaturesConfig.keepItemsWhenSlotsShrink) {
                continue;
            }
            player.drop(stack.copy(), true);
            inv.items.set(invIndex, ItemStack.EMPTY);
        }

        ExtraInventory extra = ExtraInventoryCapability.get(player);
        ItemStackHandler handler = extra.getHandler();
        int prevExtra = Math.max(0, previous - SlotCalculator.CENTER_STORAGE);
        int nextExtra = Math.max(0, unlocked - SlotCalculator.CENTER_STORAGE);
        for (int i = nextExtra; i < Math.min(prevExtra, ExtraInventory.SIZE); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (FeaturesConfig.keepItemsWhenSlotsShrink) {
                continue;
            }
            player.drop(stack.copy(), true);
            handler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    private static void sync(ServerPlayer player) {
        ExtraInventory extra = ExtraInventoryCapability.get(player);
        ModNetwork.sendToPlayer(player, new SyncInventoryRevampPacket(extra.getUnlockedCache(), extra.serializeNBT()));
    }

    /**
     * Unlocked storage for UI/slot locks.
     * Always recomputed from worn armor (synced to client) — never trust a stale cache for layout.
     */
    public static int clientUnlocked(Player player) {
        if (!SlotCalculator.isActive()) {
            return SlotCalculator.CENTER_STORAGE;
        }
        return SlotCalculator.unlockedStorage(player);
    }
}
