package yblk.features.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import yblk.features.config.FeaturesConfig;
import yblk.features.wetness.WetnessAccess;

import java.util.function.Supplier;

/** Sync wetness feature toggle + rate knobs. C2S admin / S2C truth. */
public class WetnessConfigPacket {
    private final boolean enabled;
    private final float rainRate;
    private final float dryRate;
    private final float waterEnterBare;
    private final float waterEnterFull;
    private final float waterRateBare;
    private final float waterRateFull;
    private final float heatTorch;
    private final float heatFire;
    private final float heatLava;
    private final int heatRadius;
    private final int freezeSeconds;
    private final float armorSlow;

    public WetnessConfigPacket(
            boolean enabled,
            float rainRate,
            float dryRate,
            float waterEnterBare,
            float waterEnterFull,
            float waterRateBare,
            float waterRateFull,
            float heatTorch,
            float heatFire,
            float heatLava,
            int heatRadius,
            int freezeSeconds,
            float armorSlow) {
        this.enabled = enabled;
        this.rainRate = rainRate;
        this.dryRate = dryRate;
        this.waterEnterBare = waterEnterBare;
        this.waterEnterFull = waterEnterFull;
        this.waterRateBare = waterRateBare;
        this.waterRateFull = waterRateFull;
        this.heatTorch = heatTorch;
        this.heatFire = heatFire;
        this.heatLava = heatLava;
        this.heatRadius = heatRadius;
        this.freezeSeconds = freezeSeconds;
        this.armorSlow = armorSlow;
    }

    public static WetnessConfigPacket current() {
        return new WetnessConfigPacket(
                FeaturesConfig.wetnessEnabled,
                FeaturesConfig.wetnessRainRate,
                FeaturesConfig.wetnessDryRate,
                FeaturesConfig.wetnessWaterEnterBare,
                FeaturesConfig.wetnessWaterEnterFull,
                FeaturesConfig.wetnessWaterRateBare,
                FeaturesConfig.wetnessWaterRateFull,
                FeaturesConfig.wetnessHeatTorch,
                FeaturesConfig.wetnessHeatFire,
                FeaturesConfig.wetnessHeatLava,
                FeaturesConfig.wetnessHeatRadius,
                FeaturesConfig.wetnessFreezeSeconds,
                FeaturesConfig.wetnessArmorSlow);
    }

    public static void encode(WetnessConfigPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
        buf.writeFloat(packet.rainRate);
        buf.writeFloat(packet.dryRate);
        buf.writeFloat(packet.waterEnterBare);
        buf.writeFloat(packet.waterEnterFull);
        buf.writeFloat(packet.waterRateBare);
        buf.writeFloat(packet.waterRateFull);
        buf.writeFloat(packet.heatTorch);
        buf.writeFloat(packet.heatFire);
        buf.writeFloat(packet.heatLava);
        buf.writeVarInt(packet.heatRadius);
        buf.writeVarInt(packet.freezeSeconds);
        buf.writeFloat(packet.armorSlow);
    }

    public static WetnessConfigPacket decode(FriendlyByteBuf buf) {
        return new WetnessConfigPacket(
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readFloat());
    }

    public static void handle(WetnessConfigPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender == null || !WetnessAccess.canEdit(sender)) {
                    return;
                }
                apply(packet);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), current());
            } else {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> apply(packet));
            }
        });
        context.setPacketHandled(true);
    }

    private static void apply(WetnessConfigPacket packet) {
        FeaturesConfig.setWetnessEnabled(packet.enabled);
        FeaturesConfig.setWetnessRainRate(packet.rainRate);
        FeaturesConfig.setWetnessDryRate(packet.dryRate);
        FeaturesConfig.setWetnessWaterEnterBare(packet.waterEnterBare);
        FeaturesConfig.setWetnessWaterEnterFull(packet.waterEnterFull);
        FeaturesConfig.setWetnessWaterRateBare(packet.waterRateBare);
        FeaturesConfig.setWetnessWaterRateFull(packet.waterRateFull);
        FeaturesConfig.setWetnessHeatTorch(packet.heatTorch);
        FeaturesConfig.setWetnessHeatFire(packet.heatFire);
        FeaturesConfig.setWetnessHeatLava(packet.heatLava);
        FeaturesConfig.setWetnessHeatRadius(packet.heatRadius);
        FeaturesConfig.setWetnessFreezeSeconds(packet.freezeSeconds);
        FeaturesConfig.setWetnessArmorSlow(packet.armorSlow);
    }
}
