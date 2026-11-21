package yblk.features.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.resources.ResourceLocation;
import yblk.features.network.ModNetwork;

public final class WorldEvents {
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WorldSync.sendTo(player);
            yblk.features.mobs.MobsSync.sendTo(player);
            ModNetwork.sendToPlayer(player, yblk.features.network.ItemWeightPacket.current());
            ModNetwork.sendToPlayer(player, yblk.features.network.ServerTweaksPacket.current(
                    yblk.features.server.ServerTweaks.firstRules(player)));
            yblk.features.server.ServerTweaks.onLogin(player);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        WorldMessages.forget(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            FlightRules.restore(player);
            FlightRules.forget(player.getUUID());
            yblk.features.mobs.TradeLock.forget(player.getUUID());
            yblk.features.server.ServerTweaks.onLogout(player);
        }
    }

    @SubscribeEvent
    public void onTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel)) {
            return;
        }
        ResourceLocation destination = event.getDimension().location();
        boolean blocked = false;
        for (ServerPlayer player : PortalLock.riders(event.getEntity())) {
            if (PortalLock.blocksTravel(player, destination)) {
                PortalLock.tell(player, destination);
                blocked = true;
            }
        }
        if (blocked) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ResourceLocation destination = PortalLock.netherDestination(level);
        if (!PortalLock.blocksCreation(level, event.getPos(), destination)) {
            return;
        }
        event.setCanceled(true);
        double x = event.getPos().getX() + 0.5;
        double y = event.getPos().getY() + 0.5;
        double z = event.getPos().getZ() + 0.5;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= 100.0 && PortalLock.blocksTravel(player, destination)) {
                PortalLock.tell(player, destination);
            }
        }
    }

    @SubscribeEvent
    public void onUseItem(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!event.getItemStack().is(Items.ENDER_EYE)) {
            return;
        }
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.END_PORTAL_FRAME)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ResourceLocation end = Level.END.location();
        if (!PortalLock.blocksCreation(level, event.getPos(), end)) {
            return;
        }
        event.setCanceled(true);
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCancellationResult(InteractionResult.FAIL);
        if (event.getEntity() instanceof ServerPlayer player && PortalLock.blocksTravel(player, end)) {
            PortalLock.tell(player, end);
        }
    }

    @SubscribeEvent
    public void onFirework(PlayerInteractEvent.RightClickItem event) {
        if (!event.getItemStack().is(Items.FIREWORK_ROCKET)) {
            return;
        }
        if (!FlightRules.deniesFirework(event.getEntity())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            FlightRules.tell(player, FlightRules.Deny.FIREWORK, null);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            FlightRules.tick(player);
        }
    }
}
