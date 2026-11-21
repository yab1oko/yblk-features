package yblk.features.world;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Elytra, firework boost, glide time, and creative-style flight, per dimension. */
public final class FlightRules {
    public enum Deny {
        ELYTRA, TIME, FIREWORK, FLIGHT
    }

    private record Saved(boolean mayfly, boolean flying) {
    }

    private static final Map<UUID, Saved> SAVED = new HashMap<>();
    /** Forced down by the time limit; cleared once the player actually lands. */
    private static final Set<UUID> MUST_LAND = new HashSet<>();
    private static final Set<UUID> CLIENT_GROUNDED = new HashSet<>();

    private FlightRules() {
    }

    public static void tick(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (player.onGround() || player.isInWater() || player.isPassenger()) {
            MUST_LAND.remove(player.getUUID());
        }
        if (!FeaturesConfig.elytraRulesEnabled) {
            restore(player);
            return;
        }
        if (bypasses(player)) {
            restore(player);
            return;
        }
        ElytraRules.Rule rule = rule(player);
        if (rule == null) {
            restore(player);
            return;
        }
        if (!rule.elytra() && player.isFallFlying()) {
            stopGlide(player);
            tell(player, Deny.ELYTRA, rule);
        } else if (rule.elytra() && overTime(player, rule)) {
            stopGlide(player);
            MUST_LAND.add(player.getUUID());
            tell(player, Deny.TIME, rule);
        }
        if (!rule.flight()) {
            groundFlight(player);
        } else {
            restore(player);
        }
    }

    public static boolean deniesElytraStart(ServerPlayer player) {
        if (!blocksGlide(player)) {
            return false;
        }
        ElytraRules.Rule rule = rule(player);
        boolean timed = rule != null && rule.elytra();
        tell(player, timed ? Deny.TIME : Deny.ELYTRA, rule);
        return true;
    }

    private static boolean bypasses(Player player) {
        return FeaturesConfig.elytraOpBypass && player.hasPermissions(2);
    }

    public static boolean deniesFirework(Player player) {
        if (!FeaturesConfig.elytraRulesEnabled || player == null || !player.isFallFlying()) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer && bypasses(serverPlayer)) {
            return false;
        }
        if (player.level().isClientSide() && FeaturesConfig.elytraOpBypass && player.hasPermissions(2)) {
            return false;
        }
        ElytraRules.Rule rule = ElytraRules.find(FeaturesConfig.elytraRules, player.level().dimension().location());
        return rule != null && !rule.firework();
    }

    public static boolean deniesCreativeFlight(ServerPlayer player) {
        if (!active(player) || player.isCreative() || player.isSpectator()) {
            return false;
        }
        ElytraRules.Rule rule = rule(player);
        return rule != null && !rule.flight();
    }

    /** Client and server. True when this player must not be gliding right now. */
    public static boolean blocksGlide(Player player) {
        if (!FeaturesConfig.elytraRulesEnabled || player == null || bypasses(player)) {
            return false;
        }
        if (player.level().isClientSide() && CLIENT_GROUNDED.contains(player.getUUID())) {
            return true;
        }
        if (player instanceof ServerPlayer && MUST_LAND.contains(player.getUUID())) {
            return true;
        }
        ElytraRules.Rule rule = ElytraRules.find(FeaturesConfig.elytraRules, player.level().dimension().location());
        if (rule == null) {
            return false;
        }
        if (!rule.elytra()) {
            return true;
        }
        return rule.seconds() > 0 && player.isFallFlying() && player.getFallFlyingTicks() >= rule.seconds() * 20;
    }

    public static void clientTick(Player player) {
        if (player == null || !player.level().isClientSide()) {
            return;
        }
        if (player.onGround() || player.isInWater() || player.isPassenger()) {
            CLIENT_GROUNDED.remove(player.getUUID());
        }
        if (!player.isFallFlying() || !blocksGlide(player)) {
            return;
        }
        ElytraRules.Rule rule = ElytraRules.find(FeaturesConfig.elytraRules, player.level().dimension().location());
        if (rule != null && rule.elytra() && rule.seconds() > 0) {
            CLIENT_GROUNDED.add(player.getUUID());
        }
        player.stopFallFlying();
    }

    public static void stopGlide(ServerPlayer player) {
        player.stopFallFlying();
    }

    public static void groundFlight(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            SAVED.remove(player.getUUID());
            return;
        }
        Abilities abilities = player.getAbilities();
        SAVED.putIfAbsent(player.getUUID(), new Saved(abilities.mayfly, abilities.flying));
        if (abilities.mayfly || abilities.flying) {
            abilities.mayfly = false;
            abilities.flying = false;
            player.onUpdateAbilities();
        }
    }

    public static void restore(ServerPlayer player) {
        Saved saved = SAVED.remove(player.getUUID());
        if (saved == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        Abilities abilities = player.getAbilities();
        abilities.mayfly = saved.mayfly;
        abilities.flying = saved.flying;
        player.onUpdateAbilities();
    }

    public static void forget(UUID id) {
        if (id == null) {
            return;
        }
        SAVED.remove(id);
        MUST_LAND.remove(id);
    }

    public static void tell(ServerPlayer player, Deny deny, ElytraRules.Rule rule) {
        if (player == null || deny == null) {
            return;
        }
        String label = PortalRules.label(player.level().dimension().location());
        int seconds = rule == null ? 0 : rule.seconds();
        String custom = FeaturesConfig.elytraMessage;
        Component message;
        if (custom != null && !custom.isBlank()) {
            message = Component.literal(custom
                    .replace("{dimension}", label)
                    .replace("{seconds}", Integer.toString(seconds)));
        } else {
            String key = switch (deny) {
                case TIME -> "screen." + Features.MODID + ".elytra_rules.denied.time";
                case FIREWORK -> "screen." + Features.MODID + ".elytra_rules.denied.firework";
                case FLIGHT -> "screen." + Features.MODID + ".elytra_rules.denied.flight";
                case ELYTRA -> "screen." + Features.MODID + ".elytra_rules.denied.elytra";
            };
            message = Component.translatable(key, label, seconds);
        }
        WorldMessages.actionBar(player, message);
    }

    private static boolean overTime(ServerPlayer player, ElytraRules.Rule rule) {
        return rule.seconds() > 0 && player.isFallFlying() && player.getFallFlyingTicks() >= rule.seconds() * 20;
    }

    private static boolean active(ServerPlayer player) {
        return FeaturesConfig.elytraRulesEnabled && player != null && !bypasses(player);
    }

    private static ElytraRules.Rule rule(ServerPlayer player) {
        ResourceLocation dimension = player.level().dimension().location();
        return ElytraRules.find(FeaturesConfig.elytraRules, dimension);
    }
}
