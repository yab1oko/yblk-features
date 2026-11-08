package yblk.features.f3;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;

public class F3HideFeature extends Feature {
    public F3HideFeature() {
        super(
                "f3_hide",
                new ResourceLocation(Features.MODID, "textures/gui/features/f3_hide.png")
        );
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.f3HideEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setF3HideEnabled(enabled);
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
        return F3Hide.canEdit(Minecraft.getInstance().player);
    }
}
