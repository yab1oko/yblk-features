package yblk.features.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import yblk.features.Features;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Samples a feature icon PNG and caches a vivid accent for the card border.
 * Recalculates when resources reload or when a new icon path is seen.
 */
public final class FeatureAccentColors {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int FALLBACK = 0xFF9A9AA8;
    private static final Map<ResourceLocation, Integer> CACHE = new HashMap<>();

    private FeatureAccentColors() {
    }

    public static int get(ResourceLocation icon) {
        if (icon == null) {
            return FALLBACK;
        }
        Integer cached = CACHE.get(icon);
        if (cached != null) {
            return cached;
        }
        int color = sample(icon);
        CACHE.put(icon, color);
        return color;
    }

    /** Drop cache so new / replaced icons are re-sampled. */
    public static void clear() {
        CACHE.clear();
    }

    private static int sample(ResourceLocation icon) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return FALLBACK;
        }
        ResourceManager resources = mc.getResourceManager();
        Optional<Resource> resource = resources.getResource(icon);
        if (resource.isEmpty()) {
            return FALLBACK;
        }
        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            return pickAccent(image);
        } catch (Exception e) {
            LOGGER.warn("Failed to sample accent from {}", icon, e);
            return FALLBACK;
        }
    }

    /**
     * Prefer the most frequent saturated color bucket (skips transparent / gray / extremes),
     * then average exact pixels in that bucket. Falls back to saturation-weighted mean.
     */
    private static int pickAccent(NativeImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        if (w <= 0 || h <= 0) {
            return FALLBACK;
        }

        // bucket -> (weight, rSum, gSum, bSum)
        Map<Integer, long[]> buckets = new HashMap<>();
        double meanR = 0, meanG = 0, meanB = 0, meanW = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int abgr = image.getPixelRGBA(x, y);
                int a = (abgr >>> 24) & 0xFF;
                if (a < 48) {
                    continue;
                }
                // NativeImage RGBA pack is ABGR in memory: A B G R
                int b = (abgr >>> 16) & 0xFF;
                int g = (abgr >>> 8) & 0xFF;
                int r = abgr & 0xFF;

                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));
                int chroma = max - min;
                float lum = (r * 0.2126f + g * 0.7152f + b * 0.0722f);
                if (chroma < 16 || lum < 22f || lum > 242f) {
                    continue;
                }

                float sat = chroma / (float) Math.max(1, max);
                float weight = (a / 255f) * (0.35f + 0.65f * sat);
                meanR += r * weight;
                meanG += g * weight;
                meanB += b * weight;
                meanW += weight;

                int key = ((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3);
                long[] acc = buckets.computeIfAbsent(key, k -> new long[4]);
                long iw = Math.max(1, Math.round(weight * 1000));
                acc[0] += iw;
                acc[1] += (long) r * iw;
                acc[2] += (long) g * iw;
                acc[3] += (long) b * iw;
            }
        }

        if (!buckets.isEmpty()) {
            long bestW = 0;
            long[] best = null;
            for (long[] acc : buckets.values()) {
                if (acc[0] > bestW) {
                    bestW = acc[0];
                    best = acc;
                }
            }
            if (best != null && best[0] > 0) {
                int r = (int) (best[1] / best[0]);
                int g = (int) (best[2] / best[0]);
                int b = (int) (best[3] / best[0]);
                return pack(boostForBorder(r, g, b));
            }
        }

        if (meanW > 0.001) {
            int r = (int) Math.round(meanR / meanW);
            int g = (int) Math.round(meanG / meanW);
            int b = (int) Math.round(meanB / meanW);
            return pack(boostForBorder(r, g, b));
        }
        return FALLBACK;
    }

    /** Slightly lift saturation/brightness so the thin border reads clearly. */
    private static int[] boostForBorder(int r, int g, int b) {
        float[] hsv = rgbToHsv(r, g, b);
        hsv[1] = Mth.clamp(hsv[1] * 1.12f + 0.04f, 0f, 1f);
        hsv[2] = Mth.clamp(Math.max(hsv[2], 0.42f) * 1.06f, 0f, 1f);
        return hsvToRgb(hsv[0], hsv[1], hsv[2]);
    }

    private static int pack(int[] rgb) {
        return 0xFF000000 | ((rgb[0] & 0xFF) << 16) | ((rgb[1] & 0xFF) << 8) | (rgb[2] & 0xFF);
    }

    private static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float d = max - min;
        float h;
        if (d < 1e-6f) {
            h = 0f;
        } else if (max == rf) {
            h = ((gf - bf) / d) % 6f;
        } else if (max == gf) {
            h = (bf - rf) / d + 2f;
        } else {
            h = (rf - gf) / d + 4f;
        }
        h /= 6f;
        if (h < 0f) {
            h += 1f;
        }
        float s = max <= 1e-6f ? 0f : d / max;
        return new float[]{h, s, max};
    }

    private static int[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1f - Math.abs((h * 6f) % 2f - 1f));
        float m = v - c;
        float rf;
        float gf;
        float bf;
        float sector = h * 6f;
        if (sector < 1f) {
            rf = c;
            gf = x;
            bf = 0f;
        } else if (sector < 2f) {
            rf = x;
            gf = c;
            bf = 0f;
        } else if (sector < 3f) {
            rf = 0f;
            gf = c;
            bf = x;
        } else if (sector < 4f) {
            rf = 0f;
            gf = x;
            bf = c;
        } else if (sector < 5f) {
            rf = x;
            gf = 0f;
            bf = c;
        } else {
            rf = c;
            gf = 0f;
            bf = x;
        }
        return new int[]{
                Mth.clamp(Math.round((rf + m) * 255f), 0, 255),
                Mth.clamp(Math.round((gf + m) * 255f), 0, 255),
                Mth.clamp(Math.round((bf + m) * 255f), 0, 255)
        };
    }

    @Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ReloadHook {
        private ReloadHook() {
        }

        @SubscribeEvent
        public static void onRegisterReload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> clear());
        }
    }
}
