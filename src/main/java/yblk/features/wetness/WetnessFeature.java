package yblk.features.wetness;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;
import yblk.features.network.ModNetwork;
import yblk.features.network.WetnessConfigPacket;

public class WetnessFeature extends Feature {
    public WetnessFeature() {
        super(
                "wetness",
                new ResourceLocation(Features.MODID, "textures/gui/features/wetness.png")
        );
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.wetnessEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setWetnessEnabled(enabled);
        ModNetwork.sendToServer(WetnessConfigPacket.current());
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public boolean requiresOperator() {
        return true;
    }

    @Override
    public boolean canPlayerConfigure() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return true;
        }
        return WetnessAccess.canEdit(Minecraft.getInstance().player);
    }
}
