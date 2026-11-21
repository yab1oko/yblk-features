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

/** Interaction-blocker toggle, rules, message, and operator bypass. C2S admin / S2C truth. */
public class InteractionBlockerPacket {
    private final boolean enabled;
    private final java.util.List<String> rules;
    private final String message;
    private final boolean opBypass;

    public InteractionBlockerPacket(boolean enabled, java.util.List<String> rules, String message, boolean opBypass) {
        this.enabled = enabled;
        this.rules = rules;
        this.message = message == null ? "" : message;
        this.opBypass = opBypass;
    }

    public static InteractionBlockerPacket current() {
        return new InteractionBlockerPacket(
                FeaturesConfig.interactionBlockerEnabled,
                new ArrayList<>(FeaturesConfig.interactionRules),
                FeaturesConfig.interactionMessage,
                FeaturesConfig.interactionOpBypass
        );
    }

    public static void encode(InteractionBlockerPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
        CommandBlockerPacket.writeList(buf, packet.rules);
        buf.writeUtf(packet.message, 256);
        buf.writeBoolean(packet.opBypass);
    }

    public static InteractionBlockerPacket decode(FriendlyByteBuf buf) {
        return new InteractionBlockerPacket(
                buf.readBoolean(),
                CommandBlockerPacket.readList(buf),
                buf.readUtf(256),
                buf.readBoolean()
        );
    }

    public static void handle(InteractionBlockerPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender == null || !Restrictions.canEdit(sender)) {
                    return;
                }
                FeaturesConfig.applyInteractionBlocker(packet.enabled, packet.rules, packet.message, packet.opBypass, true);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), current());
            } else {
                FeaturesConfig.applyInteractionBlocker(packet.enabled, packet.rules, packet.message, packet.opBypass, false);
            }
        });
        context.setPacketHandled(true);
    }
}
