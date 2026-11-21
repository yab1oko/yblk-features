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
import org.lwjgl.glfw.GLFW;
import yblk.features.Features;
import yblk.features.client.ui.DarkButton;
import yblk.features.client.ui.TextField;
import yblk.features.client.ui.ToggleSwitch;
import yblk.features.client.ui.Tooltips;
import yblk.features.feature.Feature;
import yblk.features.feature.FeatureCategory;
import yblk.features.feature.FeatureRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Features menu layout is designed for GUI scale 3 with three cards in a row.
 * Card size follows the current scaled screen width so other GUI scales / window sizes
 * shrink or grow the menu proportionally. Extra rows scroll with a side scrollbar.
 */
public class FeaturesScreen extends Screen {
    private static final ResourceLocation TITLE_TEX =
            new ResourceLocation(Features.MODID, "textures/gui/features_title.png");
    private static final ResourceLocation STAR_OFF =
            new ResourceLocation(Features.MODID, "textures/gui/star_off.png");
    private static final ResourceLocation STAR_ON =
            new ResourceLocation(Features.MODID, "textures/gui/star_on.png");
    private static final ResourceLocation GEAR_TEX =
            new ResourceLocation(Features.MODID, "textures/gui/settings_gear.png");
    private static final int STAR_OFF_W = 11;
    private static final int STAR_ON_W = 11;
    private static final int STAR_H = 10;
    private static final int GEAR_W = 10;
    private static final int GEAR_H = 10;
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
    private int cardHeight;
    private TextField searchBox;
    private String query = "";
    /** Null means every category. */
    private FeatureCategory chip;
    private boolean favoritesChip;
    private final List<SectionMark> sections = new ArrayList<>();
    private final List<SettingCatalog.Hit> searchHits = new ArrayList<>();
    private int keyboardIndex = -1;
    private float slide = 1f;
    private boolean searchOpen;
    /** 0 open (triangle down) .. 1 collapsed (triangle right). Survives a relayout. */
    private final Map<String, Float> chevronAnim = new HashMap<>();
    private final Map<String, Long> starPop = new HashMap<>();
    private final List<Spark> sparks = new ArrayList<>();
    private float animDt = 0.016f;
    private long lastAnimMillis;

    public FeaturesScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".features"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        MenuPrefs.load();
        layout = Layout.compute(width, height, minecraft == null ? REF_GUI_SCALE : minecraft.getWindow().getGuiScale());
        cardsTop = layout.cardsY;
        cardsBottom = height - layout.bottomReserve;

        int tool = layout.barH;
        String filterLabel = switch (MenuPrefs.enabledFilter()) {
            case ON -> "On";
            case OFF -> "Off";
            case ANY -> "*";
        };
        String sortLabel = MenuPrefs.sort() == MenuPrefs.Sort.AZ ? "Az" : "#";
        int filterW = Math.max(tool, font.width(filterLabel) + 10);
        int sortW = Math.max(tool, font.width(sortLabel) + 10);
        int presetW = Math.max(tool, font.width("P") + 10);
        int toolGap = 4;
        int cluster = filterW + sortW + presetW + toolGap * 2;
        int toolX = width - layout.margin - cluster;
        int chipCount = FeatureCategory.values().length + 2;
        int chipGaps = 3 * (chipCount - 1);
        int minChips = 18 * chipCount + chipGaps;
        int searchCap = toolX - 8 - layout.margin - minChips;
        int searchW = Math.min(layout.cardW, Math.round(width * 0.28f));
        searchW = Math.max(48, Math.min(searchW, Math.max(48, searchCap)));
        searchBox = new TextField(font, layout.margin, layout.barY, searchW, layout.barH,
                Component.translatable("screen." + Features.MODID + ".search"));
        searchBox.setMaxLength(48);
        searchBox.setValue(query);
        searchBox.setHint(Component.translatable("screen." + Features.MODID + ".search"));
        searchBox.setResponder(value -> {
            query = value == null ? "" : value;
            searchHits.clear();
            searchHits.addAll(SettingCatalog.search(query));
            keyboardIndex = -1;
            slide = 0f;
            layoutContent();
        });
        addRenderableWidget(searchBox);

