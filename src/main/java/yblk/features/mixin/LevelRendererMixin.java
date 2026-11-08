package yblk.features.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.border.BorderBarrierVisual;
import yblk.features.border.BorderDanger;
import yblk.features.client.CustomBarrierTexture;
import yblk.features.config.FeaturesConfig;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    private static final ResourceLocation VANILLA_FORCEFIELD =
            new ResourceLocation("textures/misc/forcefield.png");

    @Inject(method = "renderWorldBorder", at = @At("HEAD"), cancellable = true)
    private void features$hideWorldBorder(Camera camera, CallbackInfo ci) {
        if (BorderDanger.isActive() && FeaturesConfig.borderBarrierVisual == BorderBarrierVisual.HIDDEN) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "renderWorldBorder",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;FORCEFIELD_LOCATION:Lnet/minecraft/resources/ResourceLocation;",
                    opcode = org.objectweb.asm.Opcodes.GETSTATIC
            )
    )
    private ResourceLocation features$swapForcefieldTexture() {
        if (BorderDanger.isActive()
                && FeaturesConfig.borderBarrierVisual == BorderBarrierVisual.CUSTOM
                && CustomBarrierTexture.isReady()) {
            return CustomBarrierTexture.ID;
        }
        return VANILLA_FORCEFIELD;
    }
}
