package yblk.features.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import yblk.features.config.FeaturesConfig;

/**
 * Soft Curios hook: equipped curios that have an inventory-revamp bonus
 * (same item id map as armor) also unlock storage slots — used by Fashion.
 */
public final class CuriosInventoryBonus {
    private CuriosInventoryBonus() {
    }

    public static int bonusSlots(Player player) {
        if (player == null || !ModList.get().isLoaded("curios")) {
            return 0;
        }
        try {
            return collectReflect(player);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int collectReflect(Player player) throws Exception {
        Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
        Object optInv = curiosApi.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class)
                .invoke(null, player);
        Object handler = unwrapHandler(optInv);
        if (handler == null) {
            return 0;
        }
        Object curiosMap = handler.getClass().getMethod("getCurios").invoke(handler);
        if (!(curiosMap instanceof java.util.Map<?, ?> map)) {
            return 0;
        }
        int total = 0;
        for (Object stacksHandler : map.values()) {
            Object stacks = stacksHandler.getClass().getMethod("getStacks").invoke(stacksHandler);
            int slots = (Integer) stacks.getClass().getMethod("getSlots").invoke(stacks);
            for (int i = 0; i < slots; i++) {
                ItemStack stack = (ItemStack) stacks.getClass()
                        .getMethod("getStackInSlot", int.class)
                        .invoke(stacks, i);
                total += FeaturesConfig.bonusFor(stack);
            }
        }
        return total;
    }

    /** Curios 1.20 returns {@link LazyOptional}, not {@link java.util.Optional}. */
    private static Object unwrapHandler(Object optInv) {
        if (optInv == null) {
            return null;
        }
        if (optInv instanceof LazyOptional<?> lazy) {
            return lazy.orElse(null);
        }
        if (optInv instanceof java.util.Optional<?> optional) {
            return optional.orElse(null);
        }
        return null;
    }
}
