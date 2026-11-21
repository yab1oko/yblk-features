package yblk.features.restrict;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class RestrictionsEvents {
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RestrictionsSync.sendTo(player);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        RestrictionMessages.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        CommandBlocker.onCommand(event);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        InteractionBlocker.onRightClickBlock(event);
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        InteractionBlocker.onLeftClickBlock(event);
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        InteractionBlocker.onEntityInteract(event, event.getTarget());
    }

    @SubscribeEvent
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        InteractionBlocker.onEntityInteract(event, event.getTarget());
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        InteractionBlocker.onAttack(event);
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        InteractionBlocker.onBreak(event);
        ToolRestrictions.onBreak(event);
    }
}
