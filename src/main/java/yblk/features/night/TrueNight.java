package yblk.features.night;

import net.minecraft.world.entity.player.Player;
import yblk.features.config.FeaturesConfig;

public final class TrueNight {
    private TrueNight() {
    }

    /** Admin lock: only operators (perm 2+) may change settings or toggle the feature. */
    public static boolean canEdit(Player player) {
        if (!FeaturesConfig.trueNightAdminLocked) {
            return true;
        }
        return player != null && player.hasPermissions(2);
    }
}
