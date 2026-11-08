package yblk.features.names;

import net.minecraft.resources.ResourceLocation;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;

public class HideNamesFeature extends Feature {
    public HideNamesFeature() {
        super(
                "hide_names",
                new ResourceLocation(Features.MODID, "textures/gui/features/hide_names.png")
        );
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.hideNamesEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        FeaturesConfig.setHideNamesEnabled(enabled);
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public boolean canPlayerConfigure() {
        // Master toggle + pure hide are available to everyone.
        return true;
    }
}
