package yblk.features.remover;

import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import yblk.features.inventory.ExtraInventory;
import yblk.features.inventory.ExtraInventoryCapability;
import yblk.features.network.ItemRemoverConfigPacket;
import yblk.features.network.ModNetwork;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ItemRemoverEvents {
    private static Field lootTablePoolsField;
    private static Field lootPoolEntriesField;
    private static Field lootItemField;

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.sendToPlayer(player, ItemRemoverConfigPacket.current());
            purgeInventory(player);
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        filterRecipes(event.getServer());
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

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onItemJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !ItemRemover.isActive()) {
            return;
        }
        if (event.getEntity() instanceof ItemEntity itemEntity && ItemRemover.isBanned(itemEntity.getItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLootLoad(LootTableLoadEvent event) {
        if (!ItemRemover.isActive()) {
            return;
        }
        stripLootTable(event.getTable());
    }

    public static void onConfigChanged(MinecraftServer server) {
        if (server == null) {
            return;
        }
        filterRecipes(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            purgeInventory(player);
        }
    }

    public static void purgeInventory(Player player) {
        if (!ItemRemover.isActive()) {
            return;
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (ItemRemover.isBanned(inv.getItem(i))) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (ItemRemover.isBanned(player.getOffhandItem())) {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR && ItemRemover.isBanned(player.getItemBySlot(slot))) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        ExtraInventory extra = ExtraInventoryCapability.get(player);
        var handler = extra.getHandler();
        for (int i = 0; i < handler.getSlots(); i++) {
            if (ItemRemover.isBanned(handler.getStackInSlot(i))) {
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        if (player.containerMenu != null && ItemRemover.isBanned(player.containerMenu.getCarried())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    public static void filterRecipes(MinecraftServer server) {
        RecipeManager manager = server.getRecipeManager();
        Collection<Recipe<?>> all = manager.getRecipes();
        if (!ItemRemover.isActive()) {
            return;
        }
        List<Recipe<?>> kept = new ArrayList<>(all.size());
        for (Recipe<?> recipe : all) {
            if (!involvesBanned(recipe, server)) {
                kept.add(recipe);
            }
        }
        if (kept.size() != all.size()) {
            manager.replaceRecipes(kept);
            server.getPlayerList().broadcastAll(new ClientboundUpdateRecipesPacket(manager.getRecipes()));
        }
    }

    private static boolean involvesBanned(Recipe<?> recipe, MinecraftServer server) {
        ItemStack result = recipe.getResultItem(server.registryAccess());
        if (ItemRemover.isBanned(result)) {
            return true;
        }
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            for (ItemStack stack : ingredient.getItems()) {
                if (ItemRemover.isBanned(stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static void stripLootTable(LootTable table) {
        try {
            if (lootTablePoolsField == null) {
                lootTablePoolsField = findField(LootTable.class, "f_79109_", "pools");
            }
            List<LootPool> pools = (List<LootPool>) lootTablePoolsField.get(table);
            if (pools == null) {
                return;
            }
            for (LootPool pool : pools) {
                stripPool(pool);
            }
        } catch (Exception ignored) {
        }
    }

    private static void stripPool(LootPool pool) {
        try {
            if (lootPoolEntriesField == null) {
                lootPoolEntriesField = findField(LootPool.class, "f_79023_", "entries");
            }
            LootPoolEntryContainer[] entries = (LootPoolEntryContainer[]) lootPoolEntriesField.get(pool);
            if (entries == null || entries.length == 0) {
                return;
            }
            List<LootPoolEntryContainer> kept = new ArrayList<>();
            for (LootPoolEntryContainer entry : entries) {
                if (!entryIsBanned(entry)) {
                    kept.add(entry);
                }
            }
            if (kept.size() != entries.length) {
                lootPoolEntriesField.set(pool, kept.toArray(LootPoolEntryContainer[]::new));
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean entryIsBanned(LootPoolEntryContainer entry) {
        if (!(entry instanceof LootItem lootItem)) {
            return false;
        }
        try {
            if (lootItemField == null) {
                lootItemField = findField(LootItem.class, "f_79566_", "item");
            }
            Object item = lootItemField.get(lootItem);
            if (item instanceof ItemLike like) {
                return ItemRemover.isBanned(like.asItem());
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static Field findField(Class<?> owner, String srg, String mapped) throws NoSuchFieldException {
        try {
            Field field = ObfuscationReflectionHelper.findField(owner, srg);
            field.setAccessible(true);
            return field;
        } catch (Exception ex) {
            Field field = owner.getDeclaredField(mapped);
            field.setAccessible(true);
            return field;
        }
    }
}
