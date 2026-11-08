package yblk.features.spawn;

import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import yblk.features.config.FeaturesConfig;

public class MobSpawnEvents {
    @SubscribeEvent
    public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!FeaturesConfig.mobSpawnBlockEnabled) {
            return;
        }
        if (!shouldBlockSpawnType(event.getSpawnType())) {
            return;
        }
        if (MobSpawnCatalog.isBlocked(event.getEntity().getType())) {
            event.setSpawnCancelled(true);
        }
    }

    private static boolean shouldBlockSpawnType(MobSpawnType type) {
        if (FeaturesConfig.mobSpawnBlockAllSources) {
            return true;
        }
        return switch (type) {
            case SPAWN_EGG, COMMAND, BUCKET, DISPENSER, CONVERSION, BREEDING -> false;
            default -> true;
        };
    }
}
