package yblk.features.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import yblk.features.config.FeaturesConfig;
import yblk.features.restrict.Restrictions;

import java.util.ArrayList;
import java.util.function.Supplier;

/** Tool-restriction toggle, exception list, hand whitelist, message, and sound. C2S admin / S2C truth. */
public class ToolRestrictionsPacket {
    private final boolean enabled;
    private final java.util.List<String> exceptions;
    private final java.util.List<String> hand;
    private final String message;
    private final boolean sound;

    public ToolRestrictionsPacket(boolean enabled, java.util.List<String> exceptions, java.util.List<String> hand,
                                  String message, boolean sound) {
        this.enabled = enabled;
        this.exceptions = exceptions;
        this.hand = hand;
        this.message = message == null ? "" : message;
        this.sound = sound;
    }

    public static ToolRestrictionsPacket current() {
        return new ToolRestrictionsPacket(
                FeaturesConfig.toolRestrictionsEnabled,
                new ArrayList<>(FeaturesConfig.toolExceptions),
                new ArrayList<>(FeaturesConfig.toolHand),
                FeaturesConfig.toolMessage,
                FeaturesConfig.toolSound
        );
    }

    public static void encode(ToolRestrictionsPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
        CommandBlockerPacket.writeList(buf, packet.exceptions);
        CommandBlockerPacket.writeList(buf, packet.hand);
        buf.writeUtf(packet.message, 256);
        buf.writeBoolean(packet.sound);
    }

    public static ToolRestrictionsPacket decode(FriendlyByteBuf buf) {
        return new ToolRestrictionsPacket(
                buf.readBoolean(),
                CommandBlockerPacket.readList(buf),
                CommandBlockerPacket.readList(buf),
                buf.readUtf(256),
                buf.readBoolean()
        );
    }

    public static void handle(ToolRestrictionsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender == null || !Restrictions.canEdit(sender)) {
                    return;
                }
                FeaturesConfig.applyToolRestrictions(
                        packet.enabled, packet.exceptions, packet.hand, packet.message, packet.sound, true);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), current());
            } else {
                FeaturesConfig.applyToolRestrictions(
                        packet.enabled, packet.exceptions, packet.hand, packet.message, packet.sound, false);
            }
        });
        context.setPacketHandled(true);
    }
}
