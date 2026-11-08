package yblk.features.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import yblk.features.inventory.ExtraInventory;
import yblk.features.inventory.ExtraInventoryCapability;

import java.util.function.Supplier;

public class SyncInventoryRevampPacket {
    private final int unlocked;
    private final CompoundTag extraTag;

    public SyncInventoryRevampPacket(int unlocked, CompoundTag extraTag) {
        this.unlocked = unlocked;
        this.extraTag = extraTag;
    }

    public static void encode(SyncInventoryRevampPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.unlocked);
        buf.writeNbt(packet.extraTag);
    }

    public static SyncInventoryRevampPacket decode(FriendlyByteBuf buf) {
        return new SyncInventoryRevampPacket(buf.readVarInt(), buf.readNbt());
    }

    public static void handle(SyncInventoryRevampPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.handle(packet)));
        ctx.get().setPacketHandled(true);
    }

    private static final class Client {
        private static void handle(SyncInventoryRevampPacket packet) {
            Player player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            ExtraInventory extra = ExtraInventoryCapability.get(player);
            if (packet.extraTag != null) {
                extra.deserializeNBT(packet.extraTag);
            }
            extra.setUnlockedCache(packet.unlocked);
        }
    }
}
