package yblk.features.rfp;

/**
 * Whether vanilla first-person hands stay visible alongside the real body.
 */
public enum RfpHandMode {
    /** Body arms only — cancel vanilla FP hands. */
    OFF,
    /** Cancel vanilla hands except in awkward poses (swim / fly with elytra). */
    DYNAMIC,
    /** Keep vanilla FP hands on top of the body. */
    ALWAYS;

    public String langKey() {
        return "screen.features.real_first_person.hands." + name().toLowerCase();
    }

    public static RfpHandMode fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return OFF;
        }
        try {
            return RfpHandMode.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return OFF;
        }
    }
}
