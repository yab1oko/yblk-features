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

/** Portal lock toggle, creation switch, operator bypass, message, and dimension rules. */
public class PortalLockPacket {
    private final boolean enabled;
    private final boolean blockCreation;
    private final boolean opBypass;
    private final String message;
    private final java.util.List<String> rules;

    public PortalLockPacket(boolean enabled, boolean blockCreation, boolean opBypass, String message,
                            java.util.List<String> rules) {
        this.enabled = enabled;
        this.blockCreation = blockCreation;
        this.opBypass = opBypass;
        this.message = message == null ? "" : message;
        this.rules = rules;
    }

    public static PortalLockPacket current() {
        return new PortalLockPacket(
                FeaturesConfig.portalLockEnabled,
                FeaturesConfig.portalBlockCreation,
                FeaturesConfig.portalOpBypass,
                FeaturesConfig.portalMessage,
                new ArrayList<>(FeaturesConfig.portalRules)
        );
    }

    public static void encode(PortalLockPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
        buf.writeBoolean(packet.blockCreation);
        buf.writeBoolean(packet.opBypass);
        buf.writeUtf(packet.message, 256);
        CommandBlockerPacket.writeList(buf, packet.rules);
    }

    public static PortalLockPacket decode(FriendlyByteBuf buf) {
        return new PortalLockPacket(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(256),
                CommandBlockerPacket.readList(buf)
        );
    }

    public static void handle(PortalLockPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender == null || !Restrictions.canEdit(sender)) {
                    return;
                }
                FeaturesConfig.applyPortalLock(
                        packet.enabled, packet.blockCreation, packet.opBypass, packet.message, packet.rules, true);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), current());
            } else {
                FeaturesConfig.applyPortalLock(
                        packet.enabled, packet.blockCreation, packet.opBypass, packet.message, packet.rules, false);
            }
        });
        context.setPacketHandled(true);
    }
}
