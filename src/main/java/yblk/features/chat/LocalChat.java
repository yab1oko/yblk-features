package yblk.features.chat;

import net.minecraft.world.entity.player.Player;
import yblk.features.config.FeaturesConfig;

public final class LocalChat {
    private LocalChat() {
    }

    public static boolean isActive() {
        return FeaturesConfig.localChatEnabled;
    }

    /** Only operators may enable/disable the feature or change its settings. */
    public static boolean canEdit(Player player) {
        return player != null && player.hasPermissions(2);
    }

    /** Ops with bypass on receive local messages from anywhere on the map. */
    public static boolean canBypass(Player player) {
        return FeaturesConfig.localChatAdminBypass && canEdit(player);
    }
}
