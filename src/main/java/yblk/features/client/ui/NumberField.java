package yblk.features.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.function.Consumer;

/** Numeric edit box. Parsing matches the Wetness settings fields. */
public final class NumberField extends TextField {
    private NumberField(Font font, int w, int h) {
        super(font, 0, 0, w, h, Component.empty());
    }

    public static NumberField decimal(Font font, float value, boolean editable, Consumer<Float> setter) {
        NumberField box = new NumberField(font, 48, 14);
        box.setMaxLength(6);
        box.setValue(formatDecimal(value));
        box.setEditable(editable);
        box.setResponder(raw -> {
            if (!editable || raw == null || raw.isBlank()) {
                return;
            }
            try {
                setter.accept(Float.parseFloat(raw.trim()));
            } catch (NumberFormatException ignored) {
            }
        });
        return box;
    }

    public static NumberField percent(Font font, float fraction, boolean editable, Consumer<Float> setter) {
        NumberField box = new NumberField(font, 48, 14);
        box.setMaxLength(5);
        box.setValue(String.format(Locale.ROOT, "%d", Math.round(fraction * 100f)));
        box.setEditable(editable);
        box.setResponder(raw -> {
            if (!editable || raw == null || raw.isBlank()) {
                return;
            }
            try {
                setter.accept(Mth.clamp(Float.parseFloat(raw.trim()) / 100f, 0f, 1f));
            } catch (NumberFormatException ignored) {
            }
        });
        return box;
    }

    public static NumberField integer(Font font, int value, int min, int max, boolean editable, Consumer<Integer> setter) {
        NumberField box = new NumberField(font, 48, 14);
        box.setMaxLength(5);
        box.setValue(Integer.toString(value));
        box.setEditable(editable);
        box.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        box.setResponder(raw -> {
            if (!editable || raw == null || raw.isBlank()) {
                return;
            }
            try {
                setter.accept(Mth.clamp(Integer.parseInt(raw.trim()), min, max));
            } catch (NumberFormatException ignored) {
            }
        });
        return box;
    }

    public static String formatDecimal(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001f) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
