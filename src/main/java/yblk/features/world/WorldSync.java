package yblk.features.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import yblk.features.network.ElytraRulesPacket;
import yblk.features.network.ModNetwork;
import yblk.features.network.PortalLockPacket;

public final class WorldSync {
    private WorldSync() {
    }

    public static void afterConfigReload() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player);
        }
    }

    public static void sendTo(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, PortalLockPacket.current());
        ModNetwork.sendToPlayer(player, ElytraRulesPacket.current());
    }
}
