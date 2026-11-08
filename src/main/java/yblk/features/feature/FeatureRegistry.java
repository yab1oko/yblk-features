package yblk.features.feature;

import yblk.features.border.BorderDangerFeature;
import yblk.features.chat.LocalChatFeature;
import yblk.features.f3.F3HideFeature;
import yblk.features.hud.HudHideFeature;
import yblk.features.inventory.InventoryRevampFeature;
import yblk.features.names.HideNamesFeature;
import yblk.features.night.TrueNightFeature;
import yblk.features.remover.ItemRemoverFeature;
import yblk.features.rfp.RealFirstPersonFeature;
import yblk.features.spawn.MobSpawnBlockFeature;
import yblk.features.wetness.WetnessFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FeatureRegistry {
    private static final List<Feature> FEATURES = new ArrayList<>();

    private FeatureRegistry() {
    }

    public static void bootstrap() {
        FEATURES.clear();
        // Player-facing first…
        FEATURES.add(new HudHideFeature());
        FEATURES.add(new F3HideFeature());
        FEATURES.add(new HideNamesFeature());
        FEATURES.add(new RealFirstPersonFeature());
        FEATURES.add(new TrueNightFeature());
        // …then admin-only (FeaturesScreen draws the divider before these).
        FEATURES.add(new InventoryRevampFeature());
        FEATURES.add(new MobSpawnBlockFeature());
        FEATURES.add(new BorderDangerFeature());
        FEATURES.add(new LocalChatFeature());
        FEATURES.add(new WetnessFeature());
        FEATURES.add(new ItemRemoverFeature());
    }

    public static List<Feature> all() {
        return Collections.unmodifiableList(FEATURES);
    }

    public static List<Feature> playerFacing() {
        List<Feature> out = new ArrayList<>();
        for (Feature f : FEATURES) {
            if (!f.requiresOperator()) {
                out.add(f);
            }
        }
        return out;
    }

    public static List<Feature> operatorOnly() {
        List<Feature> out = new ArrayList<>();
        for (Feature f : FEATURES) {
            if (f.requiresOperator()) {
                out.add(f);
            }
        }
        return out;
    }
}
