package yblk.features.restrict;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;
import yblk.features.network.ModNetwork;
import yblk.features.network.ToolRestrictionsPacket;

public class ToolRestrictionsFeature extends Feature {
    public ToolRestrictionsFeature() {
        super(
                "tool_restrictions",
                new ResourceLocation(Features.MODID, "textures/gui/features/tool_restrictions.png")
        );
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.toolRestrictionsEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setToolRestrictionsEnabled(enabled);
        ModNetwork.sendToServer(ToolRestrictionsPacket.current());
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
