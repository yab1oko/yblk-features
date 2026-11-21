package yblk.features.restrict;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;
import yblk.features.network.CommandBlockerPacket;
import yblk.features.network.ModNetwork;

public class CommandBlockerFeature extends Feature {
    public CommandBlockerFeature() {
        super(
                "command_blocker",
                new ResourceLocation(Features.MODID, "textures/gui/features/command_blocker.png")
        );
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.commandBlockerEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setCommandBlockerEnabled(enabled);
        ModNetwork.sendToServer(CommandBlockerPacket.current());
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
