package yblk.features.mobs;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;
import yblk.features.network.MobDropsPacket;
import yblk.features.network.ModNetwork;
import yblk.features.restrict.Restrictions;

public class MobDropsFeature extends Feature {
    public MobDropsFeature() {
        super("mob_drops", new ResourceLocation(Features.MODID, "textures/gui/features/mob_drops.png"));
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.mobDropsEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setMobDropsEnabled(enabled);
        ModNetwork.sendToServer(MobDropsPacket.current());
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
