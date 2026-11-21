package yblk.features.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import yblk.features.Features;
import yblk.features.preset.PresetStore;

import java.util.function.Supplier;

/** OP sends a preset snapshot. The server applies it and pushes the same snapshot to every client. */
public class PresetPacket {
    private static final int MAX = 262144;
    private final String json;

    public PresetPacket(String json) {
        this.json = json == null ? "" : json;
    }

    public static void encode(PresetPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.json, MAX);
    }

    public static PresetPacket decode(FriendlyByteBuf buf) {
        return new PresetPacket(buf.readUtf(MAX));
    }

    public static void handle(PresetPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender == null || !sender.hasPermissions(2)) {
                    if (sender != null) {
                        sender.displayClientMessage(Component.translatable(
                                "screen." + Features.MODID + ".presets.denied"), true);
                    }
                    return;
                }
                if (!PresetStore.apply(packet.json)) {
                    return;
                }
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new PresetPacket(packet.json));
            } else {
                PresetStore.apply(packet.json);
            }
        });
        context.setPacketHandled(true);
    }
}
