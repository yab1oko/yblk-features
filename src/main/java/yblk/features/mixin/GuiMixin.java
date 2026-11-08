package yblk.features.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.client.HudHideClient;
import yblk.features.config.FeaturesConfig;

@Mixin(Gui.class)
public abstract class GuiMixin {
    /**
     * Vanilla {@code renderVignette} turns the screen red near the world border.
     * Pretend the player is infinitely far so only the normal darkness vignette remains.
     */
    @Redirect(
            method = "renderVignette",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/border/WorldBorder;getDistanceToBorder(Lnet/minecraft/world/entity/Entity;)D"
            )
    )
    private double features$hideBorderRedTint(WorldBorder border, Entity entity) {
        if (FeaturesConfig.borderHideVanillaRedTint) {
            return Double.MAX_VALUE;
        }
        return border.getDistanceToBorder(entity);
    }

    /*
     * Fade alpha must be applied inside the Gui draw methods — overlay Pre setColor
     * is overwritten by setupOverlayRenderState / vanilla before the first blit.
     * Hearts/food/armor/air live on ForgeGui — see ForgeGuiMixin.
     */

    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void features$hotbarFadeBegin(float partialTick, GuiGraphics graphics, CallbackInfo ci) {
        HudHideClient.applyDrawEffect(graphics);
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private void features$hotbarFadeEnd(float partialTick, GuiGraphics graphics, CallbackInfo ci) {
        HudHideClient.clearDrawEffect(graphics);
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"))
    private void features$xpFadeBegin(GuiGraphics graphics, int x, CallbackInfo ci) {
        HudHideClient.applyDrawEffect(graphics);
    }

    @Inject(method = "renderExperienceBar", at = @At("RETURN"))
    private void features$xpFadeEnd(GuiGraphics graphics, int x, CallbackInfo ci) {
        HudHideClient.clearDrawEffect(graphics);
    }

    @Inject(method = "renderEffects", at = @At("HEAD"))
    private void features$effectsFadeBegin(GuiGraphics graphics, CallbackInfo ci) {
        HudHideClient.applyDrawEffect(graphics);
    }

    @Inject(method = "renderEffects", at = @At("RETURN"))
    private void features$effectsFadeEnd(GuiGraphics graphics, CallbackInfo ci) {
        HudHideClient.clearDrawEffect(graphics);
    }

    @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"))
    private void features$itemNameFadeBegin(GuiGraphics graphics, CallbackInfo ci) {
        HudHideClient.applyDrawEffect(graphics);
    }

    @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("RETURN"))
    private void features$itemNameFadeEnd(GuiGraphics graphics, CallbackInfo ci) {
        HudHideClient.clearDrawEffect(graphics);
    }
}
