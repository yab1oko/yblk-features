package yblk.features.hud;

import net.minecraft.world.entity.player.Player;
import yblk.features.config.FeaturesConfig;

public final class HudHide {
    private HudHide() {
    }

    public static boolean isActive() {
        return FeaturesConfig.hudHideEnabled;
    }

    public static boolean isHidden(HudElement element) {
        return isActive() && FeaturesConfig.isHudElementHidden(element);
    }

    public static boolean showOnChange(HudElement element) {
        return FeaturesConfig.isHudShowOnChange(element);
    }

    /** Admin lock: only operators (perm 2+) may change settings / toggle the feature. */
    public static boolean canEdit(Player player) {
        if (!FeaturesConfig.hudHideAdminLocked) {
            return true;
        }
        return player != null && player.hasPermissions(2);
    }
}
