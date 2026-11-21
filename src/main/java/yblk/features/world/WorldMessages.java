package yblk.features.world;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One action-bar line per player per short stretch. */
public final class WorldMessages {
    private static final Map<UUID, Long> LAST = new HashMap<>();

    private WorldMessages() {
    }

    public static void actionBar(ServerPlayer player, Component message) {
        long now = player.level().getGameTime();
        Long prev = LAST.get(player.getUUID());
        if (prev != null && now - prev < 15L) {
            return;
        }
        LAST.put(player.getUUID(), now);
        player.displayClientMessage(message, true);
    }

    public static void forget(UUID id) {
        if (id != null) {
            LAST.remove(id);
        }
    }
}
