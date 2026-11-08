package yblk.features.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.client.TrueNightClient;

/**
 * True Darkness–style: after vanilla builds the lightmap, retarget each cell's
 * luminance so sky light follows the moon curve while block/emissive light stays.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @Shadow
    @Final
    private NativeImage lightPixels;

    @Shadow
    @Final
    private DynamicTexture lightTexture;

    @Shadow
    private float blockLightRedFlicker;

    @Inject(method = "updateLightTexture", at = @At("HEAD"))
    private void features$trueNightPrepare(float partialTicks, CallbackInfo ci) {
        TrueNightClient.updateLuminance(partialTicks, this.blockLightRedFlicker);
    }

    @Inject(method = "updateLightTexture", at = @At("TAIL"))
    private void features$trueNightDarken(float partialTicks, CallbackInfo ci) {
        if (!TrueNightClient.isLightmapActive()) {
            return;
        }
        for (int sky = 0; sky < 16; sky++) {
            for (int block = 0; block < 16; block++) {
                int abgr = lightPixels.getPixelRGBA(block, sky);
                int darkened = TrueNightClient.darken(abgr, block, sky);
                if (darkened != abgr) {
                    lightPixels.setPixelRGBA(block, sky, darkened);
                }
            }
        }
        lightTexture.upload();
    }
}