        addRenderableWidget(new DarkButton(font, toolX, layout.barY, filterW, tool,
                Component.literal(filterLabel), () -> {
                    MenuPrefs.cycleFilter();
                    slide = 0f;
                    init();
                }, 0xFF9A9AA8, 0xFF2A2A32));
        addRenderableWidget(new DarkButton(font, toolX + filterW + toolGap, layout.barY, sortW, tool,
                Component.literal(sortLabel), () -> {
                    MenuPrefs.toggleSort();
                    slide = 0f;
                    init();
                }, 0xFF9A9AA8, 0xFF2A2A32));
        addRenderableWidget(new DarkButton(font, toolX + filterW + sortW + toolGap * 2, layout.barY, presetW, tool,
                Component.literal("P"), () -> {
                    if (minecraft != null) {
                        minecraft.setScreen(new PresetScreen(this));
                    }
                }, 0xFF9A9AA8, 0xFF2A2A32));

        int chipsLeft = layout.margin + searchW + 4;
        int chipsRight = toolX - 6;
        FeatureCategory[] categories = FeatureCategory.values();
        int chipSpace = Math.max(chipCount, chipsRight - chipsLeft - chipGaps);
        int chipW = Math.max(1, chipSpace / chipCount);
        int cx = chipsLeft;
        addRenderableWidget(new MenuChip(cx, layout.barY, chipW, layout.barH,
                Component.translatable("screen." + Features.MODID + ".category.all"),
                () -> chip == null && !favoritesChip, () -> selectChip(null, false)));
        cx += chipW + 3;
        addRenderableWidget(new MenuChip(cx, layout.barY, chipW, layout.barH,
                Component.translatable("screen." + Features.MODID + ".category.favorites"),
                () -> favoritesChip, () -> selectChip(null, true)));
        cx += chipW + 3;
        for (FeatureCategory category : categories) {
            addRenderableWidget(new MenuChip(cx, layout.barY, chipW, layout.barH,
                    Component.translatable(category.langKey()),
                    () -> chip == category, () -> selectChip(category, false)));
            cx += chipW + 3;
        }

