package yblk.features.rfp;

import net.minecraft.resources.ResourceLocation;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;

public class RealFirstPersonFeature extends Feature {
    public RealFirstPersonFeature() {
        super(
                "real_first_person",
                new ResourceLocation(Features.MODID, "textures/gui/features/real_first_person.png")
        );
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.realFirstPersonEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        FeaturesConfig.setRealFirstPersonEnabled(enabled);
    }

    @Override
    public boolean hasSettings() {
        return true;
    }
}
