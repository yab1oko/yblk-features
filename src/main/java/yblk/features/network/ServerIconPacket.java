package yblk.features.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import yblk.features.mixin.MinecraftServerStatusAccessor;
import yblk.features.restrict.Restrictions;
import yblk.features.server.ServerTweaks;

import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Supplier;

/** Uploads a 64x64 server-list icon. Appended after server actions. */
public class ServerIconPacket {
    private final byte[] png;

    public ServerIconPacket(byte[] png) {
        this.png = png == null ? new byte[0] : png;
    }

    public static void encode(ServerIconPacket packet, FriendlyByteBuf buf) {
        buf.writeByteArray(packet.png);
    }

    public static ServerIconPacket decode(FriendlyByteBuf buf) {
        return new ServerIconPacket(buf.readByteArray(65536));
    }

    public static void handle(ServerIconPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.server == null || !Restrictions.canEdit(sender) || packet.png.length < 32) {
                return;
            }
            try {
                Files.write(sender.server.getFile("server-icon.png").toPath(), packet.png);
                MinecraftServerStatusAccessor access = (MinecraftServerStatusAccessor) (Object) sender.server;
                access.features$setStatusIcon(access.features$loadStatusIcon().orElse(null));
                sender.server.invalidateStatus();
                ServerTweaks.notePublic("server icon");
            } catch (Exception ignored) {
                ServerTweaks.notePublic("server icon failed");
            }
        });
        context.setPacketHandled(true);
    }
}
