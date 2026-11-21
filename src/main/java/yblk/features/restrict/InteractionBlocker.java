package yblk.features.restrict;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;

public final class InteractionBlocker {
    private InteractionBlocker() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!active(event.getEntity(), event.getLevel())) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        boolean holding = !event.getItemStack().isEmpty();
        boolean menu = isMenuBlock(state, event.getLevel(), event.getPos());
        if (!InteractionRules.blocksUse(state, menu, holding)) {
            return;
        }
        denyBlock(event);
        tell((ServerPlayer) event.getEntity());
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!active(event.getEntity(), event.getLevel())) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!InteractionRules.blocksBreak(state)) {
            return;
        }
        event.setCanceled(true);
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        tell((ServerPlayer) event.getEntity());
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || event.getLevel().isClientSide()) {
            return;
        }
        if (!FeaturesConfig.interactionBlockerEnabled) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player) || bypass(player)) {
            return;
        }
        if (!InteractionRules.blocksBreak(event.getState())) {
            return;
        }
        event.setCanceled(true);
        tell(player);
    }

    public static void onEntityInteract(PlayerInteractEvent event, Entity target) {
        if (target == null || !active(event.getEntity(), event.getLevel())) {
            return;
        }
        boolean holding = !event.getItemStack().isEmpty();
        if (!InteractionRules.blocksInteract(target, opensMenu(target), holding)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        tell((ServerPlayer) event.getEntity());
    }

    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (!FeaturesConfig.interactionBlockerEnabled || !(player instanceof ServerPlayer serverPlayer) || bypass(serverPlayer)) {
            return;
        }
        if (event.getTarget() == null || !InteractionRules.blocksAttack(event.getTarget())) {
            return;
        }
        event.setCanceled(true);
        tell(serverPlayer);
    }

    private static boolean active(Player player, Level level) {
        if (level.isClientSide() || !FeaturesConfig.interactionBlockerEnabled) {
            return false;
        }
        return player instanceof ServerPlayer serverPlayer && !bypass(serverPlayer);
    }

    private static boolean bypass(ServerPlayer player) {
        return FeaturesConfig.interactionOpBypass && player.hasPermissions(2);
    }

    private static void denyBlock(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    private static boolean isMenuBlock(BlockState state, Level level, BlockPos pos) {
        if (state.getMenuProvider(level, pos) != null) {
            return true;
        }
        Block block = state.getBlock();
        return block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock;
    }

    private static boolean opensMenu(Entity entity) {
        return entity instanceof MenuProvider
                || entity instanceof AbstractVillager
                || entity instanceof AbstractHorse
                || entity instanceof ChestBoat
                || entity instanceof AbstractMinecartContainer;
    }

    private static void tell(ServerPlayer player) {
        String custom = FeaturesConfig.interactionMessage;
        Component message = custom == null || custom.isBlank()
                ? Component.translatable("screen." + Features.MODID + ".interaction_blocker.denied")
                : Component.literal(custom);
        RestrictionMessages.actionBar(player, message);
    }
}
