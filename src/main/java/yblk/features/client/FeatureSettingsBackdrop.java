package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared backdrop for feature settings screens: sharp world behind, light dim only.
 * No vanilla gradient / dirt / blur stack — just ~13% darkness.
 */
public final class FeatureSettingsBackdrop {
    /** ~13% alpha black (between the requested 10–15%). */
    public static final int DIM = 0x21000000;

    private FeatureSettingsBackdrop() {
    }

    public static void render(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, DIM);
    }
}
