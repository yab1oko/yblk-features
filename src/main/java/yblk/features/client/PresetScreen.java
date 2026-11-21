package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import yblk.features.Features;
import yblk.features.client.ui.BaseSettingsScreen;
import yblk.features.client.ui.DarkButton;
import yblk.features.client.ui.TextField;
import yblk.features.network.ModNetwork;
import yblk.features.network.PresetPacket;
import yblk.features.preset.PresetStore;

import java.util.ArrayList;
import java.util.List;

/** Save, load, delete, import and export feature presets. Apply on a server needs operator. */
public class PresetScreen extends BaseSettingsScreen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFF9A9AA8;
    private static final int HOVER = 0xFF2A2A32;
    private static final int ROW = 14;

    @Override
    protected int buttonAccent() {
        return ACCENT;
    }

    @Override
    protected int buttonHover() {
        return HOVER;
    }

    private final List<String> names = new ArrayList<>();
    private TextField nameBox;
    private int selected;
    private String status = "";

    public PresetScreen(net.minecraft.client.gui.screens.Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".presets"), parent);
    }

    @Override
    protected void init() {
        clearWidgets();
        names.clear();
        names.addAll(PresetStore.names());
        selected = Mth.clamp(selected, 0, Math.max(0, names.size() - 1));

        int left = width / 2 - 140;
        nameBox = new TextField(font, left, height / 2 + 28, 160, 16,
                Component.translatable("screen." + Features.MODID + ".presets.name"));
        nameBox.setMaxLength(32);
        if (!names.isEmpty()) {
            nameBox.setValue(names.get(selected));
        }
        addRenderableWidget(nameBox);

        int x = left + 168;
        int y = height / 2 - 70;
        addRenderableWidget(button(x, y, "save", this::save));
        addRenderableWidget(button(x, y + 22, "load", this::load));
        addRenderableWidget(button(x, y + 44, "delete", this::delete));
        addRenderableWidget(button(x, y + 66, "export", this::exportPreset));
        addRenderableWidget(button(x, y + 88, "import", this::importPreset));
        addRenderableWidget(doneButton(width / 2 - 45, height - 28, 90, 18));
    }

    private DarkButton button(int x, int y, String key, Runnable action) {
        return new DarkButton(font, x, y, 100, 18,
                Component.translatable("screen." + Features.MODID + ".presets." + key),
                action, ACCENT, HOVER);
    }

    private void save() {
        if (PresetStore.save(nameBox.getValue())) {
            status = "";
            init();
        }
    }

    private void load() {
        String json = PresetStore.read(nameBox.getValue());
        if (json.isEmpty()) {
            return;
        }
        if (minecraft != null && minecraft.getSingleplayerServer() == null && minecraft.player != null) {
            if (!minecraft.player.hasPermissions(2)) {
                status = Component.translatable("screen." + Features.MODID + ".presets.denied").getString();
                return;
            }
            ModNetwork.sendToServer(new PresetPacket(json));
        } else {
            PresetStore.apply(json);
        }
        status = Component.translatable("screen." + Features.MODID + ".presets.applied").getString();
    }

    private void delete() {
        PresetStore.delete(nameBox.getValue());
        init();
    }

    private void exportPreset() {
        String json = PresetStore.read(nameBox.getValue());
        if (json.isEmpty()) {
            json = PresetStore.snapshotJson();
        }
        if (minecraft != null) {
            minecraft.keyboardHandler.setClipboard(json);
        }
    }

    private void importPreset() {
        if (minecraft == null) {
            return;
        }
        String json = minecraft.keyboardHandler.getClipboard();
        String name = nameBox.getValue().isBlank() ? "imported" : nameBox.getValue();
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            java.nio.file.Files.createDirectories(PresetStore.directory());
            java.nio.file.Files.writeString(
                    PresetStore.directory().resolve(PresetStore.sanitize(name) + ".json"), json);
            init();
        } catch (java.io.IOException ignored) {
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        drawScreenTitle(graphics, 12);
        int left = width / 2 - 140;
        int top = height / 2 - 78;
        graphics.fill(left - 4, top - 4, left + 156, top + 100, PANEL);
        if (names.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".presets.empty"),
                    left, top + 4, 0x808088, false);
        }
        for (int i = 0; i < names.size() && i < 6; i++) {
            int y = top + i * ROW;
            boolean on = i == selected;
            if (on) {
                graphics.fill(left, y, left + 148, y + ROW - 1, 0xFF2A2A32);
            }
            graphics.drawString(font, names.get(i), left + 4, y + 3, on ? 0xF0F0F0 : 0xB0B0BC, false);
        }
        if (!status.isEmpty()) {
            graphics.drawString(font, status, left, height / 2 + 48, 0xFFC8C8A0, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = width / 2 - 140;
        int top = height / 2 - 78;
        if (button == 0 && mouseX >= left && mouseX < left + 148 && mouseY >= top && mouseY < top + 6 * ROW) {
            int row = (int) ((mouseY - top) / ROW);
            if (row >= 0 && row < names.size()) {
                selected = row;
                nameBox.setValue(names.get(row));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
