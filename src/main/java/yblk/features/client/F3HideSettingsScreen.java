package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.f3.F3Category;
import yblk.features.f3.F3Element;
import yblk.features.f3.F3Hide;

import java.util.ArrayList;
import java.util.List;

public class F3HideSettingsScreen extends Screen {
    private static final int PANEL = 0xFF16161C;
    private static final int PREVIEW_BG = 0xE0101014;
    private static final int ACCENT = 0xFF50A0E0;
    private static final int CHECK = 9;
    private static final int ROW = 15;
    private static final int ACTION_H = 16;
    private static final int CATEGORY_H = 16;
    private static final int SETTINGS_W = 210;
    private static final int GAP = 8;
    private static final int PREVIEW_LINE = 9;

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private int scroll;
    private int maxScroll;
    private int settingsL;
    private int settingsR;
    private int previewL;
    private int previewR;
    private int panelTop;
    private int panelBottom;
    private int contentTop;
    private int contentH;
    private boolean canEdit;

    public F3HideSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".f3_hide"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        hits.clear();
        canEdit = F3Hide.canEdit(minecraft != null ? minecraft.player : null);

        int btnH = 18;
        int bottomPad = 6;
        int sidePad = 8;
        int usable = width - sidePad * 2;
        int settingsW = Math.min(SETTINGS_W, Math.max(160, usable * 2 / 5));
        settingsR = width - sidePad;
        settingsL = settingsR - settingsW;
        previewL = sidePad;
        previewR = settingsL - GAP;
        panelTop = 22;
        panelBottom = height - bottomPad - btnH - 6;
        contentTop = panelTop + 8;

