package yblk.features.f3;

import net.minecraft.world.entity.player.Player;
import yblk.features.config.FeaturesConfig;

public final class F3Hide {
    private F3Hide() {
    }

    public static boolean isActive() {
        return FeaturesConfig.f3HideEnabled;
    }

    public static boolean isHidden(F3Element element) {
        return isActive() && FeaturesConfig.isF3ElementHidden(element);
    }

    public static boolean canEdit(Player player) {
        if (!FeaturesConfig.f3HideAdminLocked) {
            return true;
        }
        return player != null && player.hasPermissions(2);
    }
}
