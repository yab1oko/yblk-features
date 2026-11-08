package yblk.features.rfp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import yblk.features.client.RealFirstPersonSettingsScreen;

/**
 * While RFP settings are open, force a pose on the local player so the live
 * first-person view matches the offset being tuned.
 * <p>
 * Movement is frozen for every pose. Elytra is visual-only (no {@code startFallFlying}).
 */
public final class RfpPoseOverride {
    private static RfpTunePose pose = RfpTunePose.STAND;
    private static boolean active;
    private static double freezeX;
    private static double freezeY;
    private static double freezeZ;
    private static boolean hasFreezePos;

    private RfpPoseOverride() {
    }

    public static RfpTunePose get() {
        return pose;
    }

    public static void set(RfpTunePose next) {
        pose = next == null ? RfpTunePose.STAND : next;
    }

    public static boolean isActive() {
        return active;
    }

    public static float overrideOffsetOr(float normal) {
        return active ? pose.offset() : normal;
    }

    public static void syncFromScreen(Screen screen) {
        if (screen instanceof RealFirstPersonSettingsScreen settings) {
            if (!active) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    freezeX = player.getX();
                    freezeY = player.getY();
                    freezeZ = player.getZ();
                    hasFreezePos = true;
                }
            }
            active = true;
            pose = settings.getTunePose();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                apply(player);
            }
        } else if (active) {
            clear();
        }
    }

    public static void clear() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            restore(player);
        }
        active = false;
        hasFreezePos = false;
        pose = RfpTunePose.STAND;
    }

    public static void apply(LocalPlayer player) {
        if (player == null || !active) {
            return;
        }
        // Never let menu pose-tuning move or launch the player.
        player.setDeltaMovement(Vec3.ZERO);
        player.xxa = 0.0F;
        player.zza = 0.0F;
        player.setJumping(false);
        if (hasFreezePos) {
            player.setPos(freezeX, freezeY, freezeZ);
            player.xo = freezeX;
            player.yo = freezeY;
            player.zo = freezeZ;
            player.xOld = freezeX;
            player.yOld = freezeY;
            player.zOld = freezeZ;
        }

        player.setShiftKeyDown(false);
        player.setSwimming(false);
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }

        switch (pose) {
            case STAND -> player.setPose(Pose.STANDING);
            case SNEAK -> {
                player.setShiftKeyDown(true);
                player.setPose(Pose.CROUCHING);
            }
            case SIT -> player.setPose(Pose.SITTING);
            case SWIM -> {
                player.setSwimming(true);
                player.setPose(Pose.SWIMMING);
            }
            case ELYTRA -> {
                // Pose only — startFallFlying would let you take off from the settings menu.
                player.setPose(Pose.FALL_FLYING);
            }
        }
    }

    private static void restore(LocalPlayer player) {
        player.setShiftKeyDown(false);
        player.setSwimming(false);
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
        player.setPose(Pose.STANDING);
    }
}