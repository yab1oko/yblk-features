package yblk.features.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public final class ScrollBar {
    private static final List<Grip> BUILDING = new ArrayList<>();
    private static final List<Grip> ACTIVE = new ArrayList<>();
    private static Grip held;

    private ScrollBar() {
    }

    /** Settings-panel scrollbar: 2px track inset 4px from the panel edges. */
    public static void draw(GuiGraphics graphics, int right, int panelTop, int panelBottom,
                            int scroll, int maxScroll, int thumbColor, IntConsumer onScroll) {
        if (maxScroll <= 0) {
            return;
        }
        int trackH = Math.max(1, panelBottom - panelTop - 8);
        int thumbH = Math.min(trackH, Math.max(12, trackH * trackH / (trackH + maxScroll)));
        int thumbY = panelTop + 4 + (trackH - thumbH) * scroll / maxScroll;
        graphics.fill(right - 5, panelTop + 4, right - 3, panelBottom - 4, 0xFF2A2A32);
        graphics.fill(right - 5, thumbY, right - 3, thumbY + thumbH, thumbColor);
        if (onScroll != null) {
            BUILDING.add(new Grip(right, panelTop, panelBottom, maxScroll, thumbY, thumbH, onScroll));
        }
    }

    /** Keep the grips painted this frame. Other screens drop them. */
    public static void seal(boolean settings) {
        if (settings) {
            ACTIVE.clear();
            ACTIVE.addAll(BUILDING);
        } else if (held == null) {
            ACTIVE.clear();
        }
        BUILDING.clear();
    }

    public static boolean press(double mouseX, double mouseY) {
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Grip grip = ACTIVE.get(i);
            if (!grip.hit(mouseX, mouseY)) {
                continue;
            }
            held = grip;
            grip.grab = grip.thumbContains(mouseY) ? (int) mouseY - grip.thumbY : grip.thumbH / 2;
            grip.onScroll.accept(grip.scrollAt(mouseY));
            return true;
        }
        return false;
    }

    public static boolean drag(double mouseY) {
        if (held == null) {
            return false;
        }
        held.onScroll.accept(held.scrollAt(mouseY));
        return true;
    }

    public static void release() {
        held = null;
    }

    private static final class Grip {
        private final int right;
        private final int top;
        private final int bottom;
        private final int max;
        private final int thumbY;
        private final int thumbH;
        private final IntConsumer onScroll;
        private int grab;

        private Grip(int right, int top, int bottom, int max, int thumbY, int thumbH, IntConsumer onScroll) {
            this.right = right;
            this.top = top;
            this.bottom = bottom;
            this.max = max;
            this.thumbY = thumbY;
            this.thumbH = thumbH;
            this.onScroll = onScroll;
        }

        private boolean hit(double mouseX, double mouseY) {
            return mouseX >= right - 8 && mouseX < right && mouseY >= top && mouseY < bottom;
        }

        private boolean thumbContains(double mouseY) {
            return mouseY >= thumbY && mouseY < thumbY + thumbH;
        }

        private int scrollAt(double mouseY) {
            int trackH = Math.max(1, bottom - top - 8);
            int travel = Math.max(1, trackH - thumbH);
            int local = (int) mouseY - grab - (top + 4);
            return Mth.clamp(local * max / travel, 0, max);
        }
    }
}
