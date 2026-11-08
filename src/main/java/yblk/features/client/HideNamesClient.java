package yblk.features.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.names.HideNames;
import yblk.features.names.HideNamesMode;

@Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
public final class HideNamesClient {
    private HideNamesClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }
        if (!HideNames.isActive()) {
            return;
        }

        HideNamesMode mode = HideNames.mode();
        if (mode == HideNamesMode.FULL || mode == HideNamesMode.MASKS) {
            if (HideNames.shouldFullyHide(target)) {
                event.setResult(Event.Result.DENY);
            }
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer viewer = mc.player;
        if (viewer == null) {
            return;
        }
        // Own name in F5 — still apply the same rules.
        float partial = event.getPartialTick();
        double dist = viewer.getEyePosition(partial).distanceTo(target.getEyePosition(partial));

        if (mode == HideNamesMode.LOOK_CLOSE) {
            if (!isLookingAt(viewer, target, partial, FeaturesConfig.hideNamesLookAngle)
                    || dist > FeaturesConfig.hideNamesLookDistance) {
                event.setResult(Event.Result.DENY);
            }
            return;
        }

        // PROXIMITY_FADE
        float alpha = proximityAlpha(dist);
        if (alpha <= 0.02F) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (alpha >= 0.98F) {
            return; // default full render
        }

        event.setResult(Event.Result.DENY);
        renderFadedName(event, target, alpha);
    }

    private static float proximityAlpha(double dist) {
        float near = FeaturesConfig.hideNamesFadeNear;
        float far = FeaturesConfig.hideNamesFadeFar;
        if (far <= near) {
            return dist <= near ? 1F : 0F;
        }
        if (dist <= near) {
            return 1F;
        }
        if (dist >= far) {
            return 0F;
        }
        return 1F - (float) ((dist - near) / (far - near));
    }

    private static boolean isLookingAt(Player viewer, Player target, float partial, float minDot) {
        Vec3 eye = viewer.getEyePosition(partial);
        Vec3 look = viewer.getViewVector(partial);
        Vec3 to = target.getEyePosition(partial).subtract(eye);
        double len = to.length();
        if (len < 1.0E-4) {
            return true;
        }
        to = to.scale(1.0 / len);
        return look.dot(to) >= minDot;
    }

    private static void renderFadedName(RenderNameTagEvent event, Player target, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Component content = event.getContent();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int light = event.getPackedLight();

        float opacity = Mth.clamp(alpha, 0F, 1F);
        int a = Mth.clamp((int) (opacity * 255F), 0, 255);
        int color = (a << 24) | 0xFFFFFF;
        int bgA = Mth.clamp((int) (opacity * 0.25F * 255F), 0, 255);
        int bg = (bgA << 24);

        pose.pushPose();
        pose.translate(0.0, target.getBbHeight() + 0.5, 0.0);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);

        float x = -font.width(content) / 2F;
        // Behind (see-through) then solid — same order as vanilla EntityRenderer#renderNameTag.
        font.drawInBatch(content, x, 0, color, false, pose.last().pose(), buffer,
                Font.DisplayMode.SEE_THROUGH, bg, light);
        font.drawInBatch(content, x, 0, color, false, pose.last().pose(), buffer,
                Font.DisplayMode.NORMAL, 0, light);
        pose.popPose();
    }
}
