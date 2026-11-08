package yblk.features.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import yblk.features.Features;
import yblk.features.feature.Feature;
import yblk.features.feature.FeatureRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Features menu layout is designed for GUI scale 3 with three cards in a row.
 * Card size follows the current scaled screen width so other GUI scales / window sizes
 * shrink or grow the menu proportionally. Extra rows scroll with a side scrollbar.
 */
public class FeaturesScreen extends Screen {
    private static final ResourceLocation TITLE_TEX =
            new ResourceLocation(Features.MODID, "textures/gui/features_title.png");
    private static final int TITLE_TEX_W = 1024;
    private static final int TITLE_TEX_H = 208;
    /** Reference design: 1920×1080 at GUI scale 3 → 640×360. */
    private static final int REF_GUI_SCALE = 3;
    private static final int REF_WIDTH = 640;
    private static final int COLUMNS = 3;
    private static final int CARD_FILL = 0xFF16161C;
    private static final int SCROLL_TRACK = 0xFF2A2A32;
    private static final int SCROLL_THUMB = 0xFF9A9AA8;

    private final Screen parent;
    private final List<FeatureCard> cards = new ArrayList<>();
    private Layout layout = Layout.fallback();
    /** At most one card is considered hovered — avoids sticky overlap cutoffs. */
    private FeatureCard hoveredCard;
    private int scroll;
    private int maxScroll;
    private int cardsTop;
    private int cardsBottom;
    private int contentH;
    private boolean draggingScroll;
    private int scrollDragOffset;
    /** Base Y of the admin divider (content space, before scroll). -1 if none. */
    private int adminDividerBaseY = -1;
    private int cardHeight;

    public FeaturesScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".features"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        cards.clear();
        adminDividerBaseY = -1;
        layout = Layout.compute(width, height, minecraft == null ? REF_GUI_SCALE : minecraft.getWindow().getGuiScale());

        List<Feature> playerFeatures = FeatureRegistry.playerFacing();
        List<Feature> adminFeatures = FeatureRegistry.operatorOnly();
        List<Feature> features = new ArrayList<>(playerFeatures.size() + adminFeatures.size());
        features.addAll(playerFeatures);
        features.addAll(adminFeatures);

        List<List<FormattedCharSequence>> descriptions = new ArrayList<>();
        cardHeight = 0;
        boolean anySettings = false;
        int descWidth = Math.max(20, layout.cardW - layout.pad * 2);
        for (Feature feature : features) {
            feature.getAccentColor();
            List<FormattedCharSequence> lines = font.split(feature.getDescription(), descWidth);
            descriptions.add(lines);
            int height = layout.headerH + lines.size() * layout.lineH + layout.pad;
            cardHeight = Math.max(cardHeight, height);
            if (feature.hasSettings()) {
                anySettings = true;
            }
        }
        if (anySettings) {
            cardHeight += layout.settingsH + layout.pad;
        }
        cardHeight = Math.max(cardHeight, layout.minCardH);

        cardsTop = layout.cardsY;
        cardsBottom = height - layout.bottomReserve;

        int count = Math.min(COLUMNS, Math.max(1, features.size()));
        int rowW = count * layout.cardW + Math.max(0, count - 1) * layout.gap;
        int x = (width - rowW) / 2;
        int y = cardsTop;
        int lastBottom = cardsTop;
        int descIndex = 0;

        lastBottom = placeCards(playerFeatures, descriptions, descIndex, x, y, rowW, cardHeight);
        descIndex += playerFeatures.size();
        y = lastBottom;

        if (!adminFeatures.isEmpty()) {
            int dividerH = Math.max(18, Math.round(22 * layout.ui));
            adminDividerBaseY = y + Math.max(4, layout.gap / 2);
            y = adminDividerBaseY + dividerH;
            lastBottom = placeCards(adminFeatures, descriptions, descIndex, (width - rowW) / 2, y, rowW, cardHeight);
        }

