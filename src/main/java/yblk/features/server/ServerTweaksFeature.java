package yblk.features.server;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;
import yblk.features.network.ModNetwork;
import yblk.features.network.ServerTweaksPacket;
import yblk.features.restrict.Restrictions;

public class ServerTweaksFeature extends Feature {
    public ServerTweaksFeature() {
        super("server_tweaks", new ResourceLocation(Features.MODID, "textures/gui/features/server_tweaks.png"));
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.serverTweaksEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setServerTweaksEnabled(enabled);
        ModNetwork.sendToServer(ServerTweaksPacket.current(false));
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
