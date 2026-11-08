package yblk.features.night;

import net.minecraft.resources.ResourceLocation;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;

public class TrueNightFeature extends Feature {
    public TrueNightFeature() {
        super(
                "true_night",
                new ResourceLocation(Features.MODID, "textures/gui/features/true_night.png")
        );
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.trueNightEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        FeaturesConfig.setTrueNightEnabled(enabled);
    }

    @Override
    public boolean hasSettings() {
        return true;
    }
}