        contentH = Math.max(0, lastBottom - cardsTop);
        int viewH = Math.max(1, cardsBottom - cardsTop);
        maxScroll = Math.max(0, contentH - viewH);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        syncCards();

        int btnW = layout.btnW;
        int btnH = layout.btnH;
        int btnY = height - layout.bottomPad - btnH;
        addRenderableWidget(new DarkButton(width / 2 - btnW / 2, btnY, btnW, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    private int placeCards(List<Feature> features, List<List<FormattedCharSequence>> descriptions,
                           int descOffset, int startX, int startY, int rowW, int cardH) {
        int x = startX;
        int y = startY;
        int lastBottom = startY;
        for (int i = 0; i < features.size(); i++) {
            if (i > 0 && i % COLUMNS == 0) {
                x = (width - rowW) / 2;
                y += cardH + layout.gap;
            }
            FeatureCard card = new FeatureCard(x, y, layout.cardW, cardH,
                    features.get(i), descriptions.get(descOffset + i), layout);
            cards.add(card);
            addRenderableWidget(card);
            lastBottom = y + cardH;
            x += layout.cardW + layout.gap;
        }
        return lastBottom;
    }

    private void syncCards() {
        for (FeatureCard card : cards) {
            card.setY(card.baseY - scroll);
            boolean inView = card.getY() + card.getHeight() > cardsTop && card.getY() < cardsBottom;
            card.visible = inView || maxScroll > 0;
            card.active = inView;
        }
    }

    private int scrollTrackX() {
        return width - Math.max(6, Math.round(8 * layout.ui));
    }

    private int scrollTrackW() {
        return Math.max(3, Math.round(4 * layout.ui));
    }

    private int thumbH() {
        int trackH = Math.max(1, cardsBottom - cardsTop);
        if (contentH <= 0) {
            return trackH;
        }
        return Mth.clamp(trackH * trackH / (trackH + maxScroll), 12, trackH);
    }

    private int thumbY() {
        int trackH = Math.max(1, cardsBottom - cardsTop);
        int th = thumbH();
        if (maxScroll <= 0) {
            return cardsTop;
        }
        return cardsTop + (trackH - th) * scroll / maxScroll;
    }

    private boolean overScrollBar(double mouseX, double mouseY) {
        if (maxScroll <= 0) {
            return false;
        }
        int tx = scrollTrackX();
        int tw = scrollTrackW();
        return mouseX >= tx - 2 && mouseX < tx + tw + 2
                && mouseY >= cardsTop && mouseY < cardsBottom;
    }

    private void scrollToThumb(double mouseY) {
        int trackH = Math.max(1, cardsBottom - cardsTop);
        int th = thumbH();
        int travel = Math.max(1, trackH - th);
        float t = (float) ((mouseY - cardsTop - scrollDragOffset) / travel);
        scroll = Mth.clamp(Math.round(t * maxScroll), 0, maxScroll);
        syncCards();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        maxScroll = Math.max(0, contentH - Math.max(1, cardsBottom - cardsTop));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        syncCards();
        hoveredCard = pickHoveredCard(mouseX, mouseY);

        graphics.fill(0, 0, width, height, 0xF008080C);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int titleX = (width - layout.titleW) / 2;
        graphics.blit(TITLE_TEX, titleX, layout.titleY, layout.titleW, layout.titleH,
                0f, 0f, TITLE_TEX_W, TITLE_TEX_H, TITLE_TEX_W, TITLE_TEX_H);

        // Leave headroom so hover scale/sway on the top row isn't clipped.
        int scissorTop = Math.max(0, cardsTop - layout.hoverPad);
        graphics.enableScissor(0, scissorTop, width, cardsBottom);
        if (adminDividerBaseY >= 0) {
            int divY = adminDividerBaseY - scroll;
            if (divY + 16 > cardsTop && divY < cardsBottom) {
                int lineY = divY + 7;
                int margin = layout.margin;
                graphics.fill(margin, lineY, width - margin, lineY + 1, 0xFF4A4A58);
                Component label = Component.translatable("screen." + Features.MODID + ".admin_only");
                int lw = font.width(label);
                int lx = (width - lw) / 2;
                graphics.fill(lx - 6, lineY - 5, lx + lw + 6, lineY + 7, 0xF008080C);
                graphics.drawString(font, label, lx, lineY - 3, 0xC96060, false);
            }
        }
        for (FeatureCard card : cards) {
            if (card.visible) {
                card.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int tx = scrollTrackX();
            int tw = scrollTrackW();
            graphics.fill(tx, cardsTop, tx + tw, cardsBottom, SCROLL_TRACK);
            int ty = thumbY();
            int th = thumbH();
            boolean hover = overScrollBar(mouseX, mouseY);
            graphics.fill(tx, ty, tx + tw, ty + th, hover || draggingScroll ? 0xFFC0C0C8 : SCROLL_THUMB);
        }

        // Done button (and anything else that isn't a feature card)
        for (var child : children()) {
            if (!(child instanceof FeatureCard) && child instanceof AbstractWidget widget) {
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private FeatureCard pickHoveredCard(int mouseX, int mouseY) {
        if (mouseY < cardsTop || mouseY >= cardsBottom) {
            return null;
        }
        FeatureCard best = null;
        double bestDist = Double.MAX_VALUE;
        for (FeatureCard card : cards) {
            if (!card.active || !card.containsBase(mouseX, mouseY)) {
                continue;
            }
            double dx = mouseX - card.cardCx();
            double dy = mouseY - card.cardCy();
            double dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                best = card;
            }
        }
        return best;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0 && mouseY >= cardsTop && mouseY < cardsBottom) {
            scroll = Mth.clamp(scroll - (int) Math.signum(delta) * 28, 0, maxScroll);
            syncCards();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overScrollBar(mouseX, mouseY)) {
            int ty = thumbY();
            int th = thumbH();
            if (mouseY >= ty && mouseY < ty + th) {
                draggingScroll = true;
                scrollDragOffset = (int) mouseY - ty;
            } else {
                scrollDragOffset = th / 2;
                draggingScroll = true;
                scrollToThumb(mouseY);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScroll && button == 0 && maxScroll > 0) {
            scrollToThumb(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScroll = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private static final class Layout {
        final float ui;
        final int margin;
        final int gap;
        final int cardW;
        final int pad;
        final int icon;
        final int titleW;
        final int titleH;
        final int titleY;
        final int cardsY;
        final int headerH;
        final int lineH;
        final int toggleW;
        final int toggleH;
        final int settingsH;
        final int minCardH;
        final int hoverPad;
        final int bottomPad;
        final int bottomReserve;
        final int btnW;
        final int btnH;

        private Layout(float ui, int margin, int gap, int cardW, int pad, int icon,
                       int titleW, int titleH, int titleY, int cardsY, int headerH, int lineH,
                       int toggleW, int toggleH, int settingsH, int minCardH, int hoverPad,
                       int bottomPad, int bottomReserve, int btnW, int btnH) {
            this.ui = ui;
            this.margin = margin;
            this.gap = gap;
            this.cardW = cardW;
            this.pad = pad;
            this.icon = icon;
            this.titleW = titleW;
            this.titleH = titleH;
            this.titleY = titleY;
            this.cardsY = cardsY;
            this.headerH = headerH;
            this.lineH = lineH;
            this.toggleW = toggleW;
            this.toggleH = toggleH;
            this.settingsH = settingsH;
            this.minCardH = minCardH;
            this.hoverPad = hoverPad;
            this.bottomPad = bottomPad;
            this.bottomReserve = bottomReserve;
            this.btnW = btnW;
            this.btnH = btnH;
        }

        static Layout fallback() {
            return compute(REF_WIDTH, 360, REF_GUI_SCALE);
        }

        /**
         * Designed around GUI scale 3: three equal cards across the scaled width.
         * Other scales / window sizes change {@code screenW}, so the menu shrinks or grows with it.
         */
        static Layout compute(int screenW, int screenH, double guiScale) {
            float scaleBias = Mth.clamp((float) (REF_GUI_SCALE / Math.max(1.0, guiScale)), 0.75f, 1.25f);
            int margin = Math.max(8, Math.round(screenW * 0.035f * scaleBias));
            int gap = Math.max(4, Math.round(screenW * 0.015f * scaleBias));
            int usable = Math.max(120, screenW - margin * 2);
            int cardW = Math.max(90, (usable - gap * (COLUMNS - 1)) / COLUMNS);
            float ui = Mth.clamp(cardW / 230f, 0.48f, 1.05f) * 0.82f;

            int pad = Math.max(5, Math.round(8 * ui));
            int icon = Math.max(14, Math.round(26 * ui));
            int titleW = Math.max(120, Math.min(usable - 8, Math.round(260 * ui)));
            int titleH = Math.max(24, Math.round(titleW * (float) TITLE_TEX_H / TITLE_TEX_W));
            int titleY = Math.max(4, Math.round(6 * ui));
            int hoverPad = Math.max(10, Math.round(14 * ui));
            int cardsY = titleY + titleH + Math.max(hoverPad, Math.round(10 * ui));
            int headerH = pad + icon + Math.max(3, Math.round(6 * ui));
            int lineH = Math.max(8, Math.round(10 * ui));
            int toggleW = Mth.clamp(Math.round(34 * ui), 22, Math.min(36, cardW / 4)) & ~1;
            int toggleH = Math.max(12, Math.round(14 * ui)) & ~1;
            int settingsH = Math.max(12, Math.round(15 * ui));
            int minCardH = headerH + lineH * 2 + pad;
            int bottomPad = Math.max(5, Math.round(6 * ui));
            int btnH = Math.max(14, Math.round(17 * ui));
            int btnW = Math.max(64, Math.round(88 * ui));
            int bottomReserve = btnH + bottomPad * 2 + Math.max(3, Math.round(4 * ui));
            return new Layout(ui, margin, gap, cardW, pad, icon, titleW, titleH, titleY, cardsY,
                    headerH, lineH, toggleW, toggleH, settingsH, minCardH, hoverPad,
                    bottomPad, bottomReserve, btnW, btnH);
        }
    }

    private class FeatureCard extends AbstractWidget {
        private static final float HOVER_SCALE = 0.08f;
        private static final float HOVER_SPEED_IN = 3.2f;
        private static final float HOVER_SPEED_OUT = 2.1f;
        /** Softer toggle travel — reads as interpolation, not a snap. */
        private static final float TOGGLE_SPEED = 5.2f;
        private static final float SWAY_DEG = 0.7f;

        private final Feature feature;
        private final List<FormattedCharSequence> descriptionLines;
        private final Layout L;
        private final int baseY;
        private final float swayPhase;
        private float hoverAnim;
        private float toggleAnim;

        private FeatureCard(int x, int y, int w, int h, Feature feature,
                            List<FormattedCharSequence> descriptionLines, Layout layout) {
            super(x, y, w, h, feature.getTitle());
            this.feature = feature;
            this.descriptionLines = descriptionLines;
            this.L = layout;
            this.baseY = y;
            this.swayPhase = (feature.getId().hashCode() * 0.6180339f) % ((float) Math.PI * 2f);
            this.toggleAnim = feature.isEnabled() ? 1f : 0f;
        }

        private float toggleEase() {
            float t = Mth.clamp(toggleAnim, 0f, 1f);
            return t * t * (3f - 2f * t);
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(false);
        }

        private int toggleX() {
            return getX() + width - L.pad - L.toggleW;
        }

        private int toggleY() {
            return getY() + L.pad + Math.max(0, (L.icon - L.toggleH) / 2);
        }

        private int settingsX() {
            return getX() + L.pad;
        }

        private int settingsY() {
            return getY() + height - L.settingsH - L.pad;
        }

        private int settingsW() {
            return width - L.pad * 2;
        }

        private float hoverEase() {
            float t = Mth.clamp(hoverAnim, 0f, 1f);
            return t * t * t * (t * (t * 6f - 15f) + 10f);
        }

        private float currentScale() {
            return 1f + hoverEase() * HOVER_SCALE;
        }

        private float cardCx() {
            return getX() + width * 0.5f;
        }

        private float cardCy() {
            return getY() + height * 0.5f;
        }

        private boolean containsBase(double mouseX, double mouseY) {
            return mouseX >= getX() && mouseX < getX() + width
                    && mouseY >= getY() && mouseY < getY() + height;
        }

        private double localX(double mouseX) {
            float s = currentScale();
            float cx = cardCx();
            return cx + (mouseX - cx) / s;
        }

        private double localY(double mouseY) {
            float s = currentScale();
            float cy = cardCy();
            return cy + (mouseY - cy) / s;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            if (mouseY < FeaturesScreen.this.cardsTop || mouseY >= FeaturesScreen.this.cardsBottom) {
                return false;
            }
            return FeaturesScreen.this.hoveredCard == this || containsBase(mouseX, mouseY);
        }

        @Override
        protected boolean clicked(double mouseX, double mouseY) {
            if (mouseY < FeaturesScreen.this.cardsTop || mouseY >= FeaturesScreen.this.cardsBottom) {
                return false;
            }
            return this.active && this.visible && containsBase(mouseX, mouseY);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean wantHover = FeaturesScreen.this.hoveredCard == this;
            float target = wantHover ? 1f : 0f;
            float dt = minecraft != null ? Math.max(0.001f, minecraft.getDeltaFrameTime()) : 0.05f;
            float speed = target > hoverAnim ? HOVER_SPEED_IN : HOVER_SPEED_OUT;
            float k = 1f - (float) Math.exp(-speed * dt);
            hoverAnim = Mth.lerp(k, hoverAnim, target);

            float toggleTarget = feature.isEnabled() ? 1f : 0f;
            float tk = 1f - (float) Math.exp(-TOGGLE_SPEED * dt);
            toggleAnim = Mth.lerp(tk, toggleAnim, toggleTarget);

            float ease = hoverEase();
            float scale = 1f + ease * HOVER_SCALE;
            float cx = cardCx();
            float cy = cardCy();
            // Chaotic multi-frequency sway — unique phase per feature, not a metronome rock.
            float t = Util.getMillis() * 0.001f;
            float p = swayPhase;
            float sway = (
                    Mth.sin(t * 1.6f + p) * 0.38f
                            + Mth.sin(t * 2.7f + p * 1.7f) * 0.28f
                            + Mth.sin(t * 4.3f + p * 0.6f) * 0.18f
                            + Mth.sin(t * 0.9f + p * 2.3f) * 0.16f
            ) * SWAY_DEG * ease;
            float bobX = (
                    Mth.sin(t * 2.05f + p * 1.4f) * 0.55f
                            + Mth.sin(t * 3.4f + p) * 0.3f
            ) * ease;
            float bobY = (
                    Mth.sin(t * 1.35f + p * 0.8f) * 0.4f
                            + Mth.sin(t * 2.9f + p * 1.9f) * 0.25f
            ) * ease;

            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(cx + bobX, cy + bobY, ease * 8f);
            pose.mulPose(Axis.ZP.rotationDegrees(sway));
            pose.scale(scale, scale, 1f);
            pose.translate(-cx, -cy, 0f);

            int baseAccent = feature.getAccentColor();
            float pulse = 0.5f + 0.5f * Mth.sin(Util.getMillis() * 0.0028f);
            float glow = ease * (0.16f + 0.18f * pulse);
            int topAccent = brighten(baseAccent, glow);
            int bottomAccent = lerpColor(0xFF0A0A0E, brighten(baseAccent, glow * 0.3f), ease * (0.4f + 0.2f * pulse));
            graphics.fill(getX(), getY(), getX() + width, getY() + height, CARD_FILL);
            drawGradientBorder(graphics, getX(), getY(), width, height, topAccent, bottomAccent,
                    ease * (0.2f + 0.35f * pulse));

            ResourceLocation icon = feature.getIcon();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            graphics.blit(icon, getX() + L.pad, getY() + L.pad, L.icon, L.icon, 0f, 0f, 32, 32, 32, 32);

            int titleX = getX() + L.pad + L.icon + Math.max(4, L.pad / 2);
            int titleY = getY() + L.pad + Math.max(0, (L.icon - 8) / 2);
            int titleMaxW = Math.max(8, toggleX() - titleX - 4);
            graphics.drawString(font, truncateToWidth(feature.getTitle().getString(), titleMaxW), titleX, titleY, 0xF0F0F0, false);

            int dy = getY() + L.headerH;
            int maxY = feature.hasSettings() ? settingsY() - 2 : getY() + height - L.pad;
            for (FormattedCharSequence line : descriptionLines) {
                if (dy + L.lineH > maxY) {
                    break;
                }
                graphics.drawString(font, line, getX() + L.pad, dy, 0x9A9AA8, false);
                dy += L.lineH;
            }

            boolean editable = feature.canPlayerConfigure();
            drawToggle(graphics, toggleX(), toggleY(), editable);

            double lmx = localX(mouseX);
            double lmy = localY(mouseY);

            if (feature.hasSettings()) {
                int sx = settingsX();
                int sy = settingsY();
                int sw = settingsW();
                boolean hover = editable && lmx >= sx && lmx < sx + sw && lmy >= sy && lmy < sy + L.settingsH;
                graphics.fill(sx, sy, sx + sw, sy + L.settingsH, hover ? 0xFF2A3A38 : 0xFF1E1E26);
                graphics.drawCenteredString(font,
                        Component.translatable("screen." + Features.MODID + ".feature_settings"),
                        sx + sw / 2, sy + Math.max(2, (L.settingsH - 8) / 2),
                        editable ? (hover ? 0xF0F0F0 : 0xB0B0BC) : 0x606068);
            }

            pose.popPose();
        }

        private void drawToggle(GuiGraphics graphics, int tx, int ty, boolean editable) {
            int tw = L.toggleW;
            int th = L.toggleH;
            int pad = 2;
            int knob = th - pad * 2;
            int travel = tw - pad * 2 - knob;
            float te = toggleEase();
            int knobX = tx + pad + Math.round(travel * te);
            int knobY = ty + pad;

            boolean on = te > 0.5f;
            int track = editable ? (on ? 0xFF2F6F4E : 0xFF3A3A44) : 0xFF2A2A30;
            graphics.fill(tx, ty, tx + tw, ty + th, track);
            graphics.fill(tx + 1, ty + 1, tx + tw - 1, ty + 2, 0x22000000);
            graphics.fill(tx + 1, ty + th - 2, tx + tw - 1, ty + th - 1, 0x33FFFFFF);
            graphics.fill(knobX, knobY, knobX + knob, knobY + knob, editable ? 0xFFE8E8F0 : 0xFF808088);
            graphics.fill(knobX + 1, knobY + 1, knobX + knob - 1, knobY + 2, 0x33FFFFFF);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            setFocused(false);
            if (mouseY < FeaturesScreen.this.cardsTop || mouseY >= FeaturesScreen.this.cardsBottom) {
                return;
            }
            double lmx = localX(mouseX);
            double lmy = localY(mouseY);

            if (feature.hasSettings()) {
                int sx = settingsX();
                int sy = settingsY();
                int sw = settingsW();
                if (lmx >= sx && lmx < sx + sw && lmy >= sy && lmy < sy + L.settingsH) {
                    if (!feature.canPlayerConfigure()) {
                        return;
                    }
                    if (minecraft != null) {
                        minecraft.setScreen(FeatureSettingsScreens.open(feature, FeaturesScreen.this));
                    }
                    return;
                }
            }

            int tx = toggleX();
            int ty = toggleY();
            if (lmx >= tx && lmx < tx + L.toggleW && lmy >= ty && lmy < ty + L.toggleH) {
                if (!feature.canPlayerConfigure()) {
                    return;
                }
                boolean next = !feature.isEnabled();
                feature.setEnabled(next);
                feature.onToggle(next);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private class DarkButton extends AbstractWidget {
        private static final int ACCENT = 0xFF9A9AA8;
        private final Runnable action;

        private DarkButton(int x, int y, int w, int h, Component message, Runnable action) {
            super(x, y, w, h, message);
            this.action = action;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hover = isHoveredOrFocused();
            graphics.fill(getX(), getY(), getX() + width, getY() + height, hover ? 0xFF2A2A32 : 0xFF1A1A22);
            drawGradientBorder(graphics, getX(), getY(), width, height, ACCENT, 0xFF0A0A0E);
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

    static void drawGradientBorder(GuiGraphics graphics, int x, int y, int w, int h, int topRgb, int bottomRgb) {
        drawGradientBorder(graphics, x, y, w, h, topRgb, bottomRgb, 0f);
    }

    /**
     * @param shimmer 0..1 — traveling highlight strength along the border (hover animation).
     */
    static void drawGradientBorder(GuiGraphics graphics, int x, int y, int w, int h,
                                   int topRgb, int bottomRgb, float shimmer) {
        shimmer = Mth.clamp(shimmer, 0f, 1f);
        float travel = shimmer > 0.001f
                ? (Util.getMillis() % 1600L) / 1600f
                : 0f;
        for (int i = 0; i < h; i++) {
            float t = h <= 1 ? 0f : i / (float) (h - 1);
            int color = lerpColor(topRgb, bottomRgb, t);
            if (shimmer > 0.001f) {
                float band = 1f - Mth.clamp(Math.abs(t - travel) * 3.2f, 0f, 1f);
                band = band * band * shimmer;
                color = lerpColor(color, brighten(color, 0.55f), band);
            }
            color |= 0xFF000000;
            graphics.fill(x, y + i, x + 1, y + i + 1, color);
            graphics.fill(x + w - 1, y + i, x + w, y + i + 1, color);
        }
        int top = topRgb | 0xFF000000;
        int bot = bottomRgb | 0xFF000000;
        if (shimmer > 0.001f) {
            float edge = 0.35f + 0.65f * (0.5f + 0.5f * Mth.sin(Util.getMillis() * 0.005f));
            top = lerpColor(topRgb, brighten(topRgb, 0.5f), shimmer * edge) | 0xFF000000;
            bot = lerpColor(bottomRgb, brighten(topRgb, 0.15f), shimmer * 0.45f) | 0xFF000000;
        }
        graphics.fill(x, y, x + w, y + 1, top);
        graphics.fill(x, y + h - 1, x + w, y + h, bot);
    }

    private String truncateToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisW = font.width(ellipsis);
        if (maxWidth <= ellipsisW) {
            return ellipsis;
        }
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end)) + ellipsisW > maxWidth) {
            end--;
        }
        return text.substring(0, Math.max(0, end)) + ellipsis;
    }

    private static int lerpColor(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = (int) Mth.lerp(t, ar, br);
        int g = (int) Mth.lerp(t, ag, bg);
        int bl = (int) Mth.lerp(t, ab, bb);
        return (r << 16) | (g << 8) | bl;
    }

    private static int brighten(int rgb, float amount) {
        amount = Mth.clamp(amount, 0f, 1f);
        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) + (255 - ((rgb >> 16) & 0xFF)) * amount));
        int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) + (255 - ((rgb >> 8) & 0xFF)) * amount));
        int b = Math.min(255, (int) ((rgb & 0xFF) + (255 - (rgb & 0xFF)) * amount));
        return (r << 16) | (g << 8) | b;
    }
}
