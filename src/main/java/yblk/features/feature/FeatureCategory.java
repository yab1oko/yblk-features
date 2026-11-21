package yblk.features.feature;

import yblk.features.Features;

/**
 * Menu groups. Admin groups ({@link #RESTRICTIONS}, {@link #SERVER}) use a red accent.
 * Ids that do not exist yet are listed so a module shows up in the right group
 * as soon as it is registered under that id.
 */
public enum FeatureCategory {
    PLAYER,
    INTERFACE,
    WORLD,
    MOBS,
    RESTRICTIONS,
    SERVER;

    public boolean adminAccent() {
        return this == RESTRICTIONS || this == SERVER;
    }

    public String langKey() {
        return "screen." + Features.MODID + ".category." + name().toLowerCase();
    }

    public static FeatureCategory byId(String id) {
        return switch (id) {
            case "inventory_revamp", "wetness", "real_first_person", "item_weight" -> PLAYER;
            case "hud_hide", "f3_hide", "hide_names" -> INTERFACE;
            case "true_night", "border_danger", "portal_lock", "elytra_rules" -> WORLD;
            case "mob_spawn_block", "mob_drops", "villager_trades" -> MOBS;
            case "item_remover", "interaction_blocker", "tool_restrictions", "command_blocker" -> RESTRICTIONS;
            case "local_chat", "server_tweaks" -> SERVER;
            default -> PLAYER;
        };
    }
}
