package yblk.features.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Soft TacZ bridge for Real First Person.
 * When a TacZ gun / animation item is held, we hide our FP body arms and
 * leave TacZ's viewmodel + hand bones alone.
 */
public final class TaczRfpCompat {
    private static final String MOD_ID = "tacz";
    private static Boolean present;
    private static Class<?> iGun;
    private static Class<?> iAnimationItem;
    private static boolean reflectFailed;

    private TaczRfpCompat() {
    }

    public static boolean isLoaded() {
        if (present == null) {
            present = ModList.get().isLoaded(MOD_ID);
        }
        return present;
    }

    public static boolean shouldDelegateArms(Player player) {
        if (!isLoaded() || player == null) {
            return false;
        }
        return isTaczStack(player.getMainHandItem()) || isTaczStack(player.getOffhandItem());
    }

    public static boolean isTaczStack(ItemStack stack) {
        if (!isLoaded() || stack == null || stack.isEmpty()) {
            return false;
        }
        // Namespace / path fallback — covers addons / API shape changes.
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null && (MOD_ID.equals(id.getNamespace()) || id.getNamespace().contains("tacz"))) {
            return true;
        }
        if (reflectFailed) {
            return false;
        }
        ensureReflect();
        if (reflectFailed) {
            return false;
        }
        Object item = stack.getItem();
        try {
            if (iGun != null && iGun.isInstance(item)) {
                return true;
            }
            return iAnimationItem != null && iAnimationItem.isInstance(item);
        } catch (Throwable t) {
            reflectFailed = true;
            return id != null && (id.getPath().contains("gun") || id.getPath().contains("pistol")
                    || id.getPath().contains("rifle") || id.getPath().contains("shotgun"));
        }
    }

    private static void ensureReflect() {
        if (iGun != null || iAnimationItem != null || reflectFailed) {
            return;
        }
        try {
            iGun = Class.forName("com.tacz.guns.api.item.IGun");
        } catch (ClassNotFoundException ignored) {
            iGun = null;
        }
        try {
            iAnimationItem = Class.forName("com.tacz.guns.api.item.IAnimationItem");
        } catch (ClassNotFoundException ignored) {
            iAnimationItem = null;
        }
        if (iGun == null && iAnimationItem == null) {
            // Keep namespace fallback working; don't lock reflect permanently.
            reflectFailed = false;
        }
    }
}