        int btnW = layout.btnW;
        int btnH = layout.btnH;
        int btnY = height - layout.bottomPad - btnH;
        addRenderableWidget(new DarkButton(font, width / 2 - btnW / 2, btnY, btnW, btnH,
                Component.translatable("gui.done"), this::onClose, 0xFF9A9AA8, 0xFF2A2A32));
        if (!query.isBlank()) {
            searchHits.addAll(SettingCatalog.search(query));
        }
        layoutContent();
    }

    private void selectChip(FeatureCategory category, boolean favorites) {
        chip = category;
        favoritesChip = favorites;
        scroll = 0;
        slide = 0f;
        keyboardIndex = -1;
        layoutContent();
    }

    private String filterKey() {
        return switch (MenuPrefs.enabledFilter()) {
            case ON -> "screen." + Features.MODID + ".filter.on";
            case OFF -> "screen." + Features.MODID + ".filter.off";
            case ANY -> "screen." + Features.MODID + ".filter.any";
        };
    }

    private String sortKey() {
        return MenuPrefs.sort() == MenuPrefs.Sort.AZ
                ? "screen." + Features.MODID + ".sort.az"
                : "screen." + Features.MODID + ".sort.category";
    }

    private void layoutContent() {
        for (FeatureCard card : cards) {
            removeWidget(card);
        }
        cards.clear();
        sections.clear();
        measureCardHeight(visibleFeatures());

        int rowW = COLUMNS * layout.cardW + (COLUMNS - 1) * layout.gap;
        int x0 = (width - rowW) / 2;
        int y = cardsTop;
        List<Feature> visible = visibleFeatures();

        boolean grouped = !favoritesChip && chip == null && MenuPrefs.sort() == MenuPrefs.Sort.CATEGORY;
        if (grouped) {
            List<Feature> favorites = new ArrayList<>();
            for (Feature feature : visible) {
                if (MenuPrefs.isFavorite(feature)) {
                    favorites.add(feature);
                }
            }
            if (!favorites.isEmpty()) {
                y = placeGroup("favorites", Component.translatable("screen." + Features.MODID + ".category.favorites"),
                        false, favorites, x0, y, rowW);
            }
            for (FeatureCategory category : FeatureCategory.values()) {
                List<Feature> group = new ArrayList<>();
                for (Feature feature : visible) {
                    if (feature.category() == category) {
                        group.add(feature);
                    }
                }
                if (group.isEmpty()) {
                    continue;
                }
                y = placeGroup(MenuPrefs.collapseKey(category), Component.translatable(category.langKey()),
                        category.adminAccent(), group, x0, y, rowW);
            }
        } else {
            List<Feature> flat = new ArrayList<>(visible);
            if (MenuPrefs.sort() == MenuPrefs.Sort.AZ) {
                flat.sort(Comparator.comparing(feature -> feature.getTitle().getString(), String.CASE_INSENSITIVE_ORDER));
            }
            if (!flat.isEmpty()) {
                y = placeCards(flat, x0, y, rowW, "") + layout.gap;
            }
        }

        contentH = Math.max(0, y - cardsTop);
        maxScroll = Math.max(0, contentH - Math.max(1, cardsBottom - cardsTop));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        syncCards();
    }

    private int placeGroup(String key, Component title, boolean admin, List<Feature> features,
                           int x0, int y, int rowW) {
        int header = Math.max(14, Math.round(16 * layout.ui));
        sections.add(new SectionMark(key, title, admin, features.size(), y, header));
        y += header;
        if (!MenuPrefs.isCollapsed(key)) {
            y = placeCards(features, x0, y, rowW, key) + layout.gap;
        }
        return y;
    }

    private List<Feature> visibleFeatures() {
        List<Feature> out = new ArrayList<>();
        for (Feature feature : FeatureRegistry.all()) {
            if (favoritesChip && !MenuPrefs.isFavorite(feature)) {
                continue;
            }
            if (chip != null && feature.category() != chip) {
                continue;
            }
            boolean enabled = feature.isEnabled();
            if (MenuPrefs.enabledFilter() == MenuPrefs.EnabledFilter.ON && !enabled) {
                continue;
            }
            if (MenuPrefs.enabledFilter() == MenuPrefs.EnabledFilter.OFF && enabled) {
                continue;
            }
            if (!query.isBlank()) {
                String hay = feature.getTitle().getString() + " " + feature.getDescription().getString()
                        + " " + SettingCatalog.haystack(feature.getId());
                if (!MenuSearch.matches(query, hay)) {
                    continue;
                }
            }
            out.add(feature);
        }
        return out;
    }

    private void measureCardHeight(List<Feature> features) {
        cardHeight = 0;
        boolean anySettings = false;
        int descWidth = Math.max(20, layout.cardW - layout.pad * 2);
        for (Feature feature : features) {
            feature.getAccentColor();
            int lines = font.split(feature.getDescription(), descWidth).size();
            int height = layout.headerH + lines * layout.lineH + layout.pad + 10;
            cardHeight = Math.max(cardHeight, height);
            if (feature.hasSettings()) {
                anySettings = true;
            }
        }
        if (anySettings) {
            cardHeight += layout.settingsH + layout.pad;
        }
        cardHeight = Math.max(cardHeight, layout.minCardH);
    }

    private int placeCards(List<Feature> features, int startX, int startY, int rowW, String section) {
        int x = startX;
        int y = startY;
        int lastBottom = startY;
        int descWidth = Math.max(20, layout.cardW - layout.pad * 2);
        for (int i = 0; i < features.size(); i++) {
            if (i > 0 && i % COLUMNS == 0) {
                x = startX;
                y += cardHeight + layout.gap;
            }
            Feature feature = features.get(i);
            FeatureCard card = new FeatureCard(x, y, layout.cardW, cardHeight, feature,
                    font.split(feature.getDescription(), descWidth), layout);
            card.bindSection(section);
            cards.add(card);
            addRenderableWidget(card);
            lastBottom = y + cardHeight;
            x += layout.cardW + layout.gap;
        }
        return lastBottom;
    }

    private void syncCards() {
        int slidePx = Math.round((1f - slide) * 8f);
        for (FeatureCard card : cards) {
            card.setY(card.baseY - scroll + slidePx);
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
        long now = Util.getMillis();
        animDt = lastAnimMillis == 0L ? 0.016f : (now - lastAnimMillis) / 1000f;
        lastAnimMillis = now;
        animDt = Mth.clamp(animDt, 0.001f, 0.05f);
        maxScroll = Math.max(0, contentH - Math.max(1, cardsBottom - cardsTop));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        slide = slide + (1f - slide) * 0.35f;
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
        for (SectionMark section : sections) {
            int divY = section.y - scroll + Math.round((1f - slide) * 8f);
            if (divY + section.h <= cardsTop || divY >= cardsBottom) {
                continue;
            }
            int color = section.admin ? 0xFFC96060 : 0xFFB8B8C4;
            int margin = layout.margin;
            int lineY = divY + section.h / 2;
            graphics.fill(margin, lineY, width - margin, lineY + 1, section.admin ? 0xFF5A3030 : 0xFF3A3A44);
            Component label = Component.literal(section.title.getString() + "  " + section.count);
            int lx = margin + 4;
            int lw = 10 + font.width(label);
            graphics.fill(lx - 2, divY, lx + lw + 4, divY + section.h, 0xF008080C);
            drawChevron(graphics, lx, divY + Math.max(0, (section.h - 5) / 2),
                    chevron(section.key), color);
            graphics.drawString(font, label, lx + 10, divY + Math.max(0, (section.h - 8) / 2), color, false);
        }
        if (cards.isEmpty()) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen." + Features.MODID + ".search.empty"),
                    width / 2, cardsTop + 12, 0x808088);
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
        drawSearchHits(graphics, mouseX, mouseY);
            for (var child : children()) {
                if (child instanceof DarkButton button && button.isHovered()) {
                Component tip = switch (button.getMessage().getString()) {
                    case "P" -> Component.translatable("screen." + Features.MODID + ".presets");
                    case "On" -> Component.translatable("screen." + Features.MODID + ".filter.on");
                    case "Off" -> Component.translatable("screen." + Features.MODID + ".filter.off");
                    case "*" -> Component.translatable("screen." + Features.MODID + ".filter.any");
                    case "Az" -> Component.translatable("screen." + Features.MODID + ".sort.az");
                    case "#" -> Component.translatable("screen." + Features.MODID + ".sort.category");
                    default -> button.getMessage();
                };
                Tooltips.show(graphics, font, tip, mouseX, mouseY);
            }
        }
        drawChipTooltip(graphics, mouseX, mouseY);
        drawSparks(graphics);
    }

    /** Full category name sits above the bar, and only while the cursor is on that chip. */
    private void drawChipTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (var child : children()) {
            if (!(child instanceof MenuChip chip) || !chip.isMouseOver(mouseX, mouseY)) {
                continue;
            }
            String text = chip.getMessage().getString();
            if (font.width(text) <= chip.getWidth() - 4) {
                return;
            }
            int textW = font.width(text);
            int x = Mth.clamp(chip.getX() + chip.getWidth() / 2 - textW / 2 - 4, 2, width - textW - 10);
            int y = Math.max(2, chip.getY() - 14);
            graphics.fill(x, y, x + textW + 8, y + 12, 0xF0101016);
            graphics.drawString(font, text, x + 4, y + 2, 0xF0F0F0, false);
            return;
        }
    }

    /** 1 = collapsed (points right), 0 = open (points down). */
    private float chevron(String key) {
        float target = MenuPrefs.isCollapsed(key) ? 1f : 0f;
        float current = chevronAnim.getOrDefault(key, target);
        float k = 1f - (float) Math.exp(-14f * animDt);
        current = Mth.lerp(k, current, target);
        if (Math.abs(current - target) < 0.004f) {
            current = target;
        }
        chevronAnim.put(key, current);
        return current;
    }

    /** Right-pointing triangle, rotated toward down as the group opens. */
    private void drawChevron(GuiGraphics graphics, int x, int y, float collapsed, int color) {
        float angle = (1f - collapsed) * 90f;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + 1.5f, y + 2.5f, 0f);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-1.5f, -2.5f, 0f);
        graphics.fill(0, 0, 1, 5, color);
        graphics.fill(1, 1, 2, 4, color);
        graphics.fill(2, 2, 3, 3, color);
        pose.popPose();
    }

    private void popStar(String id, String section, boolean favoriteNow) {
        starPop.put(id, Util.getMillis());
        long born = Util.getMillis();
        int count = favoriteNow ? 10 : 6;
        for (int i = 0; i < count; i++) {
            double ang = i * (Math.PI * 2.0 / count) + (i % 2) * 0.18;
            float speed = 42f + (i % 3) * 14f;
            int color = favoriteNow
                    ? ((i % 2 == 0) ? 0xFFFFC305 : 0xFFFFF1C2)
                    : 0xFFC8C8D0;
            sparks.add(new Spark(id, section, (float) Math.cos(ang) * speed, (float) Math.sin(ang) * speed, born, color));
        }
    }

    /** Star center after layout. Sparks follow this card when favorites push it down. */
    private int[] starCenter(String featureId, String section) {
        FeatureCard fallback = null;
        for (FeatureCard card : cards) {
            if (!card.sameFeature(featureId)) {
                continue;
            }
            if (card.sameSection(section)) {
                return new int[] {card.starCenterX(), card.starCenterY()};
            }
            if (fallback == null) {
                fallback = card;
            }
        }
        return fallback == null ? null : new int[] {fallback.starCenterX(), fallback.starCenterY()};
    }

    private float starPopScale(String id) {
        Long born = starPop.get(id);
        if (born == null) {
            return 1f;
        }
        float age = (Util.getMillis() - born) / 260f;
        if (age >= 1f) {
            starPop.remove(id);
            return 1f;
        }
        return 1f + Mth.sin(age * (float) Math.PI) * 0.42f;
    }

    private void drawSparks(GuiGraphics graphics) {
        if (sparks.isEmpty()) {
            return;
        }
        long now = Util.getMillis();
        Iterator<Spark> it = sparks.iterator();
        while (it.hasNext()) {
            Spark spark = it.next();
            float t = (now - spark.born) / 1000f;
            if (t > 0.55f) {
                it.remove();
                continue;
            }
            int[] origin = starCenter(spark.featureId, spark.section);
            if (origin == null) {
                it.remove();
                continue;
            }
            int px = Math.round(origin[0] + spark.vx * t);
            int py = Math.round(origin[1] + spark.vy * t);
            int alpha = Math.round(255f * (1f - t / 0.55f));
            int color = (alpha << 24) | (spark.color & 0xFFFFFF);
            graphics.fill(px, py, px + 2, py + 2, color);
        }
    }

    private void drawSearchHits(GuiGraphics graphics, int mouseX, int mouseY) {
        if (searchHits.isEmpty() || searchBox == null || query.isBlank()) {
            return;
        }
        int x = searchBox.getX();
        int y = searchBox.getY() + searchBox.getHeight() + 2;
        int w = Math.max(searchBox.getWidth(), 180);
        int row = 12;
        graphics.fill(x, y, x + w, y + searchHits.size() * row + 4, 0xF0101016);
        for (int i = 0; i < searchHits.size(); i++) {
            SettingCatalog.Hit hit = searchHits.get(i);
            int rowY = y + 2 + i * row;
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= rowY && mouseY < rowY + row;
            if (hover) {
                graphics.fill(x + 1, rowY, x + w - 1, rowY + row, 0xFF2A2A32);
            }
            Feature feature = featureById(hit.featureId());
            String title = feature == null ? hit.featureId() : feature.getTitle().getString();
            String line = truncateToWidth(title + " — " + hit.label(), w - 8);
            graphics.drawString(font, line, x + 4, rowY + 2, 0xE0E0E8, false);
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

    private void openSettings(Feature feature, String settingKey) {
        if (minecraft == null) {
            return;
        }
        SettingFocus.arm(settingKey);
        minecraft.setScreen(FeatureSettingsScreens.open(feature, this));
    }

    private Feature featureById(String id) {
        for (Feature feature : FeatureRegistry.all()) {
            if (feature.getId().equals(id)) {
                return feature;
            }
        }
        return null;
    }

    private boolean clickSearchHit(double mouseX, double mouseY) {
        if (searchHits.isEmpty() || searchBox == null || query.isBlank()) {
            return false;
        }
        int x = searchBox.getX();
        int y = searchBox.getY() + searchBox.getHeight() + 2;
        int w = Math.max(searchBox.getWidth(), 180);
        int row = 12;
        if (mouseX < x || mouseX >= x + w || mouseY < y || mouseY >= y + searchHits.size() * row + 4) {
            return false;
        }
        int index = (int) ((mouseY - y - 2) / row);
        if (index < 0 || index >= searchHits.size()) {
            return false;
        }
        SettingCatalog.Hit hit = searchHits.get(index);
        Feature feature = featureById(hit.featureId());
        if (feature != null && feature.hasSettings() && feature.canPlayerConfigure()) {
            openSettings(feature, hit.key());
        }
        return true;
    }

    private boolean clickSection(double mouseX, double mouseY) {
        if (mouseY < cardsTop || mouseY >= cardsBottom) {
            return false;
        }
        int contentY = (int) mouseY + scroll - Math.round((1f - slide) * 8f);
        for (SectionMark section : sections) {
            if (contentY >= section.y && contentY < section.y + section.h
                    && mouseX >= layout.margin && mouseX < width - layout.margin) {
                MenuPrefs.toggleCollapsed(section.key);
                slide = 0f;
                layoutContent();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && clickSearchHit(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && clickSection(mouseX, mouseY)) {
            return true;
        }
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F && hasControlDown()) {
            if (searchBox != null) {
                setFocused(searchBox);
                searchBox.setFocused(true);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !query.isEmpty()) {
            query = "";
            searchHits.clear();
            if (searchBox != null) {
                searchBox.setValue("");
                searchBox.setFocused(false);
            }
            slide = 0f;
            layoutContent();
            return true;
        }
        boolean typing = searchBox != null && searchBox.isFocused();
        if (!typing && !cards.isEmpty() && (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_LEFT
                || keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP)) {
            int columns = COLUMNS;
            int next = keyboardIndex < 0 ? 0 : keyboardIndex;
            next = switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> Math.max(0, next - 1);
                case GLFW.GLFW_KEY_RIGHT -> Math.min(cards.size() - 1, next + 1);
                case GLFW.GLFW_KEY_UP -> Math.max(0, next - columns);
                default -> Math.min(cards.size() - 1, next + columns);
            };
            keyboardIndex = next;
            FeatureCard card = cards.get(next);
            if (card.baseY - scroll < cardsTop) {
                scroll = Math.max(0, card.baseY - cardsTop);
            } else if (card.baseY + card.getHeight() - scroll > cardsBottom) {
                scroll = Math.min(maxScroll, card.baseY + card.getHeight() - cardsBottom);
            }
            syncCards();
            return true;
        }
        if (!typing && keyCode == GLFW.GLFW_KEY_ENTER && keyboardIndex >= 0 && keyboardIndex < cards.size()) {
            Feature feature = cards.get(keyboardIndex).feature;
            if (feature.hasSettings() && feature.canPlayerConfigure()) {
                openSettings(feature, "");
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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

    private class MenuChip extends AbstractWidget {
        private final java.util.function.BooleanSupplier selected;
        private final Runnable action;

        private MenuChip(int x, int y, int w, int h, Component message,
                         java.util.function.BooleanSupplier selected, Runnable action) {
            super(x, y, w, h, message);
            this.selected = selected;
            this.action = action;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean on = selected.getAsBoolean();
            int fill = on ? 0xFF2A3A38 : (isHoveredOrFocused() ? 0xFF2A2A32 : 0xFF1A1A22);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
            if (on) {
                graphics.fill(getX(), getY() + height - 2, getX() + width, getY() + height, 0xFF9A9AA8);
            }
            String text = truncateToWidth(getMessage().getString(), Math.max(4, width - 4));
            graphics.drawCenteredString(font, text, getX() + width / 2, getY() + (height - 8) / 2,
                    on ? 0xF0F0F0 : 0x9A9AA8);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            setFocused(false);
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private record SectionMark(String key, Component title, boolean admin, int count, int y, int h) {
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
        final int barY;
        final int barH;

        private Layout(float ui, int margin, int gap, int cardW, int pad, int icon,
                       int titleW, int titleH, int titleY, int cardsY, int headerH, int lineH,
                       int toggleW, int toggleH, int settingsH, int minCardH, int hoverPad,
                       int bottomPad, int bottomReserve, int btnW, int btnH, int barY, int barH) {
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
            this.barY = barY;
            this.barH = barH;
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
            int barH = Math.max(16, Math.round(18 * ui));
            int barY = titleY + titleH + Math.max(2, Math.round(3 * ui));
            int cardsY = barY + barH + Math.max(4, Math.round(6 * ui));
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
                    bottomPad, bottomReserve, btnW, btnH, barY, barH);
        }
    }

    private class FeatureCard extends AbstractWidget {
        private static final float HOVER_SCALE = 0.08f;
        private static final float HOVER_SPEED_IN = 3.2f;
        private static final float HOVER_SPEED_OUT = 2.1f;
        private static final float SWAY_DEG = 0.16f;
        private static final float TOGGLE_MS = 260f;
        private static final float GEAR_DEG_PER_SEC = 18f;

        private final Feature feature;
        private String section = "";
        private final List<FormattedCharSequence> descriptionLines;
        private final Layout L;
        private final int baseY;
        private final float swayPhase;
        private float hoverAnim;
        private float toggleShown;
        private float toggleFrom;
        private float toggleTo;
        private long toggleStart;
        private boolean toggleReady;
        private float gearAngle;

        private FeatureCard(int x, int y, int w, int h, Feature feature,
                            List<FormattedCharSequence> descriptionLines, Layout layout) {
            super(x, y, w, h, feature.getTitle());
            this.feature = feature;
            this.descriptionLines = descriptionLines;
            this.L = layout;
            this.baseY = y;
            this.swayPhase = (feature.getId().hashCode() * 0.6180339f) % ((float) Math.PI * 2f);
        }

        private void stepToggle() {
            float target = feature.isEnabled() ? 1f : 0f;
            if (!toggleReady) {
                toggleReady = true;
                toggleShown = target;
                toggleFrom = target;
                toggleTo = target;
                toggleStart = Util.getMillis();
                return;
            }
            if (target != toggleTo) {
                toggleFrom = toggleShown;
                toggleTo = target;
                toggleStart = Util.getMillis();
            }
            float u = Mth.clamp((Util.getMillis() - toggleStart) / TOGGLE_MS, 0f, 1f);
            float eased = u * u * (3f - 2f * u);
            toggleShown = Mth.lerp(eased, toggleFrom, toggleTo);
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

            stepToggle();

            float ease = hoverEase();
            float scale = 1f + ease * HOVER_SCALE;
            float cx = cardCx();
            float cy = cardCy();
            // Chaotic multi-frequency sway — unique phase per feature, not a metronome rock.
            float t = Util.getMillis() * 0.001f;
            float p = swayPhase;
            float sway = (
                    Mth.sin(t * 1.1f + p) * 0.62f
                            + Mth.sin(t * 1.7f + p * 1.3f) * 0.38f
            ) * SWAY_DEG * ease;
            float bobX = Mth.sin(t * 1.3f + p) * 0.35f * ease;
            float bobY = Mth.sin(t * 1.05f + p * 0.8f) * 0.22f * ease;

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
            if (FeaturesScreen.this.keyboardIndex >= 0
                    && FeaturesScreen.this.cards.indexOf(this) == FeaturesScreen.this.keyboardIndex) {
                drawGradientBorder(graphics, getX() - 1, getY() - 1, width + 2, height + 2, 0xFFE8E8F0, 0xFF3A3A44);
            }

            ResourceLocation icon = feature.getIcon();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            float iconTilt = Mth.sin(t * 0.85f + swayPhase) * 2.6f;
            float iconScale = 1f + 0.035f * (0.5f + 0.5f * Mth.sin(t * 1.5f + swayPhase)) + ease * 0.07f;
            int ix = getX() + L.pad;
            int iy = getY() + L.pad;
            pose.pushPose();
            pose.translate(ix + L.icon * 0.5f, iy + L.icon * 0.5f, 0f);
            pose.mulPose(Axis.ZP.rotationDegrees(iconTilt));
            pose.scale(iconScale, iconScale, 1f);
            pose.translate(-L.icon * 0.5f, -L.icon * 0.5f, 0f);
            graphics.blit(icon, 0, 0, L.icon, L.icon, 0f, 0f, 32, 32, 32, 32);
            pose.popPose();

            int titleX = getX() + L.pad + L.icon + Math.max(4, L.pad / 2);
            int titleY = getY() + L.pad + Math.max(0, (L.icon - 8) / 2);
            int star = Math.max(12, L.icon / 2);
            int titleMaxW = Math.max(8, starX(star) - titleX - 4);
            graphics.drawString(font, truncateToWidth(feature.getTitle().getString(), titleMaxW), titleX, titleY, 0xF0F0F0, false);

            int dy = getY() + L.headerH;
            int badgeY = feature.hasSettings() ? settingsY() - 12 : getY() + height - L.pad - 10;
            int maxY = badgeY - 2;
            for (FormattedCharSequence line : descriptionLines) {
                if (dy + L.lineH > maxY) {
                    break;
                }
                graphics.drawString(font, line, getX() + L.pad, dy, 0x9A9AA8, false);
                dy += L.lineH;
            }

            boolean editable = feature.canPlayerConfigure();
            drawStar(graphics, starX(star), toggleY(), star, MenuPrefs.isFavorite(feature));
            drawToggle(graphics, toggleX(), toggleY(), editable);
            drawBadges(graphics, badgeY);

            double lmx = localX(mouseX);
            double lmy = localY(mouseY);

            if (feature.hasSettings()) {
                int sx = settingsX();
                int sy = settingsY();
                int sw = settingsW();
                boolean hover = editable && lmx >= sx && lmx < sx + sw && lmy >= sy && lmy < sy + L.settingsH;
                if (hover) {
                    gearAngle += animDt * GEAR_DEG_PER_SEC;
                }
                graphics.fill(sx, sy, sx + sw, sy + L.settingsH, hover ? 0xFF2A3A38 : 0xFF1E1E26);
                drawGear(graphics, sx + 3, sy + (L.settingsH - GEAR_H) / 2f, gearAngle);
                graphics.drawCenteredString(font,
                        Component.translatable("screen." + Features.MODID + ".feature_settings"),
                        sx + sw / 2 + 4, sy + Math.max(2, (L.settingsH - 8) / 2),
                        editable ? (hover ? 0xF0F0F0 : 0xB0B0BC) : 0x606068);
            }
            if (!feature.isEnabled()) {
                graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x66000000);
            }

            pose.popPose();
        }

        private int starX(int star) {
            return toggleX() - star - 3;
        }

        void bindSection(String section) {
            this.section = section == null ? "" : section;
        }

        boolean sameFeature(String id) {
            return feature.getId().equals(id);
        }

        boolean sameSection(String section) {
            return this.section.equals(section == null ? "" : section);
        }

        int starCenterX() {
            int star = Math.max(12, L.icon / 2);
            return starX(star) + star / 2;
        }

        int starCenterY() {
            int star = Math.max(12, L.icon / 2);
            return toggleY() + star / 2;
        }

        private void drawStar(GuiGraphics graphics, int x, int y, int size, boolean on) {
            ResourceLocation tex = on ? STAR_ON : STAR_OFF;
            int tw = on ? STAR_ON_W : STAR_OFF_W;
            float pop = starPopScale(feature.getId());
            float cx = x + size * 0.5f;
            float cy = y + size * 0.5f;
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(cx, cy, 0f);
            pose.scale(pop, pop, 1f);
            pose.translate(-tw * 0.5f, -STAR_H * 0.5f, 0f);
            graphics.blit(tex, 0, 0, tw, STAR_H, 0f, 0f, tw, STAR_H, tw, STAR_H);
            pose.popPose();
        }

        private void drawGear(GuiGraphics graphics, float x, float y, float angle) {
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(x + GEAR_W * 0.5f, y + GEAR_H * 0.5f, 0f);
            pose.mulPose(Axis.ZP.rotationDegrees(angle));
            pose.translate(-GEAR_W * 0.5f, -GEAR_H * 0.5f, 0f);
            graphics.blit(GEAR_TEX, 0, 0, GEAR_W, GEAR_H, 0f, 0f, GEAR_W, GEAR_H, GEAR_W, GEAR_H);
            pose.popPose();
        }

        private void drawBadges(GuiGraphics graphics, int y) {
            int x = getX() + L.pad;
            if (feature.requiresOperator()) {
                x = badge(graphics, x, y, "\u2694", "screen." + Features.MODID + ".badge.admin", 0xFFC96060);
            }
            if (feature.category() == FeatureCategory.SERVER) {
                x = badge(graphics, x, y, "\u2601", "screen." + Features.MODID + ".badge.server", 0xFF4A9EBF);
            }
            if (feature.optionalByDefault()) {
                badge(graphics, x, y, "\u2666", "screen." + Features.MODID + ".badge.optional", 0xFF8A8A96);
            }
        }

        private int badge(GuiGraphics graphics, int x, int y, String icon, String key, int color) {
            String text = icon + " " + Component.translatable(key).getString();
            int w = font.width(text) + 4;
            if (x + w > getX() + width - L.pad) {
                return x;
            }
            graphics.drawString(font, text, x + 2, y, color, false);
            return x + w + 10;
        }

        private void drawToggle(GuiGraphics graphics, int tx, int ty, boolean editable) {
            ToggleSwitch.paint(graphics, tx, ty, L.toggleW, L.toggleH, toggleShown, editable);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            setFocused(false);
            if (mouseY < FeaturesScreen.this.cardsTop || mouseY >= FeaturesScreen.this.cardsBottom) {
                return;
            }
            double lmx = localX(mouseX);
            double lmy = localY(mouseY);

            int star = Math.max(12, L.icon / 2);
            int sxStar = starX(star);
            if (lmx >= sxStar && lmx < sxStar + star && lmy >= toggleY() && lmy < toggleY() + star) {
                MenuPrefs.toggleFavorite(feature);
                popStar(feature.getId(), section, MenuPrefs.isFavorite(feature));
                FeaturesScreen.this.slide = 0f;
                FeaturesScreen.this.layoutContent();
                return;
            }

            if (feature.hasSettings()) {
                int sx = settingsX();
                int sy = settingsY();
                int sw = settingsW();
                if (lmx >= sx && lmx < sx + sw && lmy >= sy && lmy < sy + L.settingsH) {
                    if (!feature.canPlayerConfigure()) {
                        return;
                    }
                    openSettings(feature, "");
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

    public static void drawGradientBorder(GuiGraphics graphics, int x, int y, int w, int h, int topRgb, int bottomRgb) {
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

    private static final class Spark {
        private final String featureId;
        private final String section;
        private final float vx;
        private final float vy;
        private final long born;
        private final int color;

        private Spark(String featureId, String section, float vx, float vy, long born, int color) {
            this.featureId = featureId;
            this.section = section == null ? "" : section;
            this.vx = vx;
            this.vy = vy;
            this.born = born;
            this.color = color;
        }
    }
}
