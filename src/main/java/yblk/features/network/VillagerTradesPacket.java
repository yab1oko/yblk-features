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

/** Villager trade lock: who can trade, which results disappear, and frozen prices. */
public class VillagerTradesPacket {
    private final boolean enabled;
    private final boolean blockAll;
    private final boolean blockWandering;
    private final boolean freezePrices;
    private final String message;
    private final java.util.List<String> professions;
    private final java.util.List<String> results;
    private final java.util.List<String> customs;

    public VillagerTradesPacket(boolean enabled, boolean blockAll, boolean blockWandering, boolean freezePrices,
                                String message, java.util.List<String> professions, java.util.List<String> results,
                                java.util.List<String> customs) {
        this.enabled = enabled;
        this.blockAll = blockAll;
        this.blockWandering = blockWandering;
        this.freezePrices = freezePrices;
        this.message = message == null ? "" : message;
        this.professions = professions;
        this.results = results;
        this.customs = customs;
    }

    public static VillagerTradesPacket current() {
        return new VillagerTradesPacket(
                FeaturesConfig.villagerTradesEnabled,
                FeaturesConfig.villagerBlockAll,
                FeaturesConfig.villagerBlockWandering,
                FeaturesConfig.villagerFreezePrices,
                FeaturesConfig.villagerMessage,
                new ArrayList<>(FeaturesConfig.villagerProfessions),
                new ArrayList<>(FeaturesConfig.villagerResults),
                new ArrayList<>(FeaturesConfig.villagerCustoms)
        );
    }

    public static void encode(VillagerTradesPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
        buf.writeBoolean(packet.blockAll);
        buf.writeBoolean(packet.blockWandering);
        buf.writeBoolean(packet.freezePrices);
        buf.writeUtf(packet.message, 256);
        CommandBlockerPacket.writeList(buf, packet.professions);
        CommandBlockerPacket.writeList(buf, packet.results);
        CommandBlockerPacket.writeList(buf, packet.customs);
    }

    public static VillagerTradesPacket decode(FriendlyByteBuf buf) {
        return new VillagerTradesPacket(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(256),
                CommandBlockerPacket.readList(buf),
                CommandBlockerPacket.readList(buf),
                CommandBlockerPacket.readList(buf)
        );
    }

    public static void handle(VillagerTradesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender == null || !Restrictions.canEdit(sender)) {
                    return;
                }
                FeaturesConfig.applyVillagerTrades(packet.enabled, packet.blockAll, packet.blockWandering,
                        packet.freezePrices, packet.message, packet.professions, packet.results, packet.customs, true);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), current());
            } else {
                FeaturesConfig.applyVillagerTrades(packet.enabled, packet.blockAll, packet.blockWandering,
                        packet.freezePrices, packet.message, packet.professions, packet.results, packet.customs, false);
            }
        });
        context.setPacketHandled(true);
    }
}
