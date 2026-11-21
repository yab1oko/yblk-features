package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** One settings row to outline after a search jump. Cleared when that screen closes. */
public final class SettingFocus {
    private static String key = "";

    private SettingFocus() {
    }

    public static void arm(String translationKey) {
        key = translationKey == null ? "" : translationKey;
    }

    public static void clear() {
        key = "";
    }

    public static void mark(GuiGraphics graphics, Component label, int x, int y, int w, int h) {
        if (key.isEmpty() || !(label.getContents() instanceof TranslatableContents contents)) {
            return;
        }
        if (!key.equals(contents.getKey())) {
            return;
        }
        graphics.fill(x - 2, y - 1, x + Math.max(8, w) + 2, y + h + 1, 0x66E8C36A);
    }
}
