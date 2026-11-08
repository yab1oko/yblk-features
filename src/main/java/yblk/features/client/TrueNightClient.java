package yblk.features.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;

import java.util.concurrent.ThreadLocalRandom;

/**
 * True Night — True Darkness style lightmap + real distance fog banks.
 * <p>
 * Sky light follows the moon (new moon → pitch black without torches).
 * Fog banks use FogRenderer distance/color only (never lightmap, never black near-plane).
 */
@Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
public final class TrueNightClient {
    private static final int FOG_ROLL_INTERVAL = 200;
    private static final int FOG_MIN_DURATION = 20 * 45;
    private static final int FOG_MAX_DURATION = 20 * 180;
    private static final int FOG_FADE_TICKS = 20 * 12;

    /** Target luminance per lightmap cell (block × sky), matching True Darkness. */
    private static final float[][] LUMINANCE = new float[16][16];

    private static boolean lightmapActive;
    private static int fogTicksLeft;
    private static int fogFadeIn;
    private static int fogTotal;
    private static float fogPeak;
    private static int rollCooldown;
    private static long lastDayTime = Long.MIN_VALUE;
    private static float smoothFogBank;

    private TrueNightClient() {
    }

    public static boolean isActive() {
        return FeaturesConfig.trueNightEnabled;
    }

    public static boolean isLightmapActive() {
        return lightmapActive;
    }

    /** Current fog-bank intensity 0..1 (smoothed). */
    public static float fogBankIntensity() {
        if (!isActive() || !FeaturesConfig.trueNightFogEnabled) {
            return 0f;
        }
        return smoothFogBank;
    }

    /**
     * 1 = vanilla brightness, 0 = no sky contribution.
     * Moon-phase mode: True Darkness curve (moon²). Always mode: constant night crush by strength.
     */
    public static float skyFactor(Level level) {
        if (!(level instanceof ClientLevel) || level.dimensionType().hasFixedTime()) {
            return 1f;
        }
        if (!level.dimensionType().hasSkyLight()) {
            return 1f;
        }

        float angle = level.getTimeOfDay(0f);
        float strength = Mth.clamp(FeaturesConfig.trueNightDarkness, 0f, 1f);
        if (strength <= 0.001f) {
            return 1f;
        }

        float factor;
        if (angle > 0.25f && angle < 0.75f) {
            // oldWeight → 1 near dusk/dawn, 0 at midnight.
            float oldWeight = Math.max(0f, (Math.abs(angle - 0.5f) - 0.2f)) * 20f;
            float w = oldWeight * oldWeight * oldWeight;
            float nightFactor;
            if (FeaturesConfig.trueNightMoonPhases) {
                float moon = Mth.clamp(level.getMoonBrightness(), 0f, 1f);
                nightFactor = moon * moon;
            } else {
                // Ignore moon — always crush sky light at night by configured strength.
                nightFactor = 0f;
            }
            factor = Mth.lerp(w, nightFactor, 1f);
        } else {
            factor = 1f;
        }

        factor = Mth.lerp(strength, 1f, factor);
        return Mth.clamp(factor, 0f, 1f);
    }

    public static float luminance(float r, float g, float b) {
        return r * 0.2126f + g * 0.7152f + b * 0.0722f;
    }

    /** Scale an ABGR lightmap pixel toward the precomputed target luminance. */
    public static int darken(int abgr, int blockIndex, int skyIndex) {
        float lTarget = LUMINANCE[blockIndex][skyIndex];
        float r = (abgr & 0xFF) / 255f;
        float g = ((abgr >> 8) & 0xFF) / 255f;
        float b = ((abgr >> 16) & 0xFF) / 255f;
        float l = luminance(r, g, b);
        float f = l > 1.0E-5f ? Math.min(1f, lTarget / l) : 0f;
        if (f >= 0.999f) {
            return abgr;
        }
        int nr = Math.round(f * r * 255f);
        int ng = Math.round(f * g * 255f);
        int nb = Math.round(f * b * 255f);
        int a = (abgr >>> 24) & 0xFF;
        return (a << 24) | (nb << 16) | (ng << 8) | nr;
    }

    public static void updateLuminance(float partialTick, float blockLightFlicker) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        lightmapActive = false;

