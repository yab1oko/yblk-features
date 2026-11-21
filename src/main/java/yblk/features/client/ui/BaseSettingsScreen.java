package yblk.features.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import yblk.features.Features;
import yblk.features.client.FeatureSettingsBackdrop;
import yblk.features.client.SettingFocus;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared settings shell: backdrop, pause, close, title.
 * Screens keep their own panel layout and pass the accent through the widgets.
 */
public abstract class BaseSettingsScreen extends Screen {
    protected final Screen parent;
    private final List<Tip> tips = new ArrayList<>();
    private boolean dirty;
    protected boolean asking;

    protected BaseSettingsScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    protected void markDirty() {
        dirty = true;
    }

    protected void clearDirty() {
        dirty = false;
        asking = false;
    }

    /** Save the open edits. The screen stays open. */
    protected void saveUnsaved() {
    }

    protected void tipKey(int x, int y, int w, int h, String key) {
        String tip = firstTip(key);
        if (tip == null) {
            tip = firstTip(sibling(key));
        }
        if (tip != null) {
            this.tip(x, y, w, h, Component.translatable(tip));
        }
    }

    private static String firstTip(String key) {
        if (key == null) {
            return null;
        }
        String tip = translated(key + ".tip");
        return tip != null ? tip : translated(key + ".desc");
    }

    /** A checked row and its off label share one tooltip. */
    private static String sibling(String key) {
        if (key.endsWith(".on")) {
            return key.substring(0, key.length() - 3) + ".off";
        }
        if (key.endsWith(".off")) {
            return key.substring(0, key.length() - 4) + ".on";
        }
        return null;
    }

    private static String translated(String key) {
        return Component.translatable(key).getString().equals(key) ? null : key;
    }

    protected void tip(int x, int y, int w, int h, Component text) {
        if (text != null && w > 0 && h > 0) {
            tips.add(new Tip(x, y, w, h, text));
        }
    }

    protected void dirtyDot(GuiGraphics graphics, int x, int y, boolean changed) {
        if (changed) {
            graphics.fill(x, y, x + 3, y + 3, 0xFFC4A15A);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        FeatureSettingsBackdrop.render(graphics, width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (Tip tip : tips) {
            if (mouseX >= tip.x && mouseX < tip.x + tip.w && mouseY >= tip.y && mouseY < tip.y + tip.h) {
                graphics.renderTooltip(font, tip.text, mouseX, mouseY);
                // Tooltip text sits in the font buffer. A living preview drawn later
                // ends that buffer with depth testing on, so the glyphs vanish and
                // only the purple plate remains. Flush while depth is off.
                graphics.flush();
                break;
            }
        }
        tips.clear();
        if (asking) {
            int w = 220;
            int h = 72;
            int x = (width - w) / 2;
            int y = (height - h) / 2;
            graphics.fill(0, 0, width, height, 0xA0000000);
            graphics.fill(x, y, x + w, y + h, 0xFF16161C);
            graphics.drawCenteredString(font, Component.translatable("screen." + Features.MODID + ".unsaved"),
                    width / 2, y + 12, 0xF0F0F0);
            graphics.drawCenteredString(font, Component.translatable("screen." + Features.MODID + ".unsaved.save"),
                    x + 60, y + 44, 0xFFD8C090);
            graphics.drawCenteredString(font, Component.translatable("screen." + Features.MODID + ".unsaved.discard"),
                    x + 160, y + 44, 0xFFC96060);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (asking && button == 0) {
            int w = 220;
            int h = 72;
            int x = (width - w) / 2;
            int y = (height - h) / 2;
            if (mouseY >= y + 36 && mouseY < y + 60) {
                if (mouseX >= x + 16 && mouseX < x + 104) {
                    saveUnsaved();
                    clearDirty();
                    closeNow();
                } else if (mouseX >= x + 116 && mouseX < x + 204) {
                    clearDirty();
                    closeNow();
                }
            }
            return true;
        }
        if (button == 0 && ScrollBar.press(mouseX, mouseY)) {
            setDragging(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && ScrollBar.drag(mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            ScrollBar.release();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean scrollGrabBlocked() {
        return asking;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (asking && keyCode == 256) {
            clearDirty();
            closeNow();
            return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (!asking && (keyCode == 257 || keyCode == 335)) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        if (dirty && !asking) {
            asking = true;
            return;
        }
        closeNow();
    }

    private void closeNow() {
        SettingFocus.clear();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    protected void drawScreenTitle(GuiGraphics graphics, int y) {
        graphics.drawCenteredString(font, title, width / 2, y, 0xF0F0F0);
    }

    protected int doneButtonY(int buttonH, int bottomPad) {
        return height - bottomPad - buttonH;
    }

    protected abstract int buttonAccent();

    protected abstract int buttonHover();

    /** Same constructor shape the screens used for their private buttons. */
    protected final DarkButton darkButton(int x, int y, int w, int h, Component message, Runnable action) {
        return new DarkButton(font, x, y, w, h, message, action, buttonAccent(), buttonHover());
    }

    protected DarkButton doneButton(int x, int y, int w, int h) {
        return darkButton(x, y, w, h, Component.translatable("gui.done"), this::onClose);
    }

    private record Tip(int x, int y, int w, int h, Component text) {
    }
}
