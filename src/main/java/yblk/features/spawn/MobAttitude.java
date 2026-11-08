package yblk.features.spawn;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.Set;

/**
 * Coarse attitude buckets for the mob-spawn settings UI.
 * Neutral overrides known "monster" types that only attack when provoked.
 */
public enum MobAttitude {
    HOSTILE,
    NEUTRAL,
    PASSIVE;

    private static final Set<ResourceLocation> NEUTRAL_IDS = Set.of(
            new ResourceLocation("minecraft", "enderman"),
            new ResourceLocation("minecraft", "zombified_piglin"),
            new ResourceLocation("minecraft", "piglin"),
            new ResourceLocation("minecraft", "spider"),
            new ResourceLocation("minecraft", "cave_spider"),
            new ResourceLocation("minecraft", "wolf"),
            new ResourceLocation("minecraft", "bee"),
            new ResourceLocation("minecraft", "llama"),
            new ResourceLocation("minecraft", "trader_llama"),
            new ResourceLocation("minecraft", "polar_bear"),
            new ResourceLocation("minecraft", "panda"),
            new ResourceLocation("minecraft", "dolphin"),
            new ResourceLocation("minecraft", "goat"),
            new ResourceLocation("minecraft", "iron_golem"),
            new ResourceLocation("minecraft", "snow_golem")
    );

    public static MobAttitude of(EntityType<?> type, ResourceLocation id) {
        if (id != null && NEUTRAL_IDS.contains(id)) {
            return NEUTRAL;
        }
        MobCategory category = type.getCategory();
        if (category == MobCategory.MONSTER) {
            return HOSTILE;
        }
        return PASSIVE;
    }
}
