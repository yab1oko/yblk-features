package yblk.features.world;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Per-dimension elytra, firework boost, glide limit, and creative-style flight. */
public final class ElytraRules {
    public record Rule(ResourceLocation dimension, boolean elytra, boolean firework, int seconds, boolean flight,
                       String stored) {
    }

    private ElytraRules() {
    }

    public static Rule parse(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] parts = line.trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (parts.length < 5) {
            return null;
        }
        ResourceLocation dimension = PortalRules.dimension(parts[0]);
        Boolean elytra = allow(parts[1]);
        Boolean firework = allow(parts[2]);
        Boolean flight = allow(parts[4]);
        if (dimension == null || elytra == null || firework == null || flight == null) {
            return null;
        }
        int seconds;
        try {
            seconds = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (seconds < 0 || seconds > 36000) {
            return null;
        }
        return new Rule(dimension, elytra, firework, seconds, flight, stored(dimension, elytra, firework, seconds, flight));
    }

    public static Rule build(String dimensionText, boolean elytra, boolean firework, String secondsText, boolean flight) {
        ResourceLocation dimension = PortalRules.dimension(dimensionText);
        if (dimension == null) {
            return null;
        }
        int seconds = 0;
        if (secondsText != null && !secondsText.isBlank()) {
            try {
                seconds = Integer.parseInt(secondsText.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (seconds < 0 || seconds > 36000) {
            return null;
        }
        return new Rule(dimension, elytra, firework, seconds, flight, stored(dimension, elytra, firework, seconds, flight));
    }

    public static List<String> upsert(List<String> rules, Rule rule) {
        List<String> next = new ArrayList<>();
        boolean placed = false;
        if (rules != null) {
            for (String raw : rules) {
                Rule existing = parse(raw);
                if (existing != null && existing.dimension.equals(rule.dimension)) {
                    if (!placed) {
                        next.add(rule.stored);
                        placed = true;
                    }
                } else if (raw != null && !raw.isBlank()) {
                    next.add(raw.trim());
                }
            }
        }
        if (!placed) {
            next.add(rule.stored);
        }
        return next;
    }

    public static List<String> removeStored(List<String> rules, String stored) {
        List<String> next = new ArrayList<>();
        if (rules == null) {
            return next;
        }
        for (String raw : rules) {
            if (raw != null && !raw.trim().equalsIgnoreCase(stored)) {
                next.add(raw.trim());
            }
        }
        return next;
    }

    public static Rule find(List<String> rules, ResourceLocation dimension) {
        if (rules == null || dimension == null) {
            return null;
        }
        for (String raw : rules) {
            Rule rule = parse(raw);
            if (rule != null && rule.dimension.equals(dimension)) {
                return rule;
            }
        }
        return null;
    }

    private static Boolean allow(String token) {
        return switch (token) {
            case "allow", "allowed", "on", "true" -> Boolean.TRUE;
            case "deny", "denied", "off", "false", "block" -> Boolean.FALSE;
            default -> null;
        };
    }

    public static String word(boolean allowed) {
        return allowed ? "allow" : "deny";
    }

    private static String stored(ResourceLocation dimension, boolean elytra, boolean firework, int seconds, boolean flight) {
        return dimension + " " + word(elytra) + " " + word(firework) + " " + seconds + " " + word(flight);
    }
}
