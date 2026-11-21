package yblk.features.mobs;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import yblk.features.network.MobDropsPacket;
import yblk.features.network.ModNetwork;
import yblk.features.network.VillagerTradesPacket;

public final class MobsSync {
    private MobsSync() {
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
        ModNetwork.sendToPlayer(player, MobDropsPacket.current());
        ModNetwork.sendToPlayer(player, VillagerTradesPacket.current());
    }
}
