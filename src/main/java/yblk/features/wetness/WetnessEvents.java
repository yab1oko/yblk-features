package yblk.features.wetness;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import yblk.features.config.FeaturesConfig;
import yblk.features.network.ModNetwork;
import yblk.features.network.SyncWetnessPacket;
import yblk.features.network.WetnessConfigPacket;

public class WetnessEvents {
    private static final int SYNC_INTERVAL = 20;

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.sendToPlayer(player, WetnessConfigPacket.current());
            sync(player);
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WetnessLogic.clearMovementModifier(player);
            sync(player);
        }
    }

    @SubscribeEvent
    public void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Wetness wet = WetnessCapability.get(player);
        if (!WetnessLogic.isActive() || player.isCreative() || player.isSpectator()) {
            if (wet.isWet() || wet.getWetLandSeconds() > 0) {
                wet.set(0f);
                wet.setWetLandSeconds(0);
                wet.setWasInWater(false);
                sync(serverPlayer);
            }
            WetnessLogic.clearMovementModifier(player);
            return;
        }

        // Accumulate in 1-second steps (20 ticks) for stable %/s rates.
        if (player.tickCount % 20 != 0) {
            WetnessLogic.updateMovementModifier(player, wet);
            return;
        }

        float before = wet.get();
        int landBefore = wet.getWetLandSeconds();
        boolean submerged = WetnessLogic.isFullySubmerged(player);
        boolean inWater = WetnessLogic.isInWater(player);
        boolean precipitating = WetnessLogic.isPrecipitatingOn(player);

        if (submerged) {
            // Fully underwater — soak instantly.
            wet.set(Wetness.MAX);
            wet.setWasInWater(true);
            wet.setWetLandSeconds(0);
        } else if (inWater) {
            // Wading / surface swim — ~2× open-sky rain/snow.
            wet.add(FeaturesConfig.wetnessRainRate * 2f);
            wet.setWasInWater(true);
            wet.setWetLandSeconds(0);
        } else {
            wet.setWasInWater(false);
            if (precipitating) {
                wet.add(FeaturesConfig.wetnessRainRate);
            } else {
                float dry = FeaturesConfig.wetnessDryRate;
                float heat = WetnessLogic.heatDryRate(player);
                wet.add(-(Math.max(dry, heat)));
            }

            if (wet.isWet()) {
                wet.setWetLandSeconds(wet.getWetLandSeconds() + 1);
                if (wet.getWetLandSeconds() >= FeaturesConfig.wetnessFreezeSeconds) {
                    applyFreeze(player);
                }
            } else {
                wet.setWetLandSeconds(0);
            }
        }

        WetnessLogic.updateMovementModifier(player, wet);

        boolean changed = Math.abs(wet.get() - before) > 0.05f
                || wet.getWetLandSeconds() != landBefore
                || player.tickCount % (SYNC_INTERVAL * 5) == 0;
        if (changed) {
            sync(serverPlayer);
        }
    }

    private static void applyFreeze(Player player) {
        int target = player.getTicksRequiredToFreeze() + 40;
        if (player.getTicksFrozen() < target) {
            player.setTicksFrozen(Math.min(target, player.getTicksFrozen() + 25));
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
    }

    public static void sync(ServerPlayer player) {
        Wetness wet = WetnessCapability.get(player);
        ModNetwork.sendToPlayer(player, new SyncWetnessPacket(wet.get(), wet.getWetLandSeconds(), wet.wasInWater()));
    }
}
