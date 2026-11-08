package yblk.features.compat;

import net.minecraftforge.fml.ModList;

/**
 * Soft PlayerAnimator hook for Real First Person.
 * We deliberately do not override arm / body poses — rendering goes through
 * {@code EntityRenderDispatcher}, so PlayerAnimator mixins apply naturally.
 */
public final class PlayerAnimatorRfpCompat {
    private static final String MOD_ID = "playeranimator";
    private static Boolean present;

    private PlayerAnimatorRfpCompat() {
    }

    public static boolean isLoaded() {
        if (present == null) {
            present = ModList.get().isLoaded(MOD_ID);
        }
        return present;
    }

    /**
     * True when we must not invent our own pose overrides (always the case
     * while PlayerAnimator is present — kept for call sites / future gates).
     */
    public static boolean avoidPoseOverrides() {
        return isLoaded();
    }
}
