package yblk.features.border;

import net.minecraft.resources.ResourceLocation;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;

public class BorderDangerFeature extends Feature {
    public BorderDangerFeature() {
        super(
                "border_danger",
                new ResourceLocation(Features.MODID, "textures/gui/features/border_danger.png")
        );
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.borderDangerEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        FeaturesConfig.setBorderDangerEnabled(enabled);
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
