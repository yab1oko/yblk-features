package yblk.features.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import yblk.features.config.FeaturesConfig;
import yblk.features.remover.ItemRemover;
import yblk.features.remover.ItemRemoverEvents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Sync item-remover toggle + banned IDs. C2S admin / S2C truth. */
public class ItemRemoverConfigPacket {
    private final boolean enabled;
    private final List<String> bannedIds;

    public ItemRemoverConfigPacket(boolean enabled, List<String> bannedIds) {
        this.enabled = enabled;
        this.bannedIds = bannedIds;
    }

    public static ItemRemoverConfigPacket current() {
        return new ItemRemoverConfigPacket(
                FeaturesConfig.itemRemoverEnabled,
                FeaturesConfig.bannedItemIdList());
    }

    public static void encode(ItemRemoverConfigPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
        buf.writeVarInt(packet.bannedIds.size());
        for (String id : packet.bannedIds) {
            buf.writeUtf(id);
        }
    }

    public static ItemRemoverConfigPacket decode(FriendlyByteBuf buf) {
        boolean enabled = buf.readBoolean();
        int count = buf.readVarInt();
        List<String> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(buf.readUtf());
        }
        return new ItemRemoverConfigPacket(enabled, ids);
    }

    public static void handle(ItemRemoverConfigPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender == null || !ItemRemover.canEdit(sender)) {
                    return;
                }
                FeaturesConfig.setItemRemoverEnabled(packet.enabled);
                FeaturesConfig.setBannedItemIds(new HashSet<>(packet.bannedIds));
                ItemRemoverEvents.onConfigChanged(sender.server);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), current());
            } else {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.apply(packet));
            }
        });
        context.setPacketHandled(true);
    }

    private static final class Client {
        private static void apply(ItemRemoverConfigPacket packet) {
            FeaturesConfig.itemRemoverEnabled = packet.enabled;
            Set<String> set = new HashSet<>();
            for (String id : packet.bannedIds) {
                if (id != null && !id.isBlank()) {
                    set.add(id.trim().toLowerCase());
                }
            }
            FeaturesConfig.bannedItemIds = set;
        }
    }
}
