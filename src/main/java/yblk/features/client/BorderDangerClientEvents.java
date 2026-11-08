package yblk.features.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.border.BorderDanger;
import yblk.features.border.BorderZoneFx;

@Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
public final class BorderDangerClientEvents {
    private BorderDangerClientEvents() {
    }

    /** Used by LevelMixin to drive vanilla rain renderer. */
    public static float rainWeatherBoost() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !BorderDanger.hasFx(BorderZoneFx.RAIN)) {
            return 0f;
        }
        return BorderDanger.visualIntensity(player);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float intensity = BorderDanger.visualIntensity(player);
        if (intensity <= 0.001f) {
            return;
        }
        boolean fog = BorderDanger.hasFx(BorderZoneFx.FOG);
        boolean haze = BorderDanger.hasFx(BorderZoneFx.HAZE);
        if (!fog && !haze) {
            return;
        }
        float targetFar;
        float targetNear;
        if (fog && haze) {
            targetFar = 18f;
            targetNear = 1.2f;
        } else if (fog) {
            targetFar = 14f;
            targetNear = 0.2f;
        } else {
            // Soft milky distance haze — no particles, distance fog only.
            targetFar = 42f;
            targetNear = 4f;
        }
        float baseFar = event.getFarPlaneDistance();
        float baseNear = event.getNearPlaneDistance();
        event.setFarPlaneDistance(Mth.lerp(intensity, baseFar, targetFar));
        event.setNearPlaneDistance(Mth.lerp(intensity, baseNear, targetNear));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.level() == null) {
            return;
        }
        float intensity = BorderDanger.visualIntensity(player);
        if (intensity <= 0.001f) {
            return;
        }
        boolean fog = BorderDanger.hasFx(BorderZoneFx.FOG);
        boolean haze = BorderDanger.hasFx(BorderZoneFx.HAZE);
        if (!fog && !haze) {
            return;
        }
        float[] biome = biomeFogRgb(player);
        float tr;
        float tg;
        float tb;
        if (fog && !haze) {
            tr = biome[0] * 0.55f + 0.12f;
            tg = biome[1] * 0.50f + 0.08f;
            tb = biome[2] * 0.55f + 0.10f;
        } else if (haze && !fog) {
            tr = biome[0] * 0.70f + 0.26f;
            tg = biome[1] * 0.72f + 0.24f;
            tb = biome[2] * 0.74f + 0.24f;
        } else {
            tr = biome[0] * 0.62f + 0.18f;
            tg = biome[1] * 0.60f + 0.16f;
            tb = biome[2] * 0.64f + 0.16f;
        }
        event.setRed(Mth.lerp(intensity, event.getRed(), tr));
        event.setGreen(Mth.lerp(intensity, event.getGreen(), tg));
        event.setBlue(Mth.lerp(intensity, event.getBlue(), tb));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        float intensity = BorderDanger.visualIntensity(player);
        if (intensity <= 0.001f) {
            return;
        }

        Vec3 pos = player.position();
        var random = player.getRandom();
        double[] wind = borderWind(player);

        if (BorderDanger.hasFx(BorderZoneFx.SNOW)) {
            int count = Mth.ceil(55 * intensity);
            for (int i = 0; i < count; i++) {
                double x = pos.x + (random.nextDouble() - 0.5) * 18.0;
                double y = pos.y + random.nextDouble() * 14.0 + 1.0;
                double z = pos.z + (random.nextDouble() - 0.5) * 18.0;
                double vx = wind[0] * (0.55 + random.nextDouble() * 0.9) + (random.nextDouble() - 0.5) * 0.35;
                double vy = -0.25 - random.nextDouble() * 0.55;
                double vz = wind[1] * (0.55 + random.nextDouble() * 0.9) + (random.nextDouble() - 0.5) * 0.35;
                mc.level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, vx, vy, vz);
            }
        }

        // Rain: vanilla weather mesh via getRainLevel mixin. Extra ground splashes for authenticity.
        if (BorderDanger.hasFx(BorderZoneFx.RAIN) && intensity > 0.15f) {
            int splashes = Mth.ceil(8 * intensity);
            for (int i = 0; i < splashes; i++) {
                double x = pos.x + (random.nextDouble() - 0.5) * 12.0;
                double z = pos.z + (random.nextDouble() - 0.5) * 12.0;
                int gx = Mth.floor(x);
                int gz = Mth.floor(z);
                int gy = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, gx, gz);
                mc.level.addParticle(ParticleTypes.RAIN, x, gy + 0.1, z, 0.0, 0.0, 0.0);
                if (random.nextFloat() < 0.4f) {
                    mc.level.addParticle(ParticleTypes.SPLASH, x, gy + 0.1, z,
                            (random.nextDouble() - 0.5) * 0.1, 0.05, (random.nextDouble() - 0.5) * 0.1);
                }
            }
        }
        // Haze: fog + PostChain (BorderHazeEffect) — no dust particles.
    }

    private static float[] biomeFogRgb(LocalPlayer player) {
        Biome biome = player.level().getBiome(player.blockPosition()).value();
        int color = biome.getFogColor();
        return new float[]{
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f
        };
    }

    private static double[] borderWind(LocalPlayer player) {
        WorldBorder border = player.level().getWorldBorder();
        double dx = player.getX() - border.getCenterX();
        double dz = player.getZ() - border.getCenterZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) {
            return new double[]{0.7, 0.2};
        }
        return new double[]{dx / len, dz / len};
    }
}
