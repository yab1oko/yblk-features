package yblk.features.border;

public enum BorderHarmMode {
    DAMAGE,
    EFFECTS;

    public String langKey() {
        return "screen.features.border_danger.harm." + name().toLowerCase();
    }

    public static BorderHarmMode fromConfig(String raw) {
        try {
            return BorderHarmMode.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return DAMAGE;
        }
    }
}
