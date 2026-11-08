package yblk.features.rfp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import yblk.features.compat.BetterCombatRfpCompat;
import yblk.features.compat.CuriosRfpCompat;
import yblk.features.config.FeaturesConfig;

import java.util.List;

/**
 * Renders the local player's third-person model in first person.
 * Poses come from the normal player pipeline (vanilla + PlayerAnimator).
 */
public final class FpBodyRenderer {
    private static boolean rendering;
    /** Sticky for this FP body pass — must not re-query emptied inventory. */
    private static boolean hideArmsPass;
    private static boolean hideHeldItemsPass;
    private static boolean hideHeadPass;

    private FpBodyRenderer() {
    }

    public static boolean isRendering() {
        return rendering;
    }

    /** Head mesh + helmet / custom head / head curios should be suppressed. */
    public static boolean shouldHideHeadGear() {
        if (rendering && hideHeadPass) {
            return true;
        }
        // Better Combat's own FP player render during swings.
        return isBetterCombatFirstPerson(Minecraft.getInstance().player);
    }

    public static boolean shouldHideArms() {
        return rendering && hideArmsPass;
    }

    public static boolean shouldHideHeldItems() {
        return rendering && hideHeldItemsPass;
    }

    /**
     * Arms-only for Better Combat's first-person player render (and our pass if used).
     * BC draws the full player in FP while swinging — we strip torso/legs there.
     */
    public static boolean shouldForceArmsOnly(LivingEntity entity) {
        if (!(entity instanceof LocalPlayer)) {
            return false;
        }
        return isBetterCombatFirstPerson((LocalPlayer) entity);
    }

    private static boolean isBetterCombatFirstPerson(LocalPlayer player) {
        if (player == null || !BetterCombatRfpCompat.isWeaponSwingInProgress()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.options.getCameraType() == CameraType.FIRST_PERSON && mc.player == player;
    }

    /**
     * Called from {@link net.minecraftforge.client.event.RenderLivingEvent.Pre}
     * and {@link yblk.features.mixin.PlayerModelMixin} after pose mods run.
     */
    public static void applyModelVisibility(PlayerModel<?> model, LivingEntity entity) {
        if (shouldForceArmsOnly(entity)) {
            applyArmsOnly(model);
            return;
        }
        if (!rendering) {
            return;
        }
        if (hideHeadPass) {
            model.head.visible = false;
            model.hat.visible = false;
        }
        if (hideArmsPass) {
            model.leftArm.visible = false;
            model.rightArm.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
        }
    }

    /** @deprecated use {@link #applyModelVisibility(PlayerModel, LivingEntity)} */
    public static void applyModelVisibility(PlayerModel<?> model) {
        Minecraft mc = Minecraft.getInstance();
        applyModelVisibility(model, mc.player);
    }

    public static void applyArmsOnly(PlayerModel<?> model) {
        model.setAllVisible(false);
        model.leftArm.visible = true;
        model.rightArm.visible = true;
        model.leftSleeve.visible = true;
        model.rightSleeve.visible = true;
    }

    public static void render(PoseStack poseStack, float partialTick) {
        if (rendering || !RealFirstPerson.shouldRenderBody(partialTick)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        // During BC swings the mod itself renders the local player in FP.
        // Drawing our body too = torso + double weapon. Skip; we only strip
        // that BC pass down to arms via shouldForceArmsOnly.
        if (BetterCombatRfpCompat.isWeaponSwingInProgress()) {
            return;
        }

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        if (!(dispatcher.getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = camera.getPosition();
        double x = Mth.lerp(partialTick, player.xOld, player.getX()) - cam.x;
        double y = Mth.lerp(partialTick, player.yOld, player.getY()) - cam.y;
        double z = Mth.lerp(partialTick, player.zOld, player.getZ()) - cam.z;

        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float offset = RealFirstPerson.bodyOffset(player);

        boolean delegateArms = RealFirstPerson.shouldDelegateArms(player);
        hideArmsPass = delegateArms;
        hideHeldItemsPass = delegateArms;
        hideHeadPass = FeaturesConfig.rfpHideHead;

        int arrowCount = player.getArrowCount();
        int stingCount = player.getStingerCount();
        if (!FeaturesConfig.rfpRenderStuck) {
            player.setArrowCount(0);
            player.setStingerCount(0);
        }

        Inventory inv = player.getInventory();
        ItemStack mainHand = ItemStack.EMPTY;
        ItemStack offHand = ItemStack.EMPTY;
        if (hideHeldItemsPass) {
            mainHand = inv.getSelected().copy();
            offHand = inv.offhand.get(0).copy();
            inv.setItem(inv.selected, ItemStack.EMPTY);
            inv.offhand.set(0, ItemStack.EMPTY);
        }

        ItemStack headItem = ItemStack.EMPTY;
        List<Runnable> curiosRestore = List.of();
        if (hideHeadPass) {
            headItem = player.getItemBySlot(EquipmentSlot.HEAD).copy();
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            curiosRestore = CuriosRfpCompat.suppressHeadCurios(player);
        }

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        if (offset != 0f) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            poseStack.translate(0.0, 0.0, offset);
            poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw));
        }

        dispatcher.setRenderShadow(false);
        rendering = true;
        try {
            PlayerModel<AbstractClientPlayer> model = renderer.getModel();
            applyModelVisibility(model, player);
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            int light = dispatcher.getPackedLightCoords(player, partialTick);
            renderer.render(player, bodyYaw, partialTick, poseStack, buffers, light);
            buffers.endBatch();
        } finally {
            rendering = false;
            boolean restoreHead = hideHeadPass;
            boolean restoreHands = hideHeldItemsPass;
            hideArmsPass = false;
            hideHeldItemsPass = false;
            hideHeadPass = false;
            dispatcher.setRenderShadow(true);
            if (restoreHead) {
                player.setItemSlot(EquipmentSlot.HEAD, headItem);
                for (Runnable r : curiosRestore) {
                    r.run();
                }
            }
            if (restoreHands) {
                inv.setItem(inv.selected, mainHand);
                inv.offhand.set(0, offHand);
            }
            if (!FeaturesConfig.rfpRenderStuck) {
                player.setArrowCount(arrowCount);
                player.setStingerCount(stingCount);
            }
            poseStack.popPose();
        }
    }
}
