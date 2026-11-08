package yblk.features.mixin;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.rfp.FpBodyRenderer;

/**
 * PlayerAnimator / Better Combat may re-enable parts after RenderLivingEvent.Pre.
 * Re-apply RFP / BC arms-only visibility at the end of setupAnim.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void features$rfpVisibility(T entity, float limbSwing, float limbSwingAmount,
                                        float ageInTicks, float netHeadYaw, float headPitch,
                                        CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer)) {
            return;
        }
        @SuppressWarnings("unchecked")
        PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>) (Object) this;
        FpBodyRenderer.applyModelVisibility(model, entity);
    }
}
