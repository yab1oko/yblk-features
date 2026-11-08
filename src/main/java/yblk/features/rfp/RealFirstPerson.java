package yblk.features.rfp;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.UseAnim;
import yblk.features.compat.TaczRfpCompat;
import yblk.features.config.FeaturesConfig;

public final class RealFirstPerson {
    private RealFirstPerson() {
    }

    public static boolean isActive() {
        return FeaturesConfig.realFirstPersonEnabled;
    }

    /**
     * Whether the FP body pass should run this frame for the local player.
     */
    public static boolean shouldRenderBody() {
        return shouldRenderBody(0f);
    }

    public static boolean shouldRenderBody(float partialTick) {
        if (!isActive()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return false;
        }
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            return false;
        }
        if (player.isSpectator() || player.isSleeping()) {
            return false;
        }
        // Headless torso "slides up" through the camera while leaving water / crawl pose.
        // Skip this gate while settings force a tune pose (incl. swim/elytra on land).
        if (!RfpPoseOverride.isActive() && isSwimTransition(player, partialTick)) {
            return false;
        }
        return true;
    }

    /**
     * True while the swim blend is mid-transition or the player is crawling on land.
     * Full swimming in water still renders (uses swim offset).
     */
    public static boolean isSwimTransition(LocalPlayer player, float partialTick) {
        if (player.isFallFlying()) {
            return false;
        }
        float swim = player.getSwimAmount(partialTick);
        boolean inWater = player.isInWaterOrBubble();
        // Crawling on land (pose swim, not in fluid) — hide.
        if (!inWater && (player.getPose() == Pose.SWIMMING || player.isVisuallySwimming())) {
            return true;
        }
        // Getting in/out of water: swim amount animating while not fully submerged swim.
        if (!inWater && swim > 0.05f) {
            return true;
        }
        // Standing up in shallow water — blend between stand and swim.
        if (inWater && !player.isVisuallySwimming() && swim > 0.05f && swim < 0.95f) {
            return true;
        }
        return false;
    }

    /**
     * Bow / crossbow / trident draw — keep vanilla FP hands, hide body arms.
     */
    public static boolean isUsingVanillaRanged(LocalPlayer player) {
        if (player == null || !player.isUsingItem()) {
            return false;
        }
        UseAnim anim = player.getUseItem().getUseAnimation();
        return anim == UseAnim.BOW || anim == UseAnim.CROSSBOW || anim == UseAnim.SPEAR;
    }

    /** Hands/items should be owned by vanilla FP or TacZ, not the body model. */
    public static boolean shouldDelegateArms(LocalPlayer player) {
        return TaczRfpCompat.shouldDelegateArms(player) || isUsingVanillaRanged(player);
    }

    public static float bodyOffset(LocalPlayer player) {
        if (RfpPoseOverride.isActive()) {
            return RfpPoseOverride.get().offset();
        }
        if (player.isPassenger()) {
            return FeaturesConfig.rfpOffsetSit;
        }
        if (player.isFallFlying()) {
            return FeaturesConfig.rfpOffsetElytra;
        }
        if (player.isVisuallySwimming()) {
            return FeaturesConfig.rfpOffsetSwim;
        }
        if (player.isCrouching()) {
            return FeaturesConfig.rfpOffsetSneak;
        }
        return FeaturesConfig.rfpOffsetStand;
    }
}
