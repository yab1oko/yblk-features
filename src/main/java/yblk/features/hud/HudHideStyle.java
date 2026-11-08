package yblk.features.hud;

public enum HudHideStyle {
    /** Alpha fade in place (hotbar-style). */
    FADE,
    /** Slide off-screen, then disappear. */
    SLIDE;

    public String langKey() {
        return "screen.features.hud_hide.style." + name().toLowerCase();
    }

    public static HudHideStyle fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return FADE;
        }
        try {
            return HudHideStyle.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return FADE;
        }
    }
}
