package yblk.features.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.Feature;
import yblk.features.network.LocalChatConfigPacket;
import yblk.features.network.ModNetwork;

public class LocalChatFeature extends Feature {
    public LocalChatFeature() {
        super(
                "local_chat",
                new ResourceLocation(Features.MODID, "textures/gui/features/local_chat.png")
        );
    }

    @Override
    public boolean isEnabled() {
        return FeaturesConfig.localChatEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!canPlayerConfigure()) {
            return;
        }
        FeaturesConfig.setLocalChatEnabled(enabled);
        ModNetwork.sendToServer(LocalChatConfigPacket.current());
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
        return LocalChat.canEdit(Minecraft.getInstance().player);
    }
}
