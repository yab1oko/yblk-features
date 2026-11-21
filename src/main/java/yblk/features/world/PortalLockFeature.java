package yblk.features.world;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;
import yblk.features.network.ModNetwork;
import yblk.features.network.PortalLockPacket;
import yblk.features.restrict.Restrictions;

public class PortalLockFeature extends Feature {
    public PortalLockFeature() {
        super("portal_lock", new ResourceLocation(Features.MODID, "textures/gui/features/portal_lock.png"));
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.portalLockEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setPortalLockEnabled(enabled);
        ModNetwork.sendToServer(PortalLockPacket.current());
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
