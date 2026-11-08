package yblk.features.remover;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;
import yblk.features.network.ItemRemoverConfigPacket;
import yblk.features.network.ModNetwork;

public class ItemRemoverFeature extends Feature {
    public ItemRemoverFeature() {
        super(
                "item_remover",
                new ResourceLocation(Features.MODID, "textures/gui/features/item_remover.png")
        );
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.itemRemoverEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setItemRemoverEnabled(enabled);
        ModNetwork.sendToServer(ItemRemoverConfigPacket.current());
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
        return ItemRemover.canEdit(Minecraft.getInstance().player);
    }
}
