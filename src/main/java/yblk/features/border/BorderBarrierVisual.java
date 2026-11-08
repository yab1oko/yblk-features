package yblk.features.border;

public enum BorderBarrierVisual {
    VANILLA,
    HIDDEN,
    CUSTOM;

    public String langKey() {
        return "screen.features.border_danger.barrier." + name().toLowerCase();
    }

    public static BorderBarrierVisual fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return VANILLA;
        }
        String key = raw.trim().toUpperCase();
        // Old builds used PLACEHOLDER — map to CUSTOM.
        if ("PLACEHOLDER".equals(key)) {
            return CUSTOM;
        }
        try {
            return BorderBarrierVisual.valueOf(key);
        } catch (Exception ignored) {
            return VANILLA;
        }
    }
}
