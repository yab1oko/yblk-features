package yblk.features.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.client.FeatureAccentColors;

public abstract class Feature {
    private final String id;
    private final ResourceLocation icon;

    protected Feature(String id, ResourceLocation icon) {
        this.id = id;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    /**
     * Accent for the card border — sampled from the icon on the client
     * (cached per texture path; refreshed on resource reload).
     */
    public int getAccentColor() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return FeatureAccentColors.get(icon);
        }
        return 0xFF9A9AA8;
    }

    public Component getTitle() {
        return Component.translatable("feature." + Features.MODID + "." + id);
    }

    public Component getDescription() {
        return Component.translatable("feature." + Features.MODID + "." + id + ".desc");
    }

    public abstract boolean isEnabled();

    public abstract void setEnabled(boolean enabled);

    public void onToggle(boolean enabled) {
    }

    /** When true, the features menu shows a settings button on this card. */
    public boolean hasSettings() {
        return false;
    }

    /**
     * Server/admin features — listed under the Admin-only divider; need op (perm 2+).
     */
    public boolean requiresOperator() {
        return false;
    }

    /**
     * When false, players cannot toggle this feature or change its settings
     * (admin lock / operator gate).
     */
    public boolean canPlayerConfigure() {
        if (!requiresOperator()) {
            return true;
        }
        return isLocalOperator();
    }

    protected static boolean isLocalOperator() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return false;
        }
        Player player = Minecraft.getInstance().player;
        return player != null && player.hasPermissions(2);
    }
}
