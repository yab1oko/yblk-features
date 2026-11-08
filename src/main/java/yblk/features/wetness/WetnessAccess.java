package yblk.features.wetness;

import net.minecraft.world.entity.player.Player;

public final class WetnessAccess {
    private WetnessAccess() {
    }

    /** Operators may toggle wetness and edit its rates. */
    public static boolean canEdit(Player player) {
        return player != null && player.hasPermissions(2);
    }
}
