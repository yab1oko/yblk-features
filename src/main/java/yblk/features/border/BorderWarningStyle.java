package yblk.features.border;

public enum BorderWarningStyle {
    CHAT,
    HOTBAR,
    CENTER;

    public String langKey() {
        return "screen.features.border_danger.warn_style." + name().toLowerCase();
    }

    public static BorderWarningStyle fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return CENTER;
        }
        String key = raw.trim().toUpperCase();
        // Legacy "OFF" lived in the style enum; map to CENTER (checkbox handles enable).
        if ("OFF".equals(key)) {
            return CENTER;
        }
        try {
            return BorderWarningStyle.valueOf(key);
        } catch (Exception ignored) {
            return CENTER;
        }
    }
}
