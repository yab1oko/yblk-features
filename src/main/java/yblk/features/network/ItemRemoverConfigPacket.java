package yblk.features.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import yblk.features.client.ItemRemoverClient;
import yblk.features.config.FeaturesConfig;
import yblk.features.config.FeaturesConfig.ItemBan;
import yblk.features.remover.ItemRemover;
import yblk.features.remover.ItemRemoverEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Sync item-remover toggle and per-item flags. C2S admin / S2C truth. */
public class ItemRemoverConfigPacket {
    private final boolean enabled;
    private final Map<String, ItemBan> bans;

    public ItemRemoverConfigPacket(boolean enabled, Map<String, ItemBan> bans) {
        this.enabled = enabled;
        this.bans = bans;
    }

    public static ItemRemoverConfigPacket current() {
        Map<String, ItemBan> copy = new HashMap<>();
        for (Map.Entry<String, ItemBan> entry : FeaturesConfig.itemBans.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return new ItemRemoverConfigPacket(FeaturesConfig.itemRemoverEnabled, copy);
    }

    public static void encode(ItemRemoverConfigPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
        buf.writeVarInt(packet.bans.size());
        for (Map.Entry<String, ItemBan> entry : packet.bans.entrySet()) {
            ItemBan ban = entry.getValue();
            buf.writeUtf(entry.getKey());
            buf.writeBoolean(ban.inventory);
            buf.writeBoolean(ban.recipes);
            buf.writeBoolean(ban.loot);
        }
    }

    public static ItemRemoverConfigPacket decode(FriendlyByteBuf buf) {
        boolean enabled = buf.readBoolean();
        int count = buf.readVarInt();
        Map<String, ItemBan> bans = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String id = buf.readUtf();
            ItemBan ban = new ItemBan();
            ban.inventory = buf.readBoolean();
            ban.recipes = buf.readBoolean();
            ban.loot = buf.readBoolean();
            if (id != null && !id.isBlank()) {
                bans.put(id.trim().toLowerCase(), ban);
            }
        }
        return new ItemRemoverConfigPacket(enabled, bans);
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
                FeaturesConfig.replaceBans(packet.bans, true);
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
            FeaturesConfig.replaceBans(packet.bans, false);
            ItemRemoverClient.onSynced();
        }
    }
}
