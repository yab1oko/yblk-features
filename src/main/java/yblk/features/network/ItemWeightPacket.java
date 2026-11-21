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

/** Item weight rules. Appended after the villager packet so older ids stay put. */
public class ItemWeightPacket {
    private final boolean enabled;
    private final float capacity;
    private final float slowAt;
    private final float sprintAt;
    private final float hungerAt;
    private final float wet;
    private final float slotBonus;
    private final float strength;
    private final java.util.List<String> overrides;

    public ItemWeightPacket(boolean enabled, float capacity, float slowAt, float sprintAt, float hungerAt,
                            float wet, float slotBonus, float strength, java.util.List<String> overrides) {
        this.enabled = enabled;
        this.capacity = capacity;
        this.slowAt = slowAt;
        this.sprintAt = sprintAt;
        this.hungerAt = hungerAt;
        this.wet = wet;
        this.slotBonus = slotBonus;
        this.strength = strength;
        this.overrides = overrides;
    }

    public static ItemWeightPacket current() {
        return new ItemWeightPacket(
                FeaturesConfig.itemWeightEnabled,
                FeaturesConfig.itemWeightCapacity,
                FeaturesConfig.itemWeightSlow,
                FeaturesConfig.itemWeightSprint,
                FeaturesConfig.itemWeightHunger,
                FeaturesConfig.itemWeightWet,
                FeaturesConfig.itemWeightSlotBonus,
                FeaturesConfig.itemWeightStrength,
                new ArrayList<>(FeaturesConfig.itemWeightOverrides)
        );
    }

    public static void encode(ItemWeightPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
        buf.writeFloat(packet.capacity);
        buf.writeFloat(packet.slowAt);
        buf.writeFloat(packet.sprintAt);
        buf.writeFloat(packet.hungerAt);
        buf.writeFloat(packet.wet);
        buf.writeFloat(packet.slotBonus);
        buf.writeFloat(packet.strength);
        CommandBlockerPacket.writeList(buf, packet.overrides);
    }

    public static ItemWeightPacket decode(FriendlyByteBuf buf) {
        return new ItemWeightPacket(
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                CommandBlockerPacket.readList(buf)
        );
    }

    public static void handle(ItemWeightPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender == null || !Restrictions.canEdit(sender)) {
                    return;
                }
                FeaturesConfig.applyItemWeight(packet.enabled, packet.capacity, packet.slowAt, packet.sprintAt,
                        packet.hungerAt, packet.wet, packet.slotBonus, packet.strength, packet.overrides, true);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), current());
            } else {
                FeaturesConfig.applyItemWeight(packet.enabled, packet.capacity, packet.slowAt, packet.sprintAt,
                        packet.hungerAt, packet.wet, packet.slotBonus, packet.strength, packet.overrides, false);
            }
        });
        context.setPacketHandled(true);
    }
}
