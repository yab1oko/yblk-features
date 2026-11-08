package yblk.features.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import yblk.features.wetness.Wetness;
import yblk.features.wetness.WetnessCapability;

import java.util.function.Supplier;

/** S2C wetness capability sync for HUD / client prediction. */
public class SyncWetnessPacket {
    private final float amount;
    private final int wetLandSeconds;
    private final boolean wasInWater;

    public SyncWetnessPacket(float amount, int wetLandSeconds, boolean wasInWater) {
        this.amount = amount;
        this.wetLandSeconds = wetLandSeconds;
        this.wasInWater = wasInWater;
    }

    public static void encode(SyncWetnessPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.amount);
        buf.writeVarInt(packet.wetLandSeconds);
        buf.writeBoolean(packet.wasInWater);
    }

    public static SyncWetnessPacket decode(FriendlyByteBuf buf) {
        return new SyncWetnessPacket(buf.readFloat(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(SyncWetnessPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.handle(packet)));
        ctx.get().setPacketHandled(true);
    }

    private static final class Client {
        private static void handle(SyncWetnessPacket packet) {
            Player player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            Wetness wet = WetnessCapability.get(player);
            wet.set(packet.amount);
            wet.setWetLandSeconds(packet.wetLandSeconds);
            wet.setWasInWater(packet.wasInWater);
        }
    }
}
