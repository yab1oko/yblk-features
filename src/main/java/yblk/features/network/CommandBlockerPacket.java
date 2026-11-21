package yblk.features.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import yblk.features.config.FeaturesConfig;
import yblk.features.restrict.CommandBlocker;
import yblk.features.restrict.Restrictions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Command-blocker toggle, rules, and deny message. C2S admin / S2C truth. */
public class CommandBlockerPacket {
    private final boolean enabled;
    private final List<String> rules;
    private final String message;

    public CommandBlockerPacket(boolean enabled, List<String> rules, String message) {
        this.enabled = enabled;
        this.rules = rules;
        this.message = message == null ? "" : message;
    }

    public static CommandBlockerPacket current() {
        return new CommandBlockerPacket(
                FeaturesConfig.commandBlockerEnabled,
                new ArrayList<>(FeaturesConfig.commandRules),
                FeaturesConfig.commandDenyMessage
        );
    }

    public static void encode(CommandBlockerPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
        writeList(buf, packet.rules);
        buf.writeUtf(packet.message, 256);
    }

    public static CommandBlockerPacket decode(FriendlyByteBuf buf) {
        return new CommandBlockerPacket(buf.readBoolean(), readList(buf), buf.readUtf(256));
    }

    public static void handle(CommandBlockerPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender == null || !Restrictions.canEdit(sender)) {
                    return;
                }
                FeaturesConfig.applyCommandBlocker(packet.enabled, packet.rules, packet.message, true);
                CommandBlocker.resend(sender.server);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), current());
            } else {
                FeaturesConfig.applyCommandBlocker(packet.enabled, packet.rules, packet.message, false);
            }
        });
        context.setPacketHandled(true);
    }

    public static void writeList(FriendlyByteBuf buf, List<String> list) {
        buf.writeVarInt(list.size());
        for (String entry : list) {
            buf.writeUtf(entry == null ? "" : entry, 256);
        }
    }

    public static List<String> readList(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 512) {
            throw new IllegalArgumentException("rule list");
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String entry = buf.readUtf(256);
            if (entry != null && !entry.isBlank()) {
                out.add(entry);
            }
        }
        return out;
    }
}
