package yblk.features.names;

/**
 * How player name tags are revealed while Hide Names is enabled.
 */
public enum HideNamesMode {
    /** Always hide every player name tag. */
    FULL,
    /** Hidden far away; fade in as you approach. */
    PROXIMITY_FADE,
    /** Visible only when looking at the player up close. */
    LOOK_CLOSE,
    /** Hide only when the target wears a listed head-slot item (masks / pumpkins). */
    MASKS;

    public String langKey() {
        return "screen.features.hide_names.mode." + name().toLowerCase();
    }

    public HideNamesMode next() {
        HideNamesMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static HideNamesMode fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return MASKS;
        }
        try {
            return HideNamesMode.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return MASKS;
        }
    }
}
