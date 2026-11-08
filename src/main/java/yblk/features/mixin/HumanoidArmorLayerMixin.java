package yblk.features.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.rfp.FpBodyRenderer;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void features$skipHelmetInRfp(PoseStack poseStack, MultiBufferSource buffer, T entity,
                                          EquipmentSlot slot, int packedLight, A model, CallbackInfo ci) {
        if (slot == EquipmentSlot.HEAD && FpBodyRenderer.shouldHideHeadGear()) {
            ci.cancel();
            return;
        }
        if (FpBodyRenderer.shouldForceArmsOnly(entity)
                && (slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET || slot == EquipmentSlot.HEAD)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "renderArmorPiece",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;setPartVisibility(Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/world/entity/EquipmentSlot;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void features$armorVisibilityInRfp(PoseStack poseStack, MultiBufferSource buffer, T entity,
                                               EquipmentSlot slot, int packedLight, A model, CallbackInfo ci) {
        if (FpBodyRenderer.shouldForceArmsOnly(entity) && slot == EquipmentSlot.CHEST) {
            model.body.visible = false;
            model.leftArm.visible = true;
            model.rightArm.visible = true;
            return;
        }
        if (FpBodyRenderer.shouldHideArms() && slot == EquipmentSlot.CHEST) {
            model.leftArm.visible = false;
            model.rightArm.visible = false;
        }
    }
}
