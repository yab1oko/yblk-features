package yblk.features.restrict;

import net.minecraft.world.entity.player.Player;

public final class Restrictions {
    private Restrictions() {
    }

    public static boolean canEdit(Player player) {
        return player != null && player.hasPermissions(2);
    }
}
