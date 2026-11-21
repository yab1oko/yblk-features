package yblk.features.weight;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;
import yblk.features.network.ItemWeightPacket;
import yblk.features.network.ModNetwork;
import yblk.features.restrict.Restrictions;

public class ItemWeightFeature extends Feature {
    public ItemWeightFeature() {
        super("item_weight", new ResourceLocation(Features.MODID, "textures/gui/features/item_weight.png"));
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.itemWeightEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setItemWeightEnabled(enabled);
        ModNetwork.sendToServer(ItemWeightPacket.current());
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
