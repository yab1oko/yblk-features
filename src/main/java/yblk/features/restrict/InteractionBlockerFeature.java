package yblk.features.restrict;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;
import yblk.features.network.InteractionBlockerPacket;
import yblk.features.network.ModNetwork;

public class InteractionBlockerFeature extends Feature {
    public InteractionBlockerFeature() {
        super(
                "interaction_blocker",
                new ResourceLocation(Features.MODID, "textures/gui/features/interaction_blocker.png")
        );
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.interactionBlockerEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setInteractionBlockerEnabled(enabled);
        ModNetwork.sendToServer(InteractionBlockerPacket.current());
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
        return Restrictions.canEdit(Minecraft.getInstance().player);
    }
}
