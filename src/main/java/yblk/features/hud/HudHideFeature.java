package yblk.features.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;

public class HudHideFeature extends Feature {
    public HudHideFeature() {
        super(
                "hud_hide",
                new ResourceLocation(Features.MODID, "textures/gui/features/hud_hide.png")
        );
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.hudHideEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setHudHideEnabled(enabled);
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
        return HudHide.canEdit(Minecraft.getInstance().player);
    }
}
