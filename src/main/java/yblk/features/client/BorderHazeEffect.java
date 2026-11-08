package yblk.features.client;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import yblk.features.Features;
import yblk.features.border.BorderDanger;
import yblk.features.border.BorderZoneFx;
import yblk.features.mixin.PostChainAccessor;
import yblk.features.mixin.PostPassAccessor;

@Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
public final class BorderHazeEffect {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation CHAIN_ID =
            new ResourceLocation(Features.MODID, "shaders/post/border_haze");

    private static PostChain chain;
    private static int lastW = -1;
    private static int lastH = -1;
    private static boolean loadFailed;

    private BorderHazeEffect() {
    }

    @Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void onReload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> {
                closeChain();
                loadFailed = false;
            });
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (!BorderDanger.hasFx(BorderZoneFx.HAZE)) {
            return;
        }
        float intensity = BorderDanger.visualIntensity(mc.player);
        if (intensity <= 0.01f || loadFailed) {
            return;
        }
        if (!ensureChain(mc)) {
            return;
        }

        float[] rgb = hazeRgb(mc);
        for (PostPass pass : ((PostChainAccessor) chain).features$getPasses()) {
            EffectInstance effect = ((PostPassAccessor) pass).features$getEffect();
            Uniform intensityU = effect.getUniform("Intensity");
            if (intensityU != null) {
                intensityU.set(Mth.clamp(intensity, 0f, 1f));
            }
            Uniform colorU = effect.getUniform("HazeColor");
            if (colorU != null) {
                colorU.set(rgb[0], rgb[1], rgb[2]);
            }
        }

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        chain.process(event.getPartialTick());
        mc.getMainRenderTarget().bindWrite(false);
    }

    private static float[] hazeRgb(Minecraft mc) {
        var player = mc.player;
        if (player == null) {
            return new float[]{0.72f, 0.76f, 0.80f};
        }
        int color = player.level().getBiome(player.blockPosition()).value().getFogColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        return new float[]{
                Mth.clamp(r * 0.75f + 0.22f, 0f, 1f),
                Mth.clamp(g * 0.75f + 0.22f, 0f, 1f),
                Mth.clamp(b * 0.75f + 0.22f, 0f, 1f)
        };
    }

    private static boolean ensureChain(Minecraft mc) {
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (chain == null) {
            try {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), CHAIN_ID);
                chain.resize(w, h);
                lastW = w;
                lastH = h;
            } catch (Exception ex) {
                loadFailed = true;
                LOGGER.error("Failed to load border haze post chain", ex);
                return false;
            }
        } else if (w != lastW || h != lastH) {
            chain.resize(w, h);
            lastW = w;
            lastH = h;
        }
        return true;
    }

    private static void closeChain() {
        if (chain != null) {
            chain.close();
            chain = null;
        }
        lastW = -1;
        lastH = -1;
    }
}
