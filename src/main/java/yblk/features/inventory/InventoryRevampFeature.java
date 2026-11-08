package yblk.features.inventory;

import net.minecraft.resources.ResourceLocation;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;

public class InventoryRevampFeature extends Feature {
    public InventoryRevampFeature() {
        super(
                "inventory_revamp",
                new ResourceLocation(Features.MODID, "textures/gui/features/inventory_revamp.png")
        );
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.inventoryRevampEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        FeaturesConfig.setInventoryRevampEnabled(enabled);
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public boolean requiresOperator() {
        return true;
    }
}
