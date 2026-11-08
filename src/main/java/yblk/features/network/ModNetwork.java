package yblk.features.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import yblk.features.Features;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Features.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private static int id;

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(SyncInventoryRevampPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncInventoryRevampPacket::encode)
                .decoder(SyncInventoryRevampPacket::decode)
                .consumerMainThread(SyncInventoryRevampPacket::handle)
                .add();
        // Bidirectional: client admins push changes; server pushes truth on login / after apply.
        CHANNEL.messageBuilder(LocalChatConfigPacket.class, id++)
                .encoder(LocalChatConfigPacket::encode)
                .decoder(LocalChatConfigPacket::decode)
                .consumerMainThread(LocalChatConfigPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncWetnessPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncWetnessPacket::encode)
                .decoder(SyncWetnessPacket::decode)
                .consumerMainThread(SyncWetnessPacket::handle)
                .add();
        CHANNEL.messageBuilder(WetnessConfigPacket.class, id++)
                .encoder(WetnessConfigPacket::encode)
                .decoder(WetnessConfigPacket::decode)
                .consumerMainThread(WetnessConfigPacket::handle)
                .add();
        CHANNEL.messageBuilder(ItemRemoverConfigPacket.class, id++)
                .encoder(ItemRemoverConfigPacket::encode)
                .decoder(ItemRemoverConfigPacket::decode)
                .consumerMainThread(ItemRemoverConfigPacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
