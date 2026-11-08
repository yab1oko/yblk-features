package yblk.features.f3;

import java.util.Locale;

/**
 * Toggleable F3 debug lines. Matching is based on vanilla 1.20.x string prefixes.
 */
public enum F3Element {
    // —— Important ——
    COORDINATES(F3Category.IMPORTANT, "coordinates"),
    LIGHT(F3Category.IMPORTANT, "light"),
    DAY(F3Category.IMPORTANT, "day"),
    ENTITIES(F3Category.IMPORTANT, "entities"),

    // —— Other (left / game) ——
    VERSION(F3Category.OTHER, "version"),
    FPS(F3Category.OTHER, "fps"),
    BIOME(F3Category.OTHER, "biome"),
    DIFFICULTY(F3Category.OTHER, "difficulty"),
    PARTICLES(F3Category.OTHER, "particles"),
    SOUND(F3Category.OTHER, "sound"),
    HEIGHTMAP(F3Category.OTHER, "heightmap"),
    LOOKING_BLOCK(F3Category.OTHER, "looking_block"),
    LOOKING_FLUID(F3Category.OTHER, "looking_fluid"),
    TARGETED_ENTITY(F3Category.OTHER, "targeted_entity"),
    HELP(F3Category.OTHER, "help"),
    MISC_LEFT(F3Category.OTHER, "misc_left"),

    // —— Other (right / system) ——
    JAVA(F3Category.OTHER, "java"),
    MEMORY(F3Category.OTHER, "memory"),
    CPU(F3Category.OTHER, "cpu"),
    DISPLAY(F3Category.OTHER, "display"),
    GPU(F3Category.OTHER, "gpu"),
    BLOCK_DETAILS(F3Category.OTHER, "block_details"),
    MISC_RIGHT(F3Category.OTHER, "misc_right");

    private final F3Category category;
    private final String key;

    F3Element(F3Category category, String key) {
        this.category = category;
        this.key = key;
    }

    public F3Category category() {
        return category;
    }

    public String configKey() {
        return key;
    }

    public String langKey() {
        return "screen.features.f3_hide.element." + key;
    }

    public static F3Element byConfigKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String k = raw.trim().toLowerCase(Locale.ROOT);
        for (F3Element element : values()) {
            if (element.key.equals(k)) {
                return element;
            }
        }
        return null;
    }
}
