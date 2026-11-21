package yblk.features.mobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** add/remove/replace/xp lines for one entity, tag, or mod. */
public final class MobDropRules {
    public enum Kind {
        ADD, REMOVE, REPLACE, XP
    }

    public record Rule(Kind kind, String target, String item, String item2, float chance, int min, int max,
                       int looting, boolean playerOnly, float xp, boolean enabled, String stored) {
    }

    private MobDropRules() {
    }

    public static List<Rule> compile(List<String> lines) {
        List<Rule> rules = new ArrayList<>();
        if (lines == null) {
            return rules;
        }
        for (String line : lines) {
            Rule rule = parse(line);
            if (rule != null) {
                rules.add(rule);
            }
        }
        return rules;
    }

    public static Rule parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.trim().split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        Kind kind = switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "add" -> Kind.ADD;
            case "remove" -> Kind.REMOVE;
            case "replace" -> Kind.REPLACE;
            case "xp" -> Kind.XP;
            default -> null;
        };
        if (kind == null) {
            return null;
        }
        String target = TargetMatch.entity(parts[1]);
        if (target.isEmpty()) {
            return null;
        }
        int end = parts.length;
        boolean player = false;
        boolean enabled = true;
        while (end > 2) {
            String tail = parts[end - 1];
            if ("player".equalsIgnoreCase(tail)) {
                player = true;
                end--;
                continue;
            }
            if ("off".equalsIgnoreCase(tail)) {
                enabled = false;
                end--;
                continue;
            }
            break;
        }
        final int cut = end;
        final boolean playerKill = player;
        final boolean active = enabled;
        return switch (kind) {
            case REMOVE -> cut >= 3 ? remove(target, parts[2], playerKill) : null;
            case XP -> cut >= 3 ? xp(target, parts[2], playerKill) : null;
            case ADD -> cut >= 3 ? add(target, parts, cut, playerKill, active) : null;
            case REPLACE -> cut >= 4 ? replace(target, parts, cut, playerKill) : null;
        };
    }

    public static Rule add(String target, String item, float chance, int min, int max, int looting, boolean player) {
        return add(target, item, chance, min, max, looting, player, true);
    }

    public static Rule add(String target, String item, float chance, int min, int max, int looting, boolean player, boolean enabled) {
        String entity = TargetMatch.entity(target);
        String drop = TargetMatch.item(item);
        if (entity.isEmpty() || drop.isEmpty()) {
            return null;
        }
        int lo = Math.max(0, Math.min(min, max));
        int hi = Math.max(lo, Math.max(min, max));
        float c = chanceOf(chance);
        String stored = "add " + entity + " " + drop + " " + trim(c) + " " + lo + " " + hi + " " + Math.max(0, looting)
                + (enabled ? "" : " off") + (player ? " player" : "");
        return new Rule(Kind.ADD, entity, drop, "", c, lo, hi, Math.max(0, looting), player, 1f, enabled, stored);
    }

    public static Rule remove(String target, String item, boolean player) {
        String entity = TargetMatch.entity(target);
        String drop = TargetMatch.item(item);
        if (entity.isEmpty() || drop.isEmpty()) {
            return null;
        }
        String stored = "remove " + entity + " " + drop + (player ? " player" : "");
        return new Rule(Kind.REMOVE, entity, drop, "", 1f, 0, 0, 0, player, 1f, true, stored);
    }

    public static Rule replace(String target, String from, String to, float chance, int min, int max, int looting, boolean player) {
        String entity = TargetMatch.entity(target);
        String oldItem = TargetMatch.item(from);
        String newItem = TargetMatch.item(to);
        if (entity.isEmpty() || oldItem.isEmpty() || newItem.isEmpty()) {
            return null;
        }
        int lo = Math.max(0, Math.min(min, max));
        int hi = Math.max(lo, Math.max(min, max));
        float c = chanceOf(chance);
        String stored = "replace " + entity + " " + oldItem + " " + newItem + " " + trim(c) + " " + lo + " " + hi + " "
                + Math.max(0, looting) + (player ? " player" : "");
        return new Rule(Kind.REPLACE, entity, oldItem, newItem, c, lo, hi, Math.max(0, looting), player, 1f, true, stored);
    }

    public static Rule xp(String target, String multiplier, boolean player) {
        String entity = TargetMatch.entity(target);
        if (entity.isEmpty()) {
            return null;
        }
        float value = parseFloat(multiplier, -1f);
        if (value < 0f) {
            return null;
        }
        String stored = "xp " + entity + " " + trim(value) + (player ? " player" : "");
        return new Rule(Kind.XP, entity, "", "", 1f, 0, 0, 0, player, value, true, stored);
    }

    public static List<String> upsert(List<String> rules, Rule rule) {
        List<String> next = new ArrayList<>();
        if (rules != null) {
            for (String line : rules) {
                Rule parsed = parse(line);
                if (parsed == null || !sameSlot(parsed, rule)) {
                    next.add(line);
                }
            }
        }
        next.add(rule.stored());
        return next;
    }

    public static List<String> removeStored(List<String> rules, String stored) {
        List<String> next = new ArrayList<>();
        if (rules == null) {
            return next;
        }
        for (String line : rules) {
            if (!line.equals(stored)) {
                next.add(line);
            }
        }
        return next;
    }

    private static boolean sameSlot(Rule a, Rule b) {
        return a.kind == b.kind && a.target.equals(b.target) && a.item.equals(b.item) && a.item2.equals(b.item2);
    }

    private static Rule add(String target, String[] parts, int end, boolean player, boolean enabled) {
        String item = parts[2];
        float chance = end > 3 ? parseFloat(parts[3], 1f) : 1f;
        int min = end > 4 ? parseInt(parts[4], 1) : 1;
        int max = end > 5 ? parseInt(parts[5], min) : min;
        int looting = end > 6 ? parseInt(parts[6], 0) : 0;
        return add(target, item, chance, min, max, looting, player, enabled);
    }

    private static Rule replace(String target, String[] parts, int end, boolean player) {
        float chance = end > 4 ? parseFloat(parts[4], 1f) : 1f;
        int min = end > 5 ? parseInt(parts[5], 1) : 1;
        int max = end > 6 ? parseInt(parts[6], min) : min;
        int looting = end > 7 ? parseInt(parts[7], 0) : 0;
        return replace(target, parts[2], parts[3], chance, min, max, looting, player);
    }

    private static float chanceOf(float chance) {
        if (chance > 1f && chance <= 100f) {
            chance = chance / 100f;
        }
        if (chance < 0f) {
            return 0f;
        }
        return Math.min(1f, chance);
    }

    private static float parseFloat(String raw, float fallback) {
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String trim(float value) {
        if (value == (int) value) {
            return Integer.toString((int) value);
        }
        return Float.toString(value);
    }
}
