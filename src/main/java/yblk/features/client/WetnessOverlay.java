package yblk.features.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.wetness.Wetness;
import yblk.features.wetness.WetnessCapability;

import java.io.InputStream;
import java.util.Optional;

/**
 * LSO-style centered status symbol above the XP bar.
 * <p>
 * Replaceable atlas: {@code textures/gui/wetness_hud.png}
 * Layout: 5 equal frames in a row (16×16 each). Prefer power-of-two sheet
 * (128×16); frames sit at x=0,16,32,48,64.
 */
@Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
public final class WetnessOverlay {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(Features.MODID, "textures/gui/wetness_hud.png");

    private static final int SIZE = 16;
    private static final int FRAMES = 5;
    /** How fast % / stage chase the real value (higher = snappier). */
    private static final float AMOUNT_LERP = 10f;
    private static final float STAGE_LERP = 7f;

    private static int atlasW = 128;
    private static int atlasH = 16;
    private static boolean atlasSized;

    private static float lastAmount = -1f;
    private static float smoothedAmount = -1f;
    private static float smoothedStage;
    private static long lastNanos;
    private static int trend;
    private static int trendTicks;
    private static boolean shakeSide;

    private WetnessOverlay() {
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.EXPERIENCE_BAR.type()) {
            return;
        }
        if (!FeaturesConfig.wetnessEnabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || player.isCreative() || player.isSpectator()) {
            return;
        }

        Wetness wet = WetnessCapability.get(player);
        float amount = wet.get();
        float dt = deltaSeconds();
        updateSmoothed(amount, dt);
        updateTrend(amount);

        if (smoothedAmount < 0.5f && amount < 0.5f && trend == 0) {
            lastAmount = amount;
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = event.getWindow().getGuiScaledWidth();
        int screenH = event.getWindow().getGuiScaledHeight();

        int x = screenW / 2 - SIZE / 2;
        int y = screenH - 52;

        int xOff = 0;
        int targetStage = stageOf(amount);
        if (targetStage >= 4 || smoothedStage >= 3.5f) {
            if (player.tickCount % 6 == 0) {
                shakeSide = !shakeSide;
            }
            xOff = shakeSide ? 1 : 0;
        }

        resolveAtlasSize(mc);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        drawInterpolatedIcon(graphics, x + xOff, y);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (smoothedAmount >= 0.5f || amount >= 1f) {
            String label = Math.round(smoothedAmount) + "%";
            int tw = mc.font.width(label);
            graphics.drawString(mc.font, label, x + (SIZE - tw) / 2, y - 10, 0xFFB8D8E8, true);
        }

        if (trend != 0 && trendTicks > 0) {
            drawTrend(graphics, x + xOff, y, trend > 0, trendTicks);
        }

        lastAmount = amount;
    }

    private static float deltaSeconds() {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0.016f : (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;
        return Mth.clamp(dt, 0.001f, 0.1f);
    }

    private static void updateSmoothed(float amount, float dt) {
        if (smoothedAmount < 0f) {
            smoothedAmount = amount;
            smoothedStage = stageOf(amount);
            return;
        }
        // Exponential ease toward target — frame-rate independent.
        float amountT = 1f - (float) Math.exp(-AMOUNT_LERP * dt);
        float stageT = 1f - (float) Math.exp(-STAGE_LERP * dt);
        smoothedAmount = Mth.lerp(amountT, smoothedAmount, amount);
        smoothedStage = Mth.lerp(stageT, smoothedStage, (float) stageOf(amount));
        if (amount < 0.01f && smoothedAmount < 0.15f) {
            smoothedAmount = 0f;
            smoothedStage = 0f;
        }
    }

    private static void drawInterpolatedIcon(GuiGraphics graphics, int x, int y) {
        float stage = Mth.clamp(smoothedStage, 0f, FRAMES - 1f);
        int from = Mth.clamp((int) Math.floor(stage), 0, FRAMES - 1);
        int to = Mth.clamp((int) Math.ceil(stage), 0, FRAMES - 1);
        float blend = stage - from;

        if (from == to || blend < 0.02f) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            blitStage(graphics, x, y, from);
            return;
        }

        // Crossfade previous → next stage.
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f - blend);
        blitStage(graphics, x, y, from);
        RenderSystem.setShaderColor(1f, 1f, 1f, blend);
        blitStage(graphics, x, y, to);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void blitStage(GuiGraphics graphics, int x, int y, int stage) {
        int u = stage * SIZE;
        graphics.blit(TEXTURE, x, y, u, 0, SIZE, SIZE, atlasW, atlasH);
    }

    private static void resolveAtlasSize(Minecraft mc) {
        if (atlasSized) {
            return;
        }
        try {
            Optional<Resource> resource = mc.getResourceManager().getResource(TEXTURE);
            if (resource.isPresent()) {
                try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                    if (image.getWidth() > 0 && image.getHeight() > 0) {
                        atlasW = image.getWidth();
                        atlasH = image.getHeight();
                    }
                }
            }
        } catch (Exception ignored) {
            // Keep defaults (128×16).
        }
        atlasSized = true;
    }

    private static void updateTrend(float amount) {
        if (lastAmount < 0f) {
            lastAmount = amount;
            return;
        }
        float delta = amount - lastAmount;
        if (delta > 0.4f) {
            trend = 1;
            trendTicks = 24;
        } else if (delta < -0.4f) {
            trend = -1;
            trendTicks = 24;
        } else if (trendTicks > 0) {
            trendTicks--;
            if (trendTicks == 0) {
                trend = 0;
            }
        }
    }

    private static int stageOf(float amount) {
        if (amount < 1f) {
            return 0;
        }
        if (amount < 25f) {
            return 1;
        }
        if (amount < 50f) {
            return 2;
        }
        if (amount < 75f) {
            return 3;
        }
        return 4;
    }

    private static void drawTrend(GuiGraphics g, int x, int y, boolean rising, int ticks) {
        int frame = Mth.clamp(6 - ticks / 4, 0, 5);
        int alpha = 0x40 + frame * 30;
        int color = (Math.min(255, alpha) << 24) | (rising ? 0x50C8FF : 0x6496DC);
        int tipY = rising ? y - 12 : y + SIZE + 1;
        int dir = rising ? -1 : 1;
        g.fill(x + 6, tipY, x + 10, tipY + 1, color);
        g.fill(x + 5, tipY + dir, x + 11, tipY + dir + 1, color);
        g.fill(x + 4, tipY + dir * 2, x + 12, tipY + dir * 2 + 1, color);
    }
}
