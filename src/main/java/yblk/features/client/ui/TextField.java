package yblk.features.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Edit box with no extra chrome — same pixels as a vanilla field. */
public class TextField extends EditBox {
    public TextField(Font font, int x, int y, int w, int h, Component message) {
        super(font, x, y, w, h, message);
    }
}
