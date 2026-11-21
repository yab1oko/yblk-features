package yblk.features.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import yblk.features.remover.ItemRemover;

/** Clears banned stacks from Curios slots. No-op when Curios is not installed. */
public final class CuriosItemRemover {
    private CuriosItemRemover() {
    }

    public static void purge(Player player) {
        if (player == null || !ModList.get().isLoaded("curios") || !ItemRemover.isActive()) {
            return;
        }
        try {
            purgeReflect(player);
        } catch (Throwable ignored) {
        }
    }

    private static void purgeReflect(Player player) throws Exception {
        Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
        Object optInv = curiosApi.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class)
                .invoke(null, player);
        Object handler = unwrap(optInv);
        if (handler == null) {
            return;
        }
        Object curiosMap = handler.getClass().getMethod("getCurios").invoke(handler);
        if (!(curiosMap instanceof java.util.Map<?, ?> map)) {
            return;
        }
        for (Object stacksHandler : map.values()) {
            Object stacks = stacksHandler.getClass().getMethod("getStacks").invoke(stacksHandler);
            int slots = (Integer) stacks.getClass().getMethod("getSlots").invoke(stacks);
            var setStack = stacks.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);
            for (int i = 0; i < slots; i++) {
                ItemStack stack = (ItemStack) stacks.getClass()
                        .getMethod("getStackInSlot", int.class)
                        .invoke(stacks, i);
                if (ItemRemover.removesFromInventory(stack)) {
                    setStack.invoke(stacks, i, ItemStack.EMPTY);
                }
            }
        }
    }

    private static Object unwrap(Object optInv) {
        if (optInv instanceof LazyOptional<?> lazy) {
            return lazy.orElse(null);
        }
        if (optInv instanceof java.util.Optional<?> optional) {
            return optional.orElse(null);
        }
        return null;
    }
}
