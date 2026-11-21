package yblk.features.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Server-side portal travel and, when asked, portal creation. */
public final class PortalLock {
    private PortalLock() {
    }

    public static boolean blocksTravel(ServerPlayer player, ResourceLocation destination) {
        if (!FeaturesConfig.portalLockEnabled || player == null || destination == null) {
            return false;
        }
        if (bypasses(player)) {
            return false;
        }
        return closed(destination, player.server);
    }

    public static boolean blocksCreation(ServerLevel level, BlockPos pos, ResourceLocation destination) {
        if (!FeaturesConfig.portalLockEnabled || !FeaturesConfig.portalBlockCreation || level == null || destination == null) {
            return false;
        }
        if (!closed(destination, level.getServer())) {
            return false;
        }
        return !(FeaturesConfig.portalOpBypass && nearbyOp(level, pos));
    }

    public static void tell(ServerPlayer player, ResourceLocation destination) {
        String label = PortalRules.label(destination);
        String custom = FeaturesConfig.portalMessage;
        Component message = custom == null || custom.isBlank()
                ? Component.translatable("screen." + Features.MODID + ".portal_lock.denied", label)
                : Component.literal(custom.replace("{dimension}", label));
        WorldMessages.actionBar(player, message);
    }

    public static ResourceLocation netherDestination(ServerLevel level) {
        ResourceKey<Level> here = level.dimension();
        return Level.NETHER.equals(here) ? Level.OVERWORLD.location() : Level.NETHER.location();
    }

    public static List<ServerPlayer> riders(Entity entity) {
        List<ServerPlayer> players = new ArrayList<>();
        collect(entity, players);
        return players;
    }

    private static void collect(Entity entity, List<ServerPlayer> into) {
        if (entity instanceof ServerPlayer player) {
            into.add(player);
        }
        for (Entity passenger : entity.getPassengers()) {
            collect(passenger, into);
        }
    }

    private static boolean bypasses(ServerPlayer player) {
        return FeaturesConfig.portalOpBypass && player.hasPermissions(2);
    }

    private static boolean closed(ResourceLocation destination, net.minecraft.server.MinecraftServer server) {
        PortalRules.Rule rule = PortalRules.find(FeaturesConfig.portalRules, destination);
        long day = 0L;
        if (server != null) {
            day = server.overworld().getDayTime() / 24000L;
        }
        return PortalRules.closed(rule, day, LocalDate.now());
    }

    private static boolean nearbyOp(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        for (ServerPlayer player : level.players()) {
            if (player.hasPermissions(2) && player.distanceToSqr(x, y, z) <= 100.0) {
                return true;
            }
        }
        return false;
    }
}
