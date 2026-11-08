package yblk.features.f3;

import net.minecraft.ChatFormatting;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps a raw F3 line to an {@link F3Element}. Stateful for multi-line sections.
 */
public final class F3LineClassifier {
    private static final Pattern DAY_IN_DIFFICULTY = Pattern.compile("\\s*\\(Day\\s+\\d+\\)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DAY_CAPTURE = Pattern.compile("\\(Day\\s+(\\d+)\\)", Pattern.CASE_INSENSITIVE);

    private enum Section {
        NONE,
        TARGETED_BLOCK,
        TARGETED_FLUID,
        TARGETED_ENTITY,
        BLOCK_DETAILS
    }

    private Section section = Section.NONE;
    private final boolean leftSide;

    public F3LineClassifier(boolean leftSide) {
        this.leftSide = leftSide;
    }

    public record Classified(F3Element element, String line) {
    }

    public Classified classify(String raw) {
        if (raw == null) {
            return new Classified(null, null);
        }
        String plain = ChatFormatting.stripFormatting(raw);
        if (plain == null) {
            plain = raw;
        }
        String trimmed = plain.trim();
        if (trimmed.isEmpty()) {
            section = Section.NONE;
            return new Classified(null, raw);
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);

        if (equalsHeader(lower, "targeted block")) {
            section = Section.TARGETED_BLOCK;
            return new Classified(F3Element.LOOKING_BLOCK, raw);
        }
        if (equalsHeader(lower, "targeted fluid")) {
            section = Section.TARGETED_FLUID;
            return new Classified(F3Element.LOOKING_FLUID, raw);
        }
        if (equalsHeader(lower, "targeted entity")) {
            section = Section.TARGETED_ENTITY;
            return new Classified(F3Element.TARGETED_ENTITY, raw);
        }

        if (section == Section.TARGETED_BLOCK) {
            return new Classified(F3Element.LOOKING_BLOCK, raw);
        }
        if (section == Section.TARGETED_FLUID) {
            return new Classified(F3Element.LOOKING_FLUID, raw);
        }
        if (section == Section.TARGETED_ENTITY) {
            return new Classified(F3Element.TARGETED_ENTITY, raw);
        }

        if (leftSide) {
            return classifyLeft(raw, trimmed, lower);
        }
        return classifyRight(raw, trimmed, lower);
    }

    private Classified classifyLeft(String raw, String trimmed, String lower) {
        if (lower.startsWith("minecraft ") || lower.startsWith("minecraft\t")) {
            return new Classified(F3Element.VERSION, raw);
        }
        if (lower.contains(" fps") || lower.matches("^\\d+\\s+fps\\b.*")) {
            return new Classified(F3Element.FPS, raw);
        }
        if (lower.startsWith("xyz:")) {
            return new Classified(F3Element.COORDINATES, raw);
        }
        // Block coordinates: "Block: 1 2 3" (not "Targeted Block")
        if (lower.startsWith("block:") && looksLikeBlockCoords(trimmed)) {
            return new Classified(F3Element.COORDINATES, raw);
        }
        if (lower.startsWith("chunk:")) {
            return new Classified(F3Element.COORDINATES, raw);
        }
        if (lower.startsWith("facing:")) {
            return new Classified(F3Element.COORDINATES, raw);
        }
        if (lower.startsWith("client light") || lower.startsWith("server light")) {
            return new Classified(F3Element.LIGHT, raw);
        }
        if (lower.startsWith("local difficulty")) {
            return new Classified(F3Element.DIFFICULTY, raw);
        }
        if (lower.startsWith("day ") || lower.matches("^day\\s+\\d+.*")) {
            return new Classified(F3Element.DAY, raw);
        }
        if (lower.startsWith("biome:")) {
            return new Classified(F3Element.BIOME, raw);
        }
        if (lower.startsWith("e:")) {
            return new Classified(F3Element.ENTITIES, raw);
        }
        if (lower.startsWith("p:")) {
            return new Classified(F3Element.PARTICLES, raw);
        }
        if (lower.startsWith("sounds:") || lower.startsWith("sound:")) {
            return new Classified(F3Element.SOUND, raw);
        }
        if (lower.startsWith("ch ") || lower.startsWith("sh ") || lower.startsWith("fc:")) {
            return new Classified(F3Element.HEIGHTMAP, raw);
        }
        if (lower.startsWith("debug:") || lower.startsWith("for help:") || lower.contains("[shift]:")) {
            return new Classified(F3Element.HELP, raw);
        }
        return new Classified(F3Element.MISC_LEFT, raw);
    }

    private Classified classifyRight(String raw, String trimmed, String lower) {
        if (lower.startsWith("java:")) {
            section = Section.NONE;
            return new Classified(F3Element.JAVA, raw);
        }
        if (lower.startsWith("mem:") || lower.startsWith("allocated:")) {
            section = Section.NONE;
            return new Classified(F3Element.MEMORY, raw);
        }
        if (lower.startsWith("cpu:")) {
            section = Section.NONE;
            return new Classified(F3Element.CPU, raw);
        }
        if (lower.startsWith("display:")) {
            section = Section.NONE;
            return new Classified(F3Element.DISPLAY, raw);
        }
        // GPU / GL vendor lines usually follow Display
        if (lower.contains("opengl") || lower.startsWith("gl ")
                || lower.contains("nvidia") || lower.contains("amd ") || lower.contains("intel ")
                || lower.contains("radeon") || lower.contains("geforce") || lower.contains("mesa ")) {
            return new Classified(F3Element.GPU, raw);
        }
        // Block/fluid property dump on the right (registry id or key: value)
        if (lower.contains(":") && !lower.startsWith("java") && looksLikeBlockProperty(trimmed)) {
            section = Section.BLOCK_DETAILS;
            return new Classified(F3Element.BLOCK_DETAILS, raw);
        }
        if (section == Section.BLOCK_DETAILS) {
            return new Classified(F3Element.BLOCK_DETAILS, raw);
        }
        if (lower.startsWith("debug:") || lower.startsWith("for help:")) {
            return new Classified(F3Element.HELP, raw);
        }
        return new Classified(F3Element.MISC_RIGHT, raw);
    }

    private static boolean equalsHeader(String lower, String header) {
        return lower.equals(header) || lower.equals(header + ":");
    }

    private static boolean looksLikeBlockCoords(String trimmed) {
        // "Block: 12 64 -30"
        String rest = trimmed.substring("Block:".length()).trim();
        String[] parts = rest.split("\\s+");
        if (parts.length < 3) {
            return false;
        }
        try {
            Integer.parseInt(parts[0]);
            Integer.parseInt(parts[1]);
            Integer.parseInt(parts[2]);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean looksLikeBlockProperty(String trimmed) {
        if (trimmed.startsWith("#") || trimmed.contains(":")) {
            // minecraft:stone or waterlogged: true
            return !trimmed.toLowerCase(Locale.ROOT).startsWith("xyz");
        }
        return false;
    }

    /** Rewrite Local Difficulty / Day depending on which toggles are hidden. */
    public static String rewriteDifficultyLine(String raw, boolean hideDifficulty, boolean hideDay) {
        if (!hideDifficulty && !hideDay) {
            return raw;
        }
        String plain = ChatFormatting.stripFormatting(raw);
        if (plain == null) {
            plain = raw;
        }
        if (hideDifficulty && hideDay) {
            return null;
        }
        if (hideDay) {
            String stripped = DAY_IN_DIFFICULTY.matcher(plain).replaceFirst("");
            return stripped.isBlank() ? null : stripped;
        }
        // hide difficulty, keep day
        Matcher m = DAY_CAPTURE.matcher(plain);
        if (m.find()) {
            return "Day " + m.group(1);
        }
        return null;
    }
}
