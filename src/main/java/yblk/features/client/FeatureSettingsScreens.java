package yblk.features.client;

import net.minecraft.client.gui.screens.Screen;
import yblk.features.feature.Feature;

final class FeatureSettingsScreens {
    private FeatureSettingsScreens() {
    }

    static Screen open(Feature feature, Screen parent) {
        return switch (feature.getId()) {
            case "inventory_revamp" -> new InventoryRevampSettingsScreen(parent);
            case "mob_spawn_block" -> new MobSpawnSettingsScreen(parent);
            case "border_danger" -> new BorderDangerSettingsScreen(parent);
            case "hud_hide" -> new HudHideSettingsScreen(parent);
            case "f3_hide" -> new F3HideSettingsScreen(parent);
            case "hide_names" -> new HideNamesSettingsScreen(parent);
            case "real_first_person" -> new RealFirstPersonSettingsScreen(parent);
            case "true_night" -> new TrueNightSettingsScreen(parent);
            case "local_chat" -> new LocalChatSettingsScreen(parent);
            case "wetness" -> new WetnessSettingsScreen(parent);
            case "item_remover" -> new ItemRemoverSettingsScreen(parent);
            default -> parent;
        };
    }
}
