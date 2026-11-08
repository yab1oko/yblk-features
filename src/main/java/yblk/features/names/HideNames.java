package yblk.features.names;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.config.FeaturesConfig;

import java.util.Locale;
import java.util.Map;

public final class HideNames {
    private HideNames() {
    }

    public static boolean isActive() {
        return FeaturesConfig.hideNamesEnabled;
    }

    public static HideNamesMode mode() {
        return FeaturesConfig.hideNamesMode;
    }

    /** Players may always change the reveal mode. */
    public static boolean canEditMode(Player player) {
        return true;
    }

    /** Item list / admin lock — ops when locked. */
    public static boolean canEditAdmin(Player player) {
        if (!FeaturesConfig.hideNamesAdminLocked) {
            return true;
        }
        return isOperator(player);
    }

    /** Admin-lock checkbox itself — operators only. */
    public static boolean canSeeAdminLock(Player player) {
        return isOperator(player);
    }

    public static boolean isOperator(Player player) {
        return player != null && player.hasPermissions(2);
    }

    /**
     * Whether the name tag should be fully suppressed (no fade render).
     * For fade/look modes this is false when partially/fully visible — client decides alpha.
     */
    public static boolean shouldFullyHide(Player target) {
        if (!isActive() || target == null) {
            return false;
        }
        return switch (mode()) {
            case FULL -> true;
            case MASKS -> wearsHidingItem(target);
            case PROXIMITY_FADE, LOOK_CLOSE -> false;
        };
    }

    public static boolean wearsHidingItem(Player player) {
        if (stackHides(player.getItemBySlot(EquipmentSlot.HEAD))) {
            return true;
        }
        return curiosHasHidingItem(player);
    }

    private static boolean stackHides(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        if (FeaturesConfig.hideNamesItems.contains(id.toString())) {
            return true;
        }
        // Fashion bandanas / balaclavas (Curios mask) always count in MASKS mode.
        String path = id.getPath().toLowerCase(Locale.ROOT);
        return path.contains("bandana") || path.contains("balaclava");
    }

    private static boolean curiosHasHidingItem(Player player) {
        if (!ModList.get().isLoaded("curios")) {
            return false;
        }
        try {
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
                return false;
            }
            Object curiosMap = handler.getClass().getMethod("getCurios").invoke(handler);
            if (!(curiosMap instanceof Map<?, ?> map)) {
                return false;
            }
            for (Object stacksHandler : map.values()) {
                Object stacks = stacksHandler.getClass().getMethod("getStacks").invoke(stacksHandler);
                int slots = (Integer) stacks.getClass().getMethod("getSlots").invoke(stacks);
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = (ItemStack) stacks.getClass()
                            .getMethod("getStackInSlot", int.class)
                            .invoke(stacks, i);
                    if (stackHides(stack)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
            return false;
        }
        return false;
    }
}