        if (!isActive() || level == null || player == null) {
            return;
        }
        if (player.hasEffect(MobEffects.NIGHT_VISION)
                || player.hasEffect(MobEffects.CONDUIT_POWER)
                || level.getSkyFlashTime() > 0) {
            return;
        }

        float dimSkyFactor = skyFactor(level);
        if (dimSkyFactor >= 0.999f) {
            return;
        }

        lightmapActive = true;
        float ambient = level.getSkyDarken(1.0F);
        DimensionType dim = level.dimensionType();
        float flicker = blockLightFlicker * 0.1F + 1.5F;

        for (int skyIndex = 0; skyIndex < 16; ++skyIndex) {
            float skyCurve = 1f - skyIndex / 15f;
            skyCurve = 1f - skyCurve * skyCurve * skyCurve * skyCurve;
            float skyFactor = skyCurve * dimSkyFactor;

            float min = skyFactor * 0.05f;
            float rawAmbient = ambient * skyFactor;
            float minAmbient = rawAmbient * (1f - min) + min;
            float skyBase = brightness(dim, skyIndex) * minAmbient;

            min = 0.35f * skyFactor;
            float skyRed = skyBase * (rawAmbient * (1f - min) + min);
            float skyGreen = skyBase * (rawAmbient * (1f - min) + min);
            float skyBlue = skyBase;

            for (int blockIndex = 0; blockIndex < 16; ++blockIndex) {
                float blockCurve = 1f - blockIndex / 15f;
                blockCurve = 1f - blockCurve * blockCurve * blockCurve * blockCurve;

                float blockBase = blockCurve * brightness(dim, blockIndex) * flicker;
                min = 0.4f * blockCurve;
                float blockGreen = blockBase * ((blockBase * (1f - min) + min) * (1f - min) + min);
                float blockBlue = blockBase * (blockBase * blockBase * (1f - min) + min);

                float red = skyRed + blockBase;
                float green = skyGreen + blockGreen;
                float blue = skyBlue + blockBlue;

                float f = Math.max(skyFactor, blockCurve);
                min = 0.03f * f;
                red = red * (0.99F - min) + min;
                green = green * (0.99F - min) + min;
                blue = blue * (0.99F - min) + min;

                red = Mth.clamp(red, 0f, 1f);
                green = Mth.clamp(green, 0f, 1f);
                blue = Mth.clamp(blue, 0f, 1f);

                float gamma = mc.options.gamma().get().floatValue() * f;
                float invRed = 1.0F - red;
                float invGreen = 1.0F - green;
                float invBlue = 1.0F - blue;
                invRed = 1.0F - invRed * invRed * invRed * invRed;
                invGreen = 1.0F - invGreen * invGreen * invGreen * invGreen;
                invBlue = 1.0F - invBlue * invBlue * invBlue * invBlue;
                red = red * (1.0F - gamma) + invRed * gamma;
                green = green * (1.0F - gamma) + invGreen * gamma;
                blue = blue * (1.0F - gamma) + invBlue * gamma;

                min = 0.03f * f;
                red = red * (0.99F - min) + min;
                green = green * (0.99F - min) + min;
                blue = blue * (0.99F - min) + min;

                LUMINANCE[blockIndex][skyIndex] = luminance(
                        Mth.clamp(red, 0f, 1f),
                        Mth.clamp(green, 0f, 1f),
                        Mth.clamp(blue, 0f, 1f));
            }
        }
    }

    private static float brightness(DimensionType dim, int lightLevel) {
        float f = lightLevel / 15.0F;
        float f1 = f / (4.0F - 3.0F * f);
        return Mth.lerp(dim.ambientLight(), f1, 1.0F);
    }

    private static boolean isOutdoorsForFog(LocalPlayer player) {
        Level level = player.level();
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        BlockPos eye = BlockPos.containing(camera.getPosition());
        if (!level.canSeeSky(eye)) {
            return false;
        }
        return level.getBrightness(LightLayer.SKY, eye) >= 8;
    }

    private static float targetFogBank() {
        if (!FeaturesConfig.trueNightFogEnabled || fogTicksLeft <= 0 || fogTotal <= 0) {
            return 0f;
        }
        float fadeIn = fogFadeIn <= 0 ? 1f : 1f - (fogFadeIn / (float) FOG_FADE_TICKS);
        float fadeOut = fogTicksLeft >= FOG_FADE_TICKS
                ? 1f
                : fogTicksLeft / (float) FOG_FADE_TICKS;
        return fogPeak * Mth.clamp(Math.min(fadeIn, fadeOut), 0f, 1f);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;
        if (!isActive() || player == null || level == null) {
            fogTicksLeft = 0;
            fogFadeIn = 0;
            rollCooldown = 0;
            smoothFogBank = Mth.lerp(0.18f, smoothFogBank, 0f);
            if (smoothFogBank < 0.002f) {
                smoothFogBank = 0f;
            }
            return;
        }

        long dayTime = level.getDayTime() % 24000L;
        if (lastDayTime > 13000L && dayTime < 1000L) {
            fogTicksLeft = 0;
            fogFadeIn = 0;
        }
        lastDayTime = dayTime;

        boolean night = dayTime >= 13000L && dayTime <= 23000L;
        if (!night || !FeaturesConfig.trueNightFogEnabled) {
            if (fogTicksLeft > 0) {
                fogTicksLeft = Math.min(fogTicksLeft, FOG_FADE_TICKS);
            }
        }

        if (fogFadeIn > 0) {
            fogFadeIn--;
        }
        if (fogTicksLeft > 0) {
            fogTicksLeft--;
        } else if (night && FeaturesConfig.trueNightFogEnabled) {
            if (rollCooldown > 0) {
                rollCooldown--;
            } else {
                rollCooldown = FOG_ROLL_INTERVAL;
                float chance = FeaturesConfig.trueNightFogChance;
                float perRoll = 1f - (float) Math.pow(1f - Mth.clamp(chance, 0f, 1f), 1f / 6f);
                if (ThreadLocalRandom.current().nextFloat() < perRoll) {
                    float min = FeaturesConfig.trueNightFogMin;
                    float max = Math.max(min, FeaturesConfig.trueNightFogMax);
                    fogPeak = min + ThreadLocalRandom.current().nextFloat() * (max - min);
                    fogTotal = FOG_MIN_DURATION
                            + ThreadLocalRandom.current().nextInt(Math.max(1, FOG_MAX_DURATION - FOG_MIN_DURATION));
                    fogTicksLeft = fogTotal;
                    fogFadeIn = FOG_FADE_TICKS;
                }
            }
        }

        smoothFogBank = Mth.lerp(0.18f, smoothFogBank, targetFogBank());
        if (smoothFogBank < 0.002f) {
            smoothFogBank = 0f;
        }
    }

    /**
     * Real distance mist — FOG_TERRAIN only, spherical falloff (no cylinder ring),
     * soft near→far span so the sky doesn't band into stripes.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        if (event.getType() != FogType.NONE) {
            // Don't fight water/lava/powder-snow fog.
            return;
        }
        float intensity = fogBankIntensity();
        if (intensity <= 0.001f) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !isOutdoorsForFog(player)) {
            return;
        }

        float baseFar = event.getFarPlaneDistance();
        float baseNear = event.getNearPlaneDistance();
        // Soft atmospheric mist — long fade, no hard wall / cylinder ring.
        float targetFar = Mth.lerp(intensity, baseFar, Mth.clamp(baseFar * 0.42f, 48f, 96f));
        // Start fog gradually; keep a wide near→far gap to kill banding stripes.
        float targetNear = Mth.lerp(intensity, baseNear, Math.max(baseNear, targetFar * 0.08f));
        targetNear = Math.min(targetNear, targetFar * 0.25f);

        event.setFarPlaneDistance(Math.min(baseFar, targetFar));
        event.setNearPlaneDistance(Math.max(baseNear, targetNear));
        // CYLINDER reads as a weird circle around the player; SPHERE is real mist.
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float intensity = fogBankIntensity();
        if (intensity <= 0.001f) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !isOutdoorsForFog(player)) {
            return;
        }

        // Cool grey-blue mist — blend gently with biome fog so it doesn't posterize.
        float tr = 0.42f;
        float tg = 0.48f;
        float tb = 0.58f;
        float t = intensity * 0.72f;
        event.setRed(Mth.lerp(t, event.getRed(), tr));
        event.setGreen(Mth.lerp(t, event.getGreen(), tg));
        event.setBlue(Mth.lerp(t, event.getBlue(), tb));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        lightmapActive = false;
        smoothFogBank = 0f;
        fogTicksLeft = 0;
        fogFadeIn = 0;
        rollCooldown = 0;
    }
}
