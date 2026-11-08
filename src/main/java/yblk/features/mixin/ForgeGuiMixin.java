package yblk.features.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.client.HudHideClient;

/**
 * Forge draws hearts / food / armor / air via {@link ForgeGui}, not {@code Gui.renderPlayerHealth}.
 * Fade alpha must be applied at the start of these methods — {@code setupOverlayRenderState}
 * resets shader color right before they run.
 */
@Mixin(ForgeGui.class)
public abstract class ForgeGuiMixin {

    @Inject(method = "renderHealth", at = @At("HEAD"), remap = false)
    private void features$healthFadeBegin(int width, int height, GuiGraphics graphics, CallbackInfo ci) {
        HudHideClient.applyDrawEffect(graphics);
    }

    @Inject(method = "renderHealth", at = @At("RETURN"), remap = false)
    private void features$healthFadeEnd(int width, int height, GuiGraphics graphics, CallbackInfo ci) {
        HudHideClient.clearDrawEffect(graphics);
    }

    @Inject(method = "renderFood", at = @At("HEAD"), remap = false)
    private void features$foodFadeBegin(int width, int height, GuiGraphics graphics, CallbackInfo ci) {
        HudHideClient.applyDrawEffect(graphics);
    }

    @Inject(method = "renderFood", at = @At("RETURN"), remap = false)
    private void features$foodFadeEnd(int width, int height, GuiGraphics graphics, CallbackInfo ci) {
        HudHideClient.clearDrawEffect(graphics);
    }

    @Inject(method = "renderArmor", at = @At("HEAD"), remap = false)
    private void features$armorFadeBegin(GuiGraphics graphics, int width, int height, CallbackInfo ci) {
        HudHideClient.applyDrawEffect(graphics);
    }

    @Inject(method = "renderArmor", at = @At("RETURN"), remap = false)
    private void features$armorFadeEnd(GuiGraphics graphics, int width, int height, CallbackInfo ci) {
        HudHideClient.clearDrawEffect(graphics);
    }

    @Inject(method = "renderAir", at = @At("HEAD"), remap = false)
    private void features$airFadeBegin(int width, int height, GuiGraphics graphics, CallbackInfo ci) {
        HudHideClient.applyDrawEffect(graphics);
    }

    @Inject(method = "renderAir", at = @At("RETURN"), remap = false)
    private void features$airFadeEnd(int width, int height, GuiGraphics graphics, CallbackInfo ci) {
        HudHideClient.clearDrawEffect(graphics);
    }
}
