package yblk.features.night;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
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
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setTrueNightEnabled(enabled);
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public boolean canPlayerConfigure() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return true;
        }
        return TrueNight.canEdit(Minecraft.getInstance().player);
    }
}
