package yblk.features.wetness;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import yblk.features.config.FeaturesConfig;

import java.util.UUID;

/** Shared wetness rate / armor / heat helpers. */
public final class WetnessLogic {
    public static final UUID SLOW_UUID = UUID.fromString("a7c4e91b-2f18-4d6a-9e33-0b8c5d1a4f72");
    public static final String SLOW_NAME = "features_wetness_armor_slow";

    private WetnessLogic() {
    }

    public static boolean isActive() {
        return FeaturesConfig.wetnessEnabled;
    }

    /** 0 = bare, 1 = all four armor slots filled. */
    public static float armorCoverage(Player player) {
        int pieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                pieces++;
            }
        }
        return pieces / 4f;
    }

    public static boolean wearsArmor(Player player) {
        return armorCoverage(player) > 0.01f;
    }

    /** Standing / swimming in water with head above surface. */
    public static boolean isInWater(Player player) {
        return player.isInWater() || player.isEyeInFluid(FluidTags.WATER);
    }

    /** Fully submerged (head underwater). Instant soak. */
    public static boolean isFullySubmerged(Player player) {
        return player.isUnderWater();
    }

    /**
     * Open-sky precipitation (rain or snow). Vanilla {@code isRainingAt} only
     * matches rain biomes, so snow must be checked separately.
     */
    public static boolean isPrecipitatingOn(Player player) {
        Level level = player.level();
        if (!level.isRaining()) {
            return false;
        }
        BlockPos pos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        if (!level.canSeeSky(pos)) {
            return false;
        }
        if (level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            return false;
        }
        Biome.Precipitation precip = level.getBiome(pos).value().getPrecipitationAt(pos);
        return precip == Biome.Precipitation.RAIN || precip == Biome.Precipitation.SNOW;
    }

    /** @deprecated use {@link #isPrecipitatingOn(Player)} */
    @Deprecated
    public static boolean isRainingOn(Player player) {
        return isPrecipitatingOn(player);
    }

    /**
     * Strongest heat dry rate (%/s). 0 if none.
     * Nearby: torch / campfire-fire / lava (config).
     * Standing in fire: ×5. Standing in lava: ×10.
     */
    public static float heatDryRate(Player player) {
        Level level = player.level();
        BlockPos origin = player.blockPosition();
        int radius = FeaturesConfig.wetnessHeatRadius;
        float best = 0f;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) {
                        continue;
                    }
                    BlockPos pos = origin.offset(dx, dy, dz);
                    float rate = heatOf(level.getBlockState(pos));
                    if (rate > best) {
                        best = rate;
                    }
                }
            }
        }

        // Direct contact: much stronger drying.
        if (player.isInLava()) {
            return Math.max(best, FeaturesConfig.wetnessHeatLava) * 10f;
        }
        if (player.isOnFire() || isStandingInFire(player)) {
            return Math.max(best, FeaturesConfig.wetnessHeatFire) * 5f;
        }
        return best;
    }

    private static boolean isStandingInFire(Player player) {
        BlockState feet = player.level().getBlockState(player.blockPosition());
        BlockState body = player.level().getBlockState(BlockPos.containing(player.getX(), player.getY() + 0.5, player.getZ()));
        return isFireBlock(feet) || isFireBlock(body);
    }

    private static boolean isFireBlock(BlockState state) {
        return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                || ((state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                && (!state.hasProperty(CampfireBlock.LIT) || state.getValue(CampfireBlock.LIT)));
    }

    private static float heatOf(BlockState state) {
        if (state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)) {
            return FeaturesConfig.wetnessHeatLava;
        }
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
            if (state.hasProperty(CampfireBlock.LIT) && !state.getValue(CampfireBlock.LIT)) {
                return 0f;
            }
            return FeaturesConfig.wetnessHeatFire;
        }
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN)) {
            return FeaturesConfig.wetnessHeatTorch;
        }
        return 0f;
    }

    public static void updateMovementModifier(Player player, Wetness wet) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        AttributeModifier existing = speed.getModifier(SLOW_UUID);
        boolean shouldSlow = isActive() && wet.isWet() && wearsArmor(player);
        if (shouldSlow) {
            double amount = -FeaturesConfig.wetnessArmorSlow;
            if (existing == null || Math.abs(existing.getAmount() - amount) > 0.0001) {
                speed.removeModifier(SLOW_UUID);
                speed.addTransientModifier(new AttributeModifier(
                        SLOW_UUID, SLOW_NAME, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        } else if (existing != null) {
            speed.removeModifier(SLOW_UUID);
        }
    }

    public static void clearMovementModifier(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SLOW_UUID);
        }
    }
}
