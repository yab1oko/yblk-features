package yblk.features.border;

import net.minecraft.network.chat.Component;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;

public final class BorderWarningTexts {
    private BorderWarningTexts() {
    }

    public static String defaultApproachKey() {
        return "message." + Features.MODID + ".border_danger.warning.approach";
    }

    public static String defaultDangerKey() {
        return "message." + Features.MODID + ".border_danger.warning.danger";
    }

    public static Component approach() {
        return message(FeaturesConfig.borderWarnTextApproach, defaultApproachKey());
    }

    public static Component danger() {
        return message(FeaturesConfig.borderWarnTextDanger, defaultDangerKey());
    }

    public static Component forStage(BorderDanger.WarningStage stage) {
        return stage == BorderDanger.WarningStage.DANGER ? danger() : approach();
    }

    /** Effective text for the editor (custom if set, otherwise current lang default). */
    public static String effectiveApproachRaw() {
        if (FeaturesConfig.borderWarnTextApproach != null && !FeaturesConfig.borderWarnTextApproach.isBlank()) {
            return FeaturesConfig.borderWarnTextApproach;
        }
        return Component.translatable(defaultApproachKey()).getString();
    }

    public static String effectiveDangerRaw() {
        if (FeaturesConfig.borderWarnTextDanger != null && !FeaturesConfig.borderWarnTextDanger.isBlank()) {
            return FeaturesConfig.borderWarnTextDanger;
        }
        return Component.translatable(defaultDangerKey()).getString();
    }

    private static Component message(String custom, String langKey) {
        if (custom != null && !custom.isBlank()) {
            return Component.literal(custom);
        }
        return Component.translatable(langKey);
    }
}
