package yblk.features.server;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** & and section-sign colors, the same codes the server list understands. */
public final class LegacyText {
    private LegacyText() {
    }

    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        MutableComponent out = Component.empty();
        StringBuilder run = new StringBuilder();
        Style style = Style.EMPTY;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c == '&' || c == '\u00A7') && i + 1 < raw.length()) {
                ChatFormatting format = ChatFormatting.getByCode(Character.toLowerCase(raw.charAt(i + 1)));
                if (format != null) {
                    append(out, run, style);
                    style = format == ChatFormatting.RESET ? Style.EMPTY : style.applyFormat(format);
                    i++;
                    continue;
                }
            }
            run.append(c);
        }
        append(out, run, style);
        return out;
    }

    private static void append(MutableComponent out, StringBuilder run, Style style) {
        if (run.length() == 0) {
            return;
        }
        out.append(Component.literal(run.toString()).setStyle(style));
        run.setLength(0);
    }
}