        contentH = measureContentHeight();
        maxScroll = Math.max(0, contentH - Math.max(1, panelBottom - contentTop - 4));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        addRenderableWidget(new DarkButton(settingsL + (settingsW - 90) / 2, height - bottomPad - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    private int measureContentHeight() {
        int y = 0;
        if (!canEdit) {
            y += 18;
        }
        y += 12 + 12; // hint
        y += ACTION_H + 6; // show-all / hide-all
        for (F3Category category : F3Category.values()) {
            y += 6 + CATEGORY_H + 2; // gap + category chip row
            for (F3Element element : F3Element.values()) {
                if (element.category() == category) {
                    y += ROW;
                }
            }
        }
        if (canSeeAdminLock()) {
            y += 10 + 12 + ROW; // admin
        }
        y += 6;
        return y;
    }

    private boolean canSeeAdminLock() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        contentH = measureContentHeight();
        maxScroll = Math.max(0, contentH - Math.max(1, panelBottom - contentTop - 4));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        this.renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xF0F0F0);

        drawPreview(graphics);

        graphics.fill(settingsL, panelTop, settingsR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, settingsL, panelTop, settingsR - settingsL,
                panelBottom - panelTop, ACCENT, 0xFF0A0A0E);

        graphics.enableScissor(settingsL + 2, panelTop + 2, settingsR - 2, panelBottom - 2);

        int x = settingsL + 10;
        int y = contentTop - scroll;
        int right = settingsR - 10;

        if (!canEdit) {
            graphics.drawString(font,
                    Component.translatable("screen." + Features.MODID + ".f3_hide.locked"),
                    x, y, 0xE05050, false);
            y += 18;
        }

        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".f3_hide.hint"),
                x, y, 0x808088, false);
        y += 12;

        y = drawBulkActions(graphics, x, right, y, mouseX, mouseY);

        for (F3Category category : F3Category.values()) {
            y += 6;
            boolean allHidden = allHiddenIn(category);
            y = drawCategoryToggle(graphics, x, right, y, mouseX, mouseY, category, allHidden);

            for (F3Element element : F3Element.values()) {
                if (element.category() != category) {
                    continue;
                }
                boolean hidden = FeaturesConfig.isF3ElementHidden(element);
                y = drawCheckRow(graphics, x, y, mouseX, mouseY, hidden,
                        () -> {
                            if (canEdit) {
                                FeaturesConfig.setF3ElementHidden(element, !hidden);
                            }
                        },
                        element.langKey(), canEdit);
            }
        }

        if (canSeeAdminLock()) {
            y += 10;
            y = drawSection(graphics, x, y, "screen." + Features.MODID + ".f3_hide.admin");
            drawCheckRow(graphics, x, y, mouseX, mouseY, FeaturesConfig.f3HideAdminLocked,
                    () -> {
                        FeaturesConfig.setF3HideAdminLocked(!FeaturesConfig.f3HideAdminLocked);
                        canEdit = F3Hide.canEdit(minecraft != null ? minecraft.player : null);
                    },
                    FeaturesConfig.f3HideAdminLocked
                            ? "screen." + Features.MODID + ".f3_hide.admin.on"
                            : "screen." + Features.MODID + ".f3_hide.admin.off",
                    true);
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackH = panelBottom - panelTop - 8;
            int thumbH = Math.max(12, trackH * trackH / (trackH + maxScroll));
            int thumbY = panelTop + 4 + (trackH - thumbH) * scroll / maxScroll;
            graphics.fill(settingsR - 5, panelTop + 4, settingsR - 3, panelBottom - 4, 0xFF2A2A32);
            graphics.fill(settingsR - 5, thumbY, settingsR - 3, thumbY + thumbH, ACCENT);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPreview(GuiGraphics g) {
        if (previewR - previewL < 40) {
            return;
        }
        g.fill(previewL, panelTop, previewR, panelBottom, PREVIEW_BG);
        FeaturesScreen.drawGradientBorder(g, previewL, panelTop, previewR - previewL,
                panelBottom - panelTop, 0xFF3A3A48, 0xFF0A0A0E);

        g.drawString(font,
                Component.translatable("screen." + Features.MODID + ".f3_hide.preview"),
                previewL + 8, panelTop + 6, 0x909098, false);

        int top = panelTop + 18;
        int bottom = panelBottom - 6;
        int leftX = previewL + 6;
        int rightEdge = previewR - 6;
        int midGap = 10;
        int colW = Math.max(40, (rightEdge - leftX - midGap) / 2);
        int rightX = leftX + colW + midGap;

        g.enableScissor(previewL + 2, top, previewR - 2, bottom);

        int yL = top;
        for (PreviewLine line : leftPreviewLines()) {
            if (!FeaturesConfig.isF3ElementHidden(line.element)) {
                int color = line.dim ? 0xA0A0A8 : 0xE0E0E8;
                String text = truncate(line.text, colW);
                g.drawString(font, text, leftX, yL, color, false);
                yL += PREVIEW_LINE;
                if (yL > bottom) {
                    break;
                }
            }
        }

        int yR = top;
        for (PreviewLine line : rightPreviewLines()) {
            if (!FeaturesConfig.isF3ElementHidden(line.element)) {
                int color = line.dim ? 0xA0A0A8 : 0xE0E0E8;
                String text = truncate(line.text, colW);
                int tw = font.width(text);
                g.drawString(font, text, rightX + colW - tw, yR, color, false);
                yR += PREVIEW_LINE;
                if (yR > bottom) {
                    break;
                }
            }
        }

        if (yL == top && yR == top) {
            g.drawCenteredString(font,
                    Component.translatable("screen." + Features.MODID + ".f3_hide.preview.empty"),
                    (previewL + previewR) / 2, (top + bottom) / 2 - 4, 0x606068);
        }

        g.disableScissor();
    }

    private String truncate(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "..";
        int budget = maxWidth - font.width(ellipsis);
        if (budget <= 0) {
            return ellipsis;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (font.width(sb.toString() + c) > budget) {
                break;
            }
            sb.append(c);
        }
        return sb + ellipsis;
    }

    private static List<PreviewLine> leftPreviewLines() {
        List<PreviewLine> lines = new ArrayList<>();
        lines.add(new PreviewLine(F3Element.VERSION, "Minecraft 1.20.1 (Modded)", false));
        lines.add(new PreviewLine(F3Element.FPS, "120 fps T: 20", false));
        lines.add(new PreviewLine(F3Element.COORDINATES, "XYZ: 128.5 / 72.0 / -256.3", false));
        lines.add(new PreviewLine(F3Element.COORDINATES, "Block: 128 72 -256", true));
        lines.add(new PreviewLine(F3Element.COORDINATES, "Chunk: 8 4 -16", true));
        lines.add(new PreviewLine(F3Element.COORDINATES, "Facing: north (Towards -Z)", true));
        lines.add(new PreviewLine(F3Element.LIGHT, "Client Light: 15 (15 sky, 0 block)", false));
        lines.add(new PreviewLine(F3Element.BIOME, "Biome: minecraft:plains", false));
        lines.add(new PreviewLine(F3Element.DIFFICULTY, "Local Difficulty: 2.25", false));
        lines.add(new PreviewLine(F3Element.DAY, "Day 42", false));
        lines.add(new PreviewLine(F3Element.ENTITIES, "E: 48/120", false));
        lines.add(new PreviewLine(F3Element.PARTICLES, "P: 12", false));
        lines.add(new PreviewLine(F3Element.SOUND, "Sounds: 8/247", false));
        lines.add(new PreviewLine(F3Element.HEIGHTMAP, "CH SH: 72 71", false));
        lines.add(new PreviewLine(F3Element.LOOKING_BLOCK, "Targeted Block: 128, 71, -255", false));
        lines.add(new PreviewLine(F3Element.LOOKING_BLOCK, "minecraft:grass_block", true));
        lines.add(new PreviewLine(F3Element.LOOKING_FLUID, "Targeted Fluid: none", false));
        lines.add(new PreviewLine(F3Element.TARGETED_ENTITY, "Targeted Entity: none", false));
        lines.add(new PreviewLine(F3Element.HELP, "Debug: Pie [shift]: debug charts", true));
        lines.add(new PreviewLine(F3Element.MISC_LEFT, "For help: press F3 + Q", true));
        return lines;
    }

    private static List<PreviewLine> rightPreviewLines() {
        List<PreviewLine> lines = new ArrayList<>();
        lines.add(new PreviewLine(F3Element.JAVA, "Java: 17.0.9 64bit", false));
        lines.add(new PreviewLine(F3Element.MEMORY, "Mem: 42% 1024/2048MB", false));
        lines.add(new PreviewLine(F3Element.MEMORY, "Allocated: 75% 1536MB", true));
        lines.add(new PreviewLine(F3Element.CPU, "CPU: 8x AMD Ryzen", false));
        lines.add(new PreviewLine(F3Element.DISPLAY, "Display: 1920x1080", false));
        lines.add(new PreviewLine(F3Element.GPU, "NVIDIA GeForce / GL 4.6", false));
        lines.add(new PreviewLine(F3Element.BLOCK_DETAILS, "minecraft:grass_block", false));
        lines.add(new PreviewLine(F3Element.BLOCK_DETAILS, "snowy: false", true));
        lines.add(new PreviewLine(F3Element.MISC_RIGHT, "Server: Integrated", true));
        return lines;
    }

    private static boolean allHiddenIn(F3Category category) {
        for (F3Element element : F3Element.values()) {
            if (element.category() == category && !FeaturesConfig.isF3ElementHidden(element)) {
                return false;
            }
        }
        return true;
    }

    private int drawBulkActions(GuiGraphics g, int left, int right, int y, int mx, int my) {
        int gap = 4;
        int w = Math.max(60, ((right - left) - gap) / 2);
        drawActionBtn(g, left, y, w, ACTION_H, mx, my,
                "screen." + Features.MODID + ".f3_hide.show_all",
                () -> {
                    if (canEdit) {
                        FeaturesConfig.setAllF3ElementsHidden(false);
                    }
                });
        drawActionBtn(g, left + w + gap, y, w, ACTION_H, mx, my,
                "screen." + Features.MODID + ".f3_hide.hide_all",
                () -> {
                    if (canEdit) {
                        FeaturesConfig.setAllF3ElementsHidden(true);
                    }
                });
        return y + ACTION_H + 6;
    }

    private void drawActionBtn(GuiGraphics g, int x, int y, int w, int h, int mx, int my,
                               String labelKey, Runnable action) {
        boolean hover = canEdit && insideSettings(mx, my)
                && mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, hover ? 0xFF2A3A48 : 0xFF1A1A22);
        if (hover) {
            g.fill(x, y + h - 2, x + w, y + h, ACCENT);
        }
        int color = canEdit ? (hover ? 0xF0F0F0 : 0xC0C0C8) : 0x606068;
        g.drawCenteredString(font, Component.translatable(labelKey), x + w / 2, y + (h - 8) / 2, color);
        if (canEdit) {
            hits.add(new Hit(x, y, w, h, action));
        } else {
            g.fill(x, y, x + w, y + h, 0x44000000);
        }
    }

    /**
     * Category control: filled chip / toggle pill (not a checkbox).
     * Click toggles hide-all for that category.
     */
    private int drawCategoryToggle(GuiGraphics g, int left, int right, int y, int mx, int my,
                                   F3Category category, boolean allHidden) {
        int h = CATEGORY_H;
        int w = right - left;
        boolean hover = canEdit && insideSettings(mx, my)
                && mx >= left && mx < right && my >= y && my < y + h;

        int fill = allHidden ? 0xFF243040 : (hover ? 0xFF22222C : 0xFF1A1A22);
        g.fill(left, y, right, y + h, fill);
        int border = allHidden ? ACCENT : (hover ? 0xFF505068 : 0xFF2A2A36);
        g.fill(left, y, right, y + 1, border);
        g.fill(left, y + h - 1, right, y + h, border);
        g.fill(left, y, left + 1, y + h, border);
        g.fill(right - 1, y, right, y + h, border);

        // Status pip on the left
        int pip = allHidden ? ACCENT : 0xFF606070;
        g.fill(left + 4, y + 4, left + 8, y + h - 4, pip);

        Component label = Component.translatable(category.langKey());
        int labelColor = canEdit ? (allHidden ? 0xF0F0F0 : 0xC8C8D0) : 0x606068;
        g.drawString(font, label, left + 12, y + (h - 8) / 2, labelColor, false);

        Component state = Component.translatable(allHidden
                ? "screen." + Features.MODID + ".f3_hide.category_hidden"
                : "screen." + Features.MODID + ".f3_hide.category_visible");
        int stateColor = allHidden ? ACCENT : 0x808088;
        int sw = font.width(state);
        g.drawString(font, state, right - 6 - sw, y + (h - 8) / 2, stateColor, false);

        if (canEdit) {
            hits.add(new Hit(left, y, w, h, () -> FeaturesConfig.setF3CategoryHidden(category, !allHidden)));
        } else {
            g.fill(left, y, right, y + h, 0x44000000);
        }
        return y + h + 2;
    }

    private int drawSection(GuiGraphics g, int x, int y, String key) {
        g.drawString(font, Component.translatable(key), x, y, 0xC8C8D0, false);
        return y + 12;
    }

    private int drawCheckRow(GuiGraphics g, int x, int y, int mx, int my, boolean on,
                             Runnable action, String labelKey, boolean interactive) {
        int color = interactive ? (on ? 0xF0F0F0 : 0x9A9AA8) : 0x606068;
        drawCheck(g, x, y + 3, on);
        if (!interactive) {
            g.fill(x, y + 3, x + CHECK, y + 3 + CHECK, 0x66000000);
        }
        g.drawString(font, Component.translatable(labelKey), x + CHECK + 6, y + 3, color, false);
        if (interactive) {
            hits.add(new Hit(x, y, settingsR - 10 - x, ROW - 2, action));
        }
        return y + ROW;
    }

    private static void drawCheck(GuiGraphics g, int x, int y, boolean on) {
        g.fill(x, y, x + CHECK, y + 1, 0xFFE8E8F0);
        g.fill(x, y + CHECK - 1, x + CHECK, y + CHECK, 0xFFE8E8F0);
        g.fill(x, y, x + 1, y + CHECK, 0xFFE8E8F0);
        g.fill(x + CHECK - 1, y, x + CHECK, y + CHECK, 0xFFE8E8F0);
        if (on) {
            g.fill(x + 2, y + 2, x + CHECK - 2, y + CHECK - 2, ACCENT);
        }
    }

    private boolean insideSettings(double mx, double my) {
        return mx >= settingsL && mx < settingsR && my >= panelTop && my < panelBottom;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && insideSettings(mouseX, mouseY)) {
            for (Hit hit : hits) {
                if (mouseX >= hit.x && mouseX < hit.x + hit.w && mouseY >= hit.y && mouseY < hit.y + hit.h) {
                    hit.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll <= 0 || !insideSettings(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        scroll = Mth.clamp(scroll - (int) Math.signum(delta) * 16, 0, maxScroll);
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
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

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }

    private record PreviewLine(F3Element element, String text, boolean dim) {
    }

    private class DarkButton extends AbstractWidget {
        private final Runnable action;

        private DarkButton(int x, int y, int w, int h, Component message, Runnable action) {
            super(x, y, w, h, message);
            this.action = action;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hover = isHoveredOrFocused();
            graphics.fill(getX(), getY(), getX() + width, getY() + height, hover ? 0xFF2A3A48 : 0xFF1A1A22);
            FeaturesScreen.drawGradientBorder(graphics, getX(), getY(), width, height, ACCENT, 0xFF0A0A0E);
            graphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2,
                    hover ? 0xF0F0F0 : 0xC0C0C8);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
