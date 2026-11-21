package yblk.features.mobs;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;
import yblk.features.network.ModNetwork;
import yblk.features.network.VillagerTradesPacket;
import yblk.features.restrict.Restrictions;

public class VillagerTradesFeature extends Feature {
    public VillagerTradesFeature() {
        super("villager_trades", new ResourceLocation(Features.MODID, "textures/gui/features/villager_trades.png"));
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.villagerTradesEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setVillagerTradesEnabled(enabled);
        ModNetwork.sendToServer(VillagerTradesPacket.current());
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
