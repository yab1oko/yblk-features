package yblk.features.world;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;
import yblk.features.network.ElytraRulesPacket;
import yblk.features.network.ModNetwork;
import yblk.features.restrict.Restrictions;

public class ElytraRulesFeature extends Feature {
    public ElytraRulesFeature() {
        super("elytra_rules", new ResourceLocation(Features.MODID, "textures/gui/features/elytra_rules.png"));
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.elytraRulesEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setElytraRulesEnabled(enabled);
        ModNetwork.sendToServer(ElytraRulesPacket.current());
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
