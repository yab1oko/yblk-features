package yblk.features.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.rfp.FpBodyRenderer;

@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void features$skipCapeArmsOnly(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                           AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                                           float partialTicks, float ageInTicks, float netHeadYaw, float headPitch,
                                           CallbackInfo ci) {
        if (FpBodyRenderer.shouldForceArmsOnly(player) || FpBodyRenderer.isRendering()) {
            ci.cancel();
        }
    }
}
