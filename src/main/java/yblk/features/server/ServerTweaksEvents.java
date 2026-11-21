package yblk.features.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;

@Mod.EventBusSubscriber(modid = Features.MODID)
public final class ServerTweaksEvents {
    private ServerTweaksEvents() {
    }

    @SubscribeEvent
    public static void onTabName(PlayerEvent.TabListNameFormat event) {
        if (!FeaturesConfig.serverTweaksEnabled || !FeaturesConfig.serverRolePrefix) {
            return;
        }
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        String role = ServerTweaks.roleName(player);
        Component prefix = null;
        if ("moderator".equals(role)) {
            prefix = Component.translatable("tab." + Features.MODID + ".role.moderator").withStyle(ChatFormatting.AQUA);
        } else if ("operator".equals(role)) {
            prefix = Component.translatable("tab." + Features.MODID + ".role.operator").withStyle(ChatFormatting.GOLD);
        }
        if (prefix != null) {
            event.setDisplayName(Component.empty().append(prefix).append(Component.literal(" ")).append(player.getDisplayName()));
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        ServerTweaks.tick(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChat(ServerChatEvent event) {
        if (event.getPlayer() != null && ServerTweaks.muted(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            ServerTweaks.onBreak(player, event.getState());
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerTweaks.onReach(player, player.distanceTo(event.getTarget()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        if (!FeaturesConfig.serverCheckFall || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.isCanceled() && "fall".equals(event.getSource().getMsgId())) {
            ServerTweaks.flag(player, "no-fall");
        }
    }
}
