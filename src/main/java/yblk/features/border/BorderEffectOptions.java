package yblk.features.border;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BorderEffectOptions {
    public static final Map<String, MobEffect> ALL = new LinkedHashMap<>();

    static {
        ALL.put("nausea", MobEffects.CONFUSION);
        ALL.put("wither", MobEffects.WITHER);
        ALL.put("poison", MobEffects.POISON);
        ALL.put("blindness", MobEffects.BLINDNESS);
        ALL.put("slowness", MobEffects.MOVEMENT_SLOWDOWN);
        ALL.put("hunger", MobEffects.HUNGER);
        ALL.put("weakness", MobEffects.WEAKNESS);
    }

    private BorderEffectOptions() {
    }
}
