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

/** Elytra rules: toggle, operator bypass, message, and per-dimension lines. */
public class ElytraRulesPacket {
    private final boolean enabled;
    private final boolean opBypass;
    private final String message;
    private final java.util.List<String> rules;

    public ElytraRulesPacket(boolean enabled, boolean opBypass, String message, java.util.List<String> rules) {
        this.enabled = enabled;
        this.opBypass = opBypass;
        this.message = message == null ? "" : message;
        this.rules = rules;
    }

    public static ElytraRulesPacket current() {
        return new ElytraRulesPacket(
                FeaturesConfig.elytraRulesEnabled,
                FeaturesConfig.elytraOpBypass,
                FeaturesConfig.elytraMessage,
                new ArrayList<>(FeaturesConfig.elytraRules)
        );
    }

    public static void encode(ElytraRulesPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
        buf.writeBoolean(packet.opBypass);
        buf.writeUtf(packet.message, 256);
        CommandBlockerPacket.writeList(buf, packet.rules);
    }

    public static ElytraRulesPacket decode(FriendlyByteBuf buf) {
        return new ElytraRulesPacket(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(256),
                CommandBlockerPacket.readList(buf)
        );
    }

    public static void handle(ElytraRulesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender == null || !Restrictions.canEdit(sender)) {
                    return;
                }
                FeaturesConfig.applyElytraRules(packet.enabled, packet.opBypass, packet.message, packet.rules, true);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), current());
            } else {
                FeaturesConfig.applyElytraRules(packet.enabled, packet.opBypass, packet.message, packet.rules, false);
            }
        });
        context.setPacketHandled(true);
    }
}
