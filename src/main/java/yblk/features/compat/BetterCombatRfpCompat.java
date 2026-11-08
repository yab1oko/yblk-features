package yblk.features.compat;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;

/**
 * Soft Better Combat bridge: detect weapon-swing so FP body can yield to
 * PlayerAnimator first-person attack poses (avoids back/neck + double weapon).
 */
public final class BetterCombatRfpCompat {
    private static final String MOD_ID = "bettercombat";
    private static Boolean present;
    private static int apiState; // 0 unknown, 1 ok, -1 missing
    private static java.lang.reflect.Method swingInProgress;

    private BetterCombatRfpCompat() {
    }

    public static boolean isLoaded() {
        if (present == null) {
            present = ModList.get().isLoaded(MOD_ID);
        }
        return present;
    }

    public static boolean isWeaponSwingInProgress() {
        if (!isLoaded()) {
            return false;
        }
        ensureReflect();
        if (apiState != 1 || swingInProgress == null) {
            return false;
        }
        try {
            Object result = swingInProgress.invoke(Minecraft.getInstance());
            return result instanceof Boolean b && b;
        } catch (Throwable t) {
            apiState = -1;
            swingInProgress = null;
            return false;
        }
    }

    private static void ensureReflect() {
        if (apiState != 0) {
            return;
        }
        try {
            Class<?> clientApi = Class.forName("net.bettercombat.api.MinecraftClient_BetterCombat");
            swingInProgress = clientApi.getMethod("isWeaponSwingInProgress");
            apiState = 1;
        } catch (Throwable t) {
            swingInProgress = null;
            apiState = -1;
        }
    }
}
