package yblk.features.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.network.LocalChatConfigPacket;
import yblk.features.network.ModNetwork;

/**
 * Local chat by default (range). Prefix {@code !} sends globally.
 * Admins with bypass see every local message map-wide.
 */
public class LocalChatEvents {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onServerChat(ServerChatEvent event) {
        if (!LocalChat.isActive()) {
            return;
        }

        ServerPlayer sender = event.getPlayer();
        String raw = event.getRawText();
        if (raw == null) {
            return;
        }

        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        boolean global = trimmed.startsWith("!");
        String body = global ? trimmed.substring(1).stripLeading() : trimmed;
        if (body.isEmpty()) {
            // Bare "!" — swallow so it doesn't spam empty chat.
            event.setCanceled(true);
            return;
        }

        event.setCanceled(true);

        Component message = formatMessage(sender, body, global);
        if (global) {
            broadcastAll(sender, message);
        } else {
            broadcastLocal(sender, message);
        }
    }

    private static Component formatMessage(ServerPlayer sender, String body, boolean global) {
        MutableComponent prefix = Component.translatable(
                        global ? "chat." + Features.MODID + ".global" : "chat." + Features.MODID + ".local")
                .withStyle(global ? ChatFormatting.GOLD : ChatFormatting.DARK_AQUA);
        MutableComponent name = sender.getDisplayName().copy().withStyle(ChatFormatting.WHITE);
        MutableComponent text = Component.literal(body)
                .withStyle(global ? ChatFormatting.WHITE : ChatFormatting.GRAY);

        return Component.empty()
                .append(prefix)
                .append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                .append(name)
                .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                .append(text);
    }

    private static void broadcastAll(ServerPlayer sender, Component message) {
        for (ServerPlayer player : sender.server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
        sender.server.sendSystemMessage(message);
    }

    private static void broadcastLocal(ServerPlayer sender, Component message) {
        double range = Math.max(1, FeaturesConfig.localChatRange);
        double rangeSq = range * range;

        for (ServerPlayer player : sender.server.getPlayerList().getPlayers()) {
            if (player == sender || canHearLocal(sender, player, rangeSq)) {
                player.sendSystemMessage(message);
            }
        }
        // Always log locals on the server console.
        sender.server.sendSystemMessage(message);
    }

    private static boolean canHearLocal(ServerPlayer sender, ServerPlayer receiver, double rangeSq) {
        if (LocalChat.canBypass(receiver)) {
            return true;
        }
        if (receiver.level() != sender.level()) {
            return false;
        }
        return receiver.distanceToSqr(sender) <= rangeSq;
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.sendToPlayer(player, LocalChatConfigPacket.current());
        }
    }
}
