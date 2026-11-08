package yblk.features.border;

import net.minecraft.world.entity.Entity;
import yblk.features.config.FeaturesConfig;

public final class BorderDanger {
    private BorderDanger() {
    }

    public static boolean isActive() {
        return FeaturesConfig.borderDangerEnabled;
    }

    /** Blocks from the world border edge; inside the world this is positive. */
    public static double distanceToBorder(Entity entity) {
        return entity.level().getWorldBorder().getDistanceToBorder(entity);
    }

    /** True inside the lethal / effect radius. */
    public static boolean inHarmZone(Entity entity) {
        return isActive() && distanceToBorder(entity) < FeaturesConfig.borderDangerDistance;
    }

    /** True anywhere visuals or warnings may apply. */
    public static boolean inVisualRange(Entity entity) {
        return visualIntensity(entity) > 0.001f || warningStage(entity) != WarningStage.NONE;
    }

    /**
     * Visual FX intensity 0..1.
     * 0 at {@code visualDistance}, 1 at/inside {@code dangerDistance}.
     */
    public static float visualIntensity(Entity entity) {
        if (!isActive()) {
            return 0f;
        }
        double danger = FeaturesConfig.borderDangerDistance;
        double visual = Math.max(danger + 0.01, FeaturesConfig.borderVisualDistance);
        double dist = distanceToBorder(entity);
        if (dist >= visual) {
            return 0f;
        }
        if (dist <= danger) {
            return 1f;
        }
        float t = (float) ((visual - dist) / (visual - danger));
        return t * t * (3f - 2f * t);
    }

    /** Harm intensity — only inside the damage radius (full strength at the border). */
    public static float harmIntensity(Entity entity) {
        if (!isActive()) {
            return 0f;
        }
        double danger = FeaturesConfig.borderDangerDistance;
        double dist = distanceToBorder(entity);
        if (dist >= danger) {
            return 0f;
        }
        if (dist <= 0) {
            return 1f;
        }
        float t = (float) (1.0 - dist / danger);
        return t * t * (3f - 2f * t);
    }

    public static WarningStage warningStage(Entity entity) {
        if (!isActive() || !FeaturesConfig.borderShowWarning) {
            return WarningStage.NONE;
        }
        double dist = distanceToBorder(entity);
        if (dist < FeaturesConfig.borderDangerDistance) {
            return WarningStage.DANGER;
        }
        if (dist < FeaturesConfig.borderApproachDistance) {
            return WarningStage.APPROACH;
        }
        return WarningStage.NONE;
    }

    public static boolean hasFx(BorderZoneFx fx) {
        return FeaturesConfig.borderZoneFx.contains(fx.configKey());
    }

    public enum WarningStage {
        NONE,
        APPROACH,
        DANGER
    }
}
