package yblk.features.spawn;

import net.minecraft.resources.ResourceLocation;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;

public class MobSpawnBlockFeature extends Feature {
    public MobSpawnBlockFeature() {
        super(
                "mob_spawn_block",
                new ResourceLocation(Features.MODID, "textures/gui/features/mob_spawn_block.png")
        );
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.mobSpawnBlockEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        FeaturesConfig.setMobSpawnBlockEnabled(enabled);
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public boolean requiresOperator() {
        return true;
    }
}
