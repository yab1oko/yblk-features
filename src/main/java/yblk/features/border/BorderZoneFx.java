package yblk.features.border;

public enum BorderZoneFx {
    HAZE,
    FOG,
    SNOW,
    RAIN;

    public String langKey() {
        return "screen.features.border_danger.fx." + name().toLowerCase();
    }

    public String configKey() {
        return name().toLowerCase();
    }

    public static boolean isValidKey(String raw) {
        if (raw == null) {
            return false;
        }
        try {
            BorderZoneFx.valueOf(raw.trim().toUpperCase());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static BorderZoneFx fromKey(String raw) {
        try {
            return BorderZoneFx.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return null;
        }
    }
}
