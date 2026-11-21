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
        CHANNEL.messageBuilder(PresetPacket.class, id++)
                .encoder(PresetPacket::encode)
                .decoder(PresetPacket::decode)
                .consumerMainThread(PresetPacket::handle)
                .add();
        CHANNEL.messageBuilder(CommandBlockerPacket.class, id++)
                .encoder(CommandBlockerPacket::encode)
                .decoder(CommandBlockerPacket::decode)
                .consumerMainThread(CommandBlockerPacket::handle)
                .add();
        CHANNEL.messageBuilder(InteractionBlockerPacket.class, id++)
                .encoder(InteractionBlockerPacket::encode)
                .decoder(InteractionBlockerPacket::decode)
                .consumerMainThread(InteractionBlockerPacket::handle)
                .add();
        CHANNEL.messageBuilder(ToolRestrictionsPacket.class, id++)
                .encoder(ToolRestrictionsPacket::encode)
                .decoder(ToolRestrictionsPacket::decode)
                .consumerMainThread(ToolRestrictionsPacket::handle)
                .add();
        CHANNEL.messageBuilder(PortalLockPacket.class, id++)
                .encoder(PortalLockPacket::encode)
                .decoder(PortalLockPacket::decode)
                .consumerMainThread(PortalLockPacket::handle)
                .add();
        CHANNEL.messageBuilder(ElytraRulesPacket.class, id++)
                .encoder(ElytraRulesPacket::encode)
                .decoder(ElytraRulesPacket::decode)
                .consumerMainThread(ElytraRulesPacket::handle)
                .add();
        CHANNEL.messageBuilder(MobDropsPacket.class, id++)
                .encoder(MobDropsPacket::encode)
                .decoder(MobDropsPacket::decode)
                .consumerMainThread(MobDropsPacket::handle)
                .add();
        CHANNEL.messageBuilder(VillagerTradesPacket.class, id++)
                .encoder(VillagerTradesPacket::encode)
                .decoder(VillagerTradesPacket::decode)
                .consumerMainThread(VillagerTradesPacket::handle)
                .add();
        CHANNEL.messageBuilder(ItemWeightPacket.class, id++)
                .encoder(ItemWeightPacket::encode)
                .decoder(ItemWeightPacket::decode)
                .consumerMainThread(ItemWeightPacket::handle)
                .add();
        CHANNEL.messageBuilder(ServerTweaksPacket.class, id++)
                .encoder(ServerTweaksPacket::encode)
                .decoder(ServerTweaksPacket::decode)
                .consumerMainThread(ServerTweaksPacket::handle)
                .add();
        CHANNEL.messageBuilder(ServerActionPacket.class, id++)
                .encoder(ServerActionPacket::encode)
                .decoder(ServerActionPacket::decode)
                .consumerMainThread(ServerActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(ServerIconPacket.class, id++)
                .encoder(ServerIconPacket::encode)
                .decoder(ServerIconPacket::decode)
                .consumerMainThread(ServerIconPacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
