package yblk.features.restrict;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One action-bar line per player per short stretch, so a held click does not flood chat. */
public final class RestrictionMessages {
    private static final Map<UUID, Long> LAST = new HashMap<>();

    private RestrictionMessages() {
    }

    public static boolean actionBar(ServerPlayer player, Component message) {
        long now = player.level().getGameTime();
        Long prev = LAST.get(player.getUUID());
        if (prev != null && now - prev < 15L) {
            return false;
        }
        LAST.put(player.getUUID(), now);
        player.displayClientMessage(message, true);
        return true;
    }

    public static void forget(UUID id) {
        if (id != null) {
            LAST.remove(id);
        }
    }
}
