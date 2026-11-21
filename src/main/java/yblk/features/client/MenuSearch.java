package yblk.features.client;

import java.util.Locale;

/** Fuzzy match: word order, one-layout typos, and EN↔RU keyboard layout. */
public final class MenuSearch {
    private static final String EN = "qwertyuiop[]asdfghjkl;'zxcvbnm,.";
    private static final String RU = "йцукенгшщзхъфывапролджэячсмитьбю";

    private MenuSearch() {
    }

    public static boolean matches(String query, String... haystacks) {
        String q = norm(query);
        if (q.isEmpty()) {
            return true;
        }
        StringBuilder joined = new StringBuilder();
        for (String hay : haystacks) {
            if (hay != null && !hay.isEmpty()) {
                joined.append(' ').append(norm(hay));
            }
        }
        String hay = joined.toString();
        return wordsMatch(q, hay) || wordsMatch(swapLayout(q), hay) || wordsMatch(q, swapLayout(hay));
    }

    private static boolean wordsMatch(String query, String hay) {
        String[] words = query.split("\\s+");
        if (words.length == 0) {
            return true;
        }
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!wordHits(word, hay)) {
                return false;
            }
        }
        return true;
    }

    private static boolean wordHits(String word, String hay) {
        if (hay.contains(word)) {
            return true;
        }
        int limit = word.length() >= 8 ? 2 : word.length() >= 4 ? 1 : 0;
        if (limit == 0) {
            return false;
        }
        for (String token : hay.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (Math.abs(token.length() - word.length()) > limit) {
                continue;
            }
            if (distance(word, token) <= limit) {
                return true;
            }
        }
        return false;
    }

    static int distance(String a, String b) {
        int n = a.length();
        int m = b.length();
        int[] prev = new int[m + 1];
        int[] cur = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            cur[0] = i;
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = cur;
            cur = swap;
        }
        return prev[m];
    }

    private static String norm(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.toLowerCase(Locale.ROOT).replace('ё', 'е').trim();
    }

    static String swapLayout(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int en = EN.indexOf(c);
            int ru = RU.indexOf(c);
            if (en >= 0 && en < RU.length()) {
                out.append(RU.charAt(en));
            } else if (ru >= 0 && ru < EN.length()) {
                out.append(EN.charAt(ru));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
