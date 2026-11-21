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

/** Mob drop rules: toggle, clear vanilla drops, and the rule lines. */
public class MobDropsPacket {
    private final boolean enabled;
    private final boolean clearVanilla;
    private final java.util.List<String> rules;

    public MobDropsPacket(boolean enabled, boolean clearVanilla, java.util.List<String> rules) {
        this.enabled = enabled;
        this.clearVanilla = clearVanilla;
        this.rules = rules;
    }

    public static MobDropsPacket current() {
        return new MobDropsPacket(FeaturesConfig.mobDropsEnabled, FeaturesConfig.mobDropsClearVanilla,
                new ArrayList<>(FeaturesConfig.mobDropRules));
    }

    public static void encode(MobDropsPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
        buf.writeBoolean(packet.clearVanilla);
        CommandBlockerPacket.writeList(buf, packet.rules);
    }

    public static MobDropsPacket decode(FriendlyByteBuf buf) {
        return new MobDropsPacket(buf.readBoolean(), buf.readBoolean(), CommandBlockerPacket.readList(buf));
    }

    public static void handle(MobDropsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender == null || !Restrictions.canEdit(sender)) {
                    return;
                }
                FeaturesConfig.applyMobDrops(packet.enabled, packet.clearVanilla, packet.rules, true);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), current());
            } else {
                FeaturesConfig.applyMobDrops(packet.enabled, packet.clearVanilla, packet.rules, false);
            }
        });
        context.setPacketHandled(true);
    }
}
