package yblk.features.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Soft Curios hook for Real First Person: temporarily clear head-worn curios
 * during the FP body pass so hats / masks / face gear do not clip the camera.
 */
public final class CuriosRfpCompat {
    private static final Set<String> HEAD_SLOT_IDS = Set.of(
            "head", "face", "hat", "crown", "headset", "mask", "eyes", "eye", "forehead", "glasses"
    );

    private CuriosRfpCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("curios");
    }

    /**
     * Clears head-like curios. Returns restore actions (always safe to run).
     */
    public static List<Runnable> suppressHeadCurios(Player player) {
        List<Runnable> restore = new ArrayList<>();
        if (!isLoaded() || player == null) {
            return restore;
        }
        try {
            suppressReflect(player, restore);
        } catch (Throwable ignored) {
            // Soft fail — Curios API shape may differ.
        }
        return restore;
    }

    @SuppressWarnings("unchecked")
    private static void suppressReflect(Player player, List<Runnable> restore) throws Exception {
        Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
        Object optInv = curiosApi.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class)
                .invoke(null, player);
        Object handler = null;
        if (optInv instanceof net.minecraftforge.common.util.LazyOptional<?> lazy) {
            handler = lazy.orElse(null);
        } else if (optInv instanceof java.util.Optional<?> optional) {
            handler = optional.orElse(null);
        }
        if (handler == null) {
            return;
        }
        Object curiosMap = handler.getClass().getMethod("getCurios").invoke(handler);
        if (!(curiosMap instanceof Map<?, ?> map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String slotId = String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT);
            if (!isHeadSlot(slotId)) {
                continue;
            }
            Object stacksHandler = entry.getValue();
            Object stacks = stacksHandler.getClass().getMethod("getStacks").invoke(stacksHandler);
            int slots = (Integer) stacks.getClass().getMethod("getSlots").invoke(stacks);
            for (int i = 0; i < slots; i++) {
                ItemStack stack = (ItemStack) stacks.getClass()
                        .getMethod("getStackInSlot", int.class)
                        .invoke(stacks, i);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                ItemStack copy = stack.copy();
                final int index = i;
                stacks.getClass().getMethod("setStackInSlot", int.class, ItemStack.class)
                        .invoke(stacks, index, ItemStack.EMPTY);
                restore.add(() -> {
                    try {
                        stacks.getClass().getMethod("setStackInSlot", int.class, ItemStack.class)
                                .invoke(stacks, index, copy);
                    } catch (Throwable ignored) {
                    }
                });
            }
        }
    }

    private static boolean isHeadSlot(String slotId) {
        if (HEAD_SLOT_IDS.contains(slotId)) {
            return true;
        }
        return slotId.contains("head") || slotId.contains("hat") || slotId.contains("face")
                || slotId.contains("mask") || slotId.contains("crown") || slotId.contains("eye")
                || slotId.contains("glass");
    }
}
