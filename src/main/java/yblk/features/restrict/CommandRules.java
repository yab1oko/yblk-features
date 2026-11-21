package yblk.features.restrict;

import net.minecraft.world.entity.player.Player;
import yblk.features.config.FeaturesConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A rule is {@code roles:pattern}. Roles are a mix of {@code p} (level 0), {@code m} (level 1),
 * and {@code o} (level 2+). A missing prefix means players. One token blocks that command and
 * anything under it. {@code *} matches one token; a trailing {@code *} also hides the parent node.
 */
public final class CommandRules {
    private CommandRules() {
    }

    public record Rule(boolean player, boolean moderator, boolean operator, String[] tokens, String stored) {
    }

    public static List<Rule> compile() {
        List<Rule> out = new ArrayList<>();
        for (String raw : FeaturesConfig.commandRules) {
            Rule rule = parse(raw);
            if (rule != null) {
                out.add(rule);
            }
        }
        return out;
    }

    public static Rule parse(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        boolean player = false;
        boolean moderator = false;
        boolean operator = false;
        String pattern = s;
        int colon = s.indexOf(':');
        if (colon > 0 && colon <= 3) {
            String prefix = s.substring(0, colon).toLowerCase(Locale.ROOT);
            boolean roles = !prefix.isEmpty();
            for (int i = 0; i < prefix.length() && roles; i++) {
                char c = prefix.charAt(i);
                if (c == 'p') {
                    player = true;
                } else if (c == 'm') {
                    moderator = true;
                } else if (c == 'o') {
                    operator = true;
                } else {
                    roles = false;
                }
            }
            if (roles && (player || moderator || operator)) {
                pattern = s.substring(colon + 1).trim();
            } else {
                player = true;
                moderator = false;
                operator = false;
                pattern = s;
            }
        } else {
            player = true;
        }
        if (pattern.startsWith("/")) {
            pattern = pattern.substring(1).trim();
        }
        if (pattern.isEmpty()) {
            return null;
        }
        String[] tokens = pattern.toLowerCase(Locale.ROOT).split("\\s+");
        if (tokens.length == 0 || tokens[0].isEmpty()) {
            return null;
        }
        StringBuilder stored = new StringBuilder();
        if (player) {
            stored.append('p');
        }
        if (moderator) {
            stored.append('m');
        }
        if (operator) {
            stored.append('o');
        }
        stored.append(':').append(String.join(" ", tokens));
        return new Rule(player, moderator, operator, tokens, stored.toString());
    }

    /** {@code audience} 0 = players, 1 = players and moderators, 2 = every role. */
    public static String normalize(String typed, int audience) {
        if (typed == null) {
            return null;
        }
        String s = typed.trim();
        if (s.startsWith("/")) {
            s = s.substring(1).trim();
        }
        if (s.isEmpty()) {
            return null;
        }
        if (!s.matches("(?i)[pmo]{1,3}:.*")) {
            String prefix = switch (audience) {
                case 1 -> "pm:";
                case 2 -> "pmo:";
                default -> "p:";
            };
            s = prefix + s;
        }
        Rule rule = parse(s);
        return rule == null ? null : rule.stored;
    }

    public static boolean blocked(Player player, String raw) {
        if (!FeaturesConfig.commandBlockerEnabled || player == null) {
            return false;
        }
        return blocked(compile(), player, raw);
    }

    public static boolean blocked(List<Rule> rules, Player player, String raw) {
        String[] tokens = tokenize(raw);
        if (matches(rules, player, tokens)) {
            return true;
        }
        boolean execute = false;
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equalsIgnoreCase("execute")) {
                execute = true;
            }
            if (execute && tokens[i].equalsIgnoreCase("run") && i + 1 < tokens.length) {
                if (matches(rules, player, Arrays.copyOfRange(tokens, i + 1, tokens.length))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hides(Player player, List<String> path, List<Rule> rules) {
        if (!FeaturesConfig.commandBlockerEnabled || player == null || path.isEmpty()) {
            return false;
        }
        return matches(rules, player, path.toArray(String[]::new));
    }

    private static boolean matches(List<Rule> rules, Player player, String[] input) {
        if (input.length == 0) {
            return false;
        }
        for (Rule rule : rules) {
            if (applies(rule, player) && covers(rule.tokens, input)) {
                return true;
            }
        }
        return false;
    }

    private static boolean applies(Rule rule, Player player) {
        if (player.hasPermissions(2)) {
            return rule.operator;
        }
        if (player.hasPermissions(1)) {
            return rule.moderator;
        }
        return rule.player;
    }

    static boolean covers(String[] pattern, String[] input) {
        if (pattern.length == 0 || input.length == 0) {
            return false;
        }
        int n = Math.min(pattern.length, input.length);
        for (int i = 0; i < n; i++) {
            if (!tokenMatches(pattern[i], input[i])) {
                return false;
            }
        }
        if (input.length >= pattern.length) {
            return true;
        }
        for (int i = input.length; i < pattern.length; i++) {
            if (!"*".equals(pattern[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean tokenMatches(String pattern, String token) {
        if ("*".equals(pattern)) {
            return true;
        }
        if (pattern.endsWith("*") && pattern.length() > 1) {
            return token.toLowerCase(Locale.ROOT).startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return pattern.equalsIgnoreCase(token);
    }

    public static String[] tokenize(String raw) {
        if (raw == null) {
            return new String[0];
        }
        String s = raw.trim();
        if (s.startsWith("/")) {
            s = s.substring(1).trim();
        }
        if (s.isEmpty()) {
            return new String[0];
        }
        return s.split("\\s+");
    }
}
