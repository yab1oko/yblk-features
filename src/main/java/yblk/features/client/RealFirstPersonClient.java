package yblk.features.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.compat.BetterCombatRfpCompat;
import yblk.features.config.FeaturesConfig;
import yblk.features.rfp.FpBodyRenderer;
import yblk.features.rfp.RealFirstPerson;
import yblk.features.rfp.RfpHandMode;
import yblk.features.rfp.RfpPoseOverride;

@Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
public final class RealFirstPersonClient {
    private RealFirstPersonClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        RfpPoseOverride.syncFromScreen(mc.screen);
    }

    /**
     * {@code PlayerRenderer.setModelProperties} re-enables every part — re-hide after that.
     * Also strips Better Combat's FP player render down to arms only.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof LocalPlayer local)) {
            return;
        }
        if (!FpBodyRenderer.isRendering() && !FpBodyRenderer.shouldForceArmsOnly(local)) {
            return;
        }
        if (event.getRenderer().getModel() instanceof PlayerModel<?> model) {
            FpBodyRenderer.applyModelVisibility(model, local);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !RealFirstPerson.isActive()) {
            return;
        }
        if (mc.options.getCameraType() != net.minecraft.client.CameraType.FIRST_PERSON) {
            return;
        }

        // BC swing: weapon/arms come from BC's FP player render (arms-only) —
        // cancel vanilla/PA held-item hands to avoid a second weapon.
        if (BetterCombatRfpCompat.isWeaponSwingInProgress()) {
            event.setCanceled(true);
            return;
        }

        if (!RealFirstPerson.shouldRenderBody(event.getPartialTick())) {
            return;
        }

        // TacZ / bow-crossbow-trident: keep vanilla (or mod) FP hands.
        if (RealFirstPerson.shouldDelegateArms(player)) {
            return;
        }

        RfpHandMode mode = FeaturesConfig.rfpHandMode;
        if (mode == RfpHandMode.ALWAYS) {
            return;
        }
        if (mode == RfpHandMode.DYNAMIC && keepVanillaHands(player)) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (RfpPoseOverride.isActive() && mc.player != null) {
            RfpPoseOverride.apply(mc.player);
        }
        if (!RealFirstPerson.shouldRenderBody(event.getPartialTick())) {
            return;
        }
        FpBodyRenderer.render(event.getPoseStack(), event.getPartialTick());
    }

    private static boolean keepVanillaHands(LocalPlayer player) {
        return player.isVisuallySwimming() || player.isFallFlying();
    }
}
