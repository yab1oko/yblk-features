package yblk.features.remover;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import yblk.features.compat.CuriosItemRemover;
import yblk.features.inventory.ExtraInventory;
import yblk.features.inventory.ExtraInventoryCapability;
import yblk.features.network.ItemRemoverConfigPacket;
import yblk.features.network.ModNetwork;

import java.util.ArrayList;
import java.util.List;

public class ItemRemoverEvents {
    private static final String TRADE_BACKUP = "features:trade_backup";
    private static List<Recipe<?>> sourceRecipes = List.of();

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.sendToPlayer(player, ItemRemoverConfigPacket.current());
            purgeInventory(player);
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        filterRecipes(event.getServer(), true);
    }

    @SubscribeEvent
    public void onDatapack(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        MinecraftServer server = event.getPlayerList().getServer();
        filterRecipes(server, true);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!ItemRemover.isActive()) {
            return;
        }
        if (event.player.tickCount % 10 != 0) {
            return;
        }
        purgeInventory(event.player);
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide || !ItemRemover.isActive()) {
            return;
        }
        if (event.level.getGameTime() % 40L != 0L || !(event.level instanceof ServerLevel level)) {
            return;
        }
        AABB box = loadedBox(level);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box,
                item -> ItemRemover.removesFromInventory(item.getItem()))) {
            entity.discard();
        }
        for (AbstractVillager villager : level.getEntitiesOfClass(AbstractVillager.class, box)) {
            syncVillagerTrades(villager);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onItemJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !ItemRemover.isActive()) {
            return;
        }
        if (event.getEntity() instanceof ItemEntity itemEntity && ItemRemover.removesFromInventory(itemEntity.getItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPickup(EntityItemPickupEvent event) {
        if (!ItemRemover.removesFromInventory(event.getItem().getItem())) {
            return;
        }
        event.setCanceled(true);
        event.getItem().discard();
    }

    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!ItemRemover.isActive()) {
            return;
        }
        List<ItemStack> remove = new ArrayList<>();
        for (var entry : event.getEntries()) {
            if (ItemRemover.removesFromInventory(entry.getKey())) {
                remove.add(entry.getKey());
            }
        }
        for (ItemStack stack : remove) {
            event.getEntries().remove(stack);
        }
    }

    public static void onConfigChanged(MinecraftServer server) {
        if (server == null) {
            return;
        }
        filterRecipes(server, false);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            purgeInventory(player);
        }
        for (ServerLevel level : server.getAllLevels()) {
            AABB box = loadedBox(level);
            for (AbstractVillager villager : level.getEntitiesOfClass(AbstractVillager.class, box)) {
                syncVillagerTrades(villager);
            }
        }
    }

    public static void purgeInventory(Player player) {
        if (!ItemRemover.isActive() || player == null) {
            return;
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (ItemRemover.removesFromInventory(inv.getItem(i))) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (ItemRemover.removesFromInventory(player.getOffhandItem())) {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR && ItemRemover.removesFromInventory(player.getItemBySlot(slot))) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        ExtraInventory extra = ExtraInventoryCapability.get(player);
        var handler = extra.getHandler();
        for (int i = 0; i < handler.getSlots(); i++) {
            if (ItemRemover.removesFromInventory(handler.getStackInSlot(i))) {
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        Container ender = player.getEnderChestInventory();
        for (int i = 0; i < ender.getContainerSize(); i++) {
            if (ItemRemover.removesFromInventory(ender.getItem(i))) {
                ender.setItem(i, ItemStack.EMPTY);
            }
        }
        CuriosItemRemover.purge(player);
        AbstractContainerMenu menu = player.containerMenu;
        if (menu != null) {
            if (ItemRemover.removesFromInventory(menu.getCarried())) {
                menu.setCarried(ItemStack.EMPTY);
            }
            for (Slot slot : menu.slots) {
                if (ItemRemover.removesFromInventory(slot.getItem())) {
                    slot.set(ItemStack.EMPTY);
                }
            }
        }
    }

    public static void filterRecipes(MinecraftServer server, boolean recapture) {
        RecipeManager manager = server.getRecipeManager();
        if (recapture || sourceRecipes.isEmpty()) {
            sourceRecipes = new ArrayList<>(manager.getRecipes());
        }
        List<Recipe<?>> kept = new ArrayList<>(sourceRecipes.size());
        boolean active = ItemRemover.isActive();
        for (Recipe<?> recipe : sourceRecipes) {
            if (active && hidesRecipe(recipe, server)) {
                continue;
            }
            kept.add(recipe);
        }
        manager.replaceRecipes(kept);
        server.getPlayerList().broadcastAll(new ClientboundUpdateRecipesPacket(manager.getRecipes()));
    }

    private static boolean hidesRecipe(Recipe<?> recipe, MinecraftServer server) {
        ItemStack result = recipe.getResultItem(server.registryAccess());
        if (ItemRemover.hidesFromRecipes(result)) {
            return true;
        }
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            for (ItemStack stack : ingredient.getItems()) {
                if (ItemRemover.hidesFromRecipes(stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void syncVillagerTrades(AbstractVillager villager) {
        CompoundTag data = villager.getPersistentData();
        MerchantOffers live = villager.getOffers();
        MerchantOffers backup = readTrades(data);
        if (backup.isEmpty() && !data.contains(TRADE_BACKUP)) {
            for (MerchantOffer offer : live) {
                backup.add(new MerchantOffer(offer.createTag()));
            }
        }
        for (MerchantOffer offer : live) {
            MerchantOffer known = findDeal(backup, offer);
            if (known == null) {
                backup.add(new MerchantOffer(offer.createTag()));
            } else {
                backup.set(backup.indexOf(known), new MerchantOffer(offer.createTag()));
            }
        }
        writeTrades(data, backup);
        MerchantOffers visible = new MerchantOffers();
        for (MerchantOffer offer : backup) {
            if (!hidesTrade(offer)) {
                visible.add(new MerchantOffer(offer.createTag()));
            }
        }
        if (!sameDeals(live, visible)) {
            villager.overrideOffers(visible);
        }
    }

    private static boolean hidesTrade(MerchantOffer offer) {
        return ItemRemover.removesFromLoot(offer.getResult())
                || ItemRemover.removesFromLoot(offer.getBaseCostA())
                || ItemRemover.removesFromLoot(offer.getCostB());
    }

    private static MerchantOffer findDeal(MerchantOffers offers, MerchantOffer probe) {
        for (MerchantOffer offer : offers) {
            if (ItemStack.isSameItem(offer.getResult(), probe.getResult())
                    && ItemStack.isSameItem(offer.getBaseCostA(), probe.getBaseCostA())
                    && ItemStack.isSameItem(offer.getCostB(), probe.getCostB())) {
                return offer;
            }
        }
        return null;
    }

    private static boolean sameDeals(MerchantOffers left, MerchantOffers right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            if (findDeal(right, left.get(i)) == null) {
                return false;
            }
        }
        return true;
    }

    private static MerchantOffers readTrades(CompoundTag data) {
        MerchantOffers offers = new MerchantOffers();
        if (!data.contains(TRADE_BACKUP, Tag.TAG_LIST)) {
            return offers;
        }
        ListTag list = data.getList(TRADE_BACKUP, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            offers.add(new MerchantOffer(list.getCompound(i)));
        }
        return offers;
    }

    private static void writeTrades(CompoundTag data, MerchantOffers offers) {
        ListTag list = new ListTag();
        for (MerchantOffer offer : offers) {
            list.add(offer.createTag());
        }
        data.put(TRADE_BACKUP, list);
    }

    private static AABB loadedBox(Level level) {
        return new AABB(
                level.getWorldBorder().getMinX(), level.getMinBuildHeight(), level.getWorldBorder().getMinZ(),
                level.getWorldBorder().getMaxX(), level.getMaxBuildHeight(), level.getWorldBorder().getMaxZ());
    }
}
