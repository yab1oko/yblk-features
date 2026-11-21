package yblk.features.remover;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.config.FeaturesConfig;
import yblk.features.config.FeaturesConfig.ItemBan;

public final class ItemRemover {
    private ItemRemover() {
    }

    public static boolean isActive() {
        return FeaturesConfig.itemRemoverEnabled;
    }

    public static boolean canEdit(Player player) {
        return player != null && player.hasPermissions(2);
    }

    public static boolean isBanned(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return isBanned(stack.getItem());
    }

    public static boolean isBanned(Item item) {
        if (!isActive() || item == null) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null && FeaturesConfig.isItemBanned(id);
    }

    public static boolean isBanned(ResourceLocation id) {
        return isActive() && id != null && FeaturesConfig.isItemBanned(id);
    }

    public static boolean removesFromInventory(ItemStack stack) {
        return flag(stack, ban -> ban.inventory);
    }

    public static boolean hidesFromRecipes(ItemStack stack) {
        return flag(stack, ban -> ban.recipes);
    }

    public static boolean removesFromLoot(ItemStack stack) {
        return flag(stack, ban -> ban.loot);
    }

    public static boolean removesFromInventory(Item item) {
        return item != null && removesFromInventory(new ItemStack(item));
    }

    private static boolean flag(ItemStack stack, java.util.function.Predicate<ItemBan> test) {
        if (!isActive() || stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        ItemBan ban = FeaturesConfig.itemBan(id);
        return ban != null && test.test(ban);
    }
}
