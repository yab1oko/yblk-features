package yblk.features.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import yblk.features.chat.LocalChat;
import yblk.features.config.FeaturesConfig;

import java.util.function.Supplier;

/** Sync local-chat settings. C2S: admin request. S2C: server truth. */
public class LocalChatConfigPacket {
    private final boolean enabled;
    private final int range;
    private final boolean adminBypass;

    public LocalChatConfigPacket(boolean enabled, int range, boolean adminBypass) {
        this.enabled = enabled;
        this.range = range;
        this.adminBypass = adminBypass;
    }

    public static LocalChatConfigPacket current() {
        return new LocalChatConfigPacket(
                FeaturesConfig.localChatEnabled,
                FeaturesConfig.localChatRange,
                FeaturesConfig.localChatAdminBypass);
    }

    public static void encode(LocalChatConfigPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
        buf.writeVarInt(packet.range);
        buf.writeBoolean(packet.adminBypass);
    }

    public static LocalChatConfigPacket decode(FriendlyByteBuf buf) {
        return new LocalChatConfigPacket(buf.readBoolean(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(LocalChatConfigPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender == null || !LocalChat.canEdit(sender)) {
                    return;
                }
                FeaturesConfig.setLocalChatEnabled(packet.enabled);
                FeaturesConfig.setLocalChatRange(packet.range);
                FeaturesConfig.setLocalChatAdminBypass(packet.adminBypass);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), current());
            } else {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.apply(packet));
            }
        });
        context.setPacketHandled(true);
    }

    private static final class Client {
        private static void apply(LocalChatConfigPacket packet) {
            FeaturesConfig.localChatEnabled = packet.enabled;
            FeaturesConfig.localChatRange = Math.max(1, Math.min(512, packet.range));
            FeaturesConfig.localChatAdminBypass = packet.adminBypass;
        }
    }
}
