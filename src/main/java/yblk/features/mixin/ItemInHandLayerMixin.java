package yblk.features.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.rfp.FpBodyRenderer;

@Mixin(value = {ItemInHandLayer.class, PlayerItemInHandLayer.class})
public abstract class ItemInHandLayerMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void features$skipHeldInRfp(LivingEntity entity, ItemStack stack, ItemDisplayContext display,
                                        HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer,
                                        int light, CallbackInfo ci) {
        if (FpBodyRenderer.shouldHideHeldItems()) {
            ci.cancel();
        }
    }
}
