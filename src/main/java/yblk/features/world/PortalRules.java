package yblk.features.world;

import net.minecraft.resources.ResourceLocation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One portal rule per dimension: allow, block, or unlock on an in-game day or a real date. */
public final class PortalRules {
    public enum Mode {
        ALLOW, BLOCK, DAY, DATE
    }

    public record Rule(ResourceLocation dimension, Mode mode, int day, LocalDate date, String stored) {
    }

    private PortalRules() {
    }

    public static Rule parse(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] parts = line.trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        ResourceLocation dimension = dimension(parts[0]);
        if (dimension == null) {
            return null;
        }
        Mode mode = switch (parts[1]) {
            case "allow", "allowed" -> Mode.ALLOW;
            case "block", "blocked", "deny" -> Mode.BLOCK;
            case "day" -> Mode.DAY;
            case "date" -> Mode.DATE;
            default -> null;
        };
        if (mode == null) {
            return null;
        }
        int day = 0;
        LocalDate date = null;
        if (mode == Mode.DAY) {
            if (parts.length < 3) {
                return null;
            }
            try {
                day = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return null;
            }
            if (day < 0 || day > 1_000_000) {
                return null;
            }
        } else if (mode == Mode.DATE) {
            if (parts.length < 3) {
                return null;
            }
            try {
                date = LocalDate.parse(parts[2]);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        return new Rule(dimension, mode, day, date, stored(dimension, mode, day, date));
    }

    public static Rule build(String dimensionText, int modeIndex, String valueText) {
        Mode mode = switch (Math.max(0, modeIndex) % 4) {
            case 1 -> Mode.BLOCK;
            case 2 -> Mode.DAY;
            case 3 -> Mode.DATE;
            default -> Mode.ALLOW;
        };
        ResourceLocation dimension = dimension(dimensionText);
        if (dimension == null) {
            return null;
        }
        int day = 0;
        LocalDate date = null;
        if (mode == Mode.DAY) {
            try {
                day = Integer.parseInt(valueText == null ? "" : valueText.trim());
            } catch (NumberFormatException e) {
                return null;
            }
            if (day < 0 || day > 1_000_000) {
                return null;
            }
        } else if (mode == Mode.DATE) {
            try {
                date = LocalDate.parse(valueText == null ? "" : valueText.trim());
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        return new Rule(dimension, mode, day, date, stored(dimension, mode, day, date));
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

    public static List<String> unlock(List<String> rules, ResourceLocation dimension) {
        Rule allow = new Rule(dimension, Mode.ALLOW, 0, null, stored(dimension, Mode.ALLOW, 0, null));
        return upsert(rules, allow);
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

    public static boolean closed(Rule rule, long worldDay, LocalDate today) {
        if (rule == null) {
            return false;
        }
        return switch (rule.mode) {
            case ALLOW -> false;
            case BLOCK -> true;
            case DAY -> worldDay < rule.day;
            case DATE -> today.isBefore(rule.date);
        };
    }

    public static ResourceLocation dimension(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }
        text = switch (text) {
            case "overworld", "minecraft:overworld" -> "minecraft:overworld";
            case "nether", "the_nether", "minecraft:the_nether" -> "minecraft:the_nether";
            case "end", "the_end", "minecraft:the_end" -> "minecraft:the_end";
            default -> text.contains(":") ? text : "minecraft:" + text;
        };
        return ResourceLocation.tryParse(text);
    }

    public static String label(ResourceLocation id) {
        if (id == null) {
            return "";
        }
        if ("minecraft".equals(id.getNamespace())) {
            return switch (id.getPath()) {
                case "the_nether" -> "nether";
                case "the_end" -> "end";
                default -> id.getPath();
            };
        }
        return id.toString();
    }

    private static String stored(ResourceLocation dimension, Mode mode, int day, LocalDate date) {
        return switch (mode) {
            case ALLOW -> dimension + " allow";
            case BLOCK -> dimension + " block";
            case DAY -> dimension + " day " + day;
            case DATE -> dimension + " date " + date;
        };
    }
}
