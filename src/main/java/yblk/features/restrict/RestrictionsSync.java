package yblk.features.restrict;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import yblk.features.network.CommandBlockerPacket;
import yblk.features.network.InteractionBlockerPacket;
import yblk.features.network.ModNetwork;
import yblk.features.network.ToolRestrictionsPacket;

public final class RestrictionsSync {
    private RestrictionsSync() {
    }

    public static void afterConfigReload() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        CommandBlocker.resend(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player);
        }
    }

    public static void sendTo(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, CommandBlockerPacket.current());
        ModNetwork.sendToPlayer(player, InteractionBlockerPacket.current());
        ModNetwork.sendToPlayer(player, ToolRestrictionsPacket.current());
    }
}
