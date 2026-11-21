package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.lwjgl.glfw.GLFW;
import yblk.features.Features;
import yblk.features.client.ui.BaseSettingsScreen;
import yblk.features.client.ui.Checks;
import yblk.features.client.ui.DarkButton;
import yblk.features.client.ui.HintLines;
import yblk.features.client.ui.ScrollBar;
import yblk.features.client.ui.TextField;
import yblk.features.config.FeaturesConfig;
import yblk.features.network.ModNetwork;
import yblk.features.network.ToolRestrictionsPacket;
import yblk.features.restrict.Restrictions;
import yblk.features.restrict.ToolRestrictions;

import java.util.ArrayList;
import java.util.List;

public class ToolRestrictionsSettingsScreen extends BaseSettingsScreen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFFC96060;
    private static final int ROW = 16;
    private static final int CHECK = 9;

    private final List<Hit> hits = new ArrayList<>();
    private EditBox messageBox;
    private EditBox exceptionBox;
    private EditBox handBox;
    private boolean canEdit;
    private int panelL;
    private int panelR;
    private int panelTop;
    private int panelBottom;
    private int listTop;
    private int listBottom;
    private int scroll;
    private int maxScroll;

    public ToolRestrictionsSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".tool_restrictions"), parent);
    }

    @Override
    protected void init() {
        clearWidgets();
        hits.clear();
        canEdit = Restrictions.canEdit(minecraft != null ? minecraft.player : null);
        int btnH = 18;
        panelL = (width - Math.min(360, width - 16)) / 2;
        panelR = width - panelL;
        panelTop = 22;
        panelBottom = height - 6 - btnH - 6;

        int inner = panelR - panelL - 24;
        int addW = font.width(Component.translatable("screen." + Features.MODID + ".tool_restrictions.add")) + 12;
        messageBox = field(panelL + 12, panelTop + 60, inner, FeaturesConfig.toolMessage,
                "screen." + Features.MODID + ".tool_restrictions.message", value -> {
                    if (!value.equals(FeaturesConfig.toolMessage)) {
                        FeaturesConfig.setToolMessage(value);
                        push();
                    }
                });
        addRenderableWidget(messageBox);

        int exceptionY = panelTop + 96;
        exceptionBox = field(panelL + 12, exceptionY, inner - addW - 4, "",
                "screen." + Features.MODID + ".tool_restrictions.add", null);
        addRenderableWidget(exceptionBox);
        DarkButton addException = darkButton(panelR - 12 - addW, exceptionY, addW, 14,
                Component.translatable("screen." + Features.MODID + ".tool_restrictions.add"), this::addException);
        addException.active = canEdit;
        addRenderableWidget(addException);

        int handY = panelTop + 128;
        handBox = field(panelL + 12, handY, inner - addW - 4, "",
                "screen." + Features.MODID + ".tool_restrictions.add", null);
        addRenderableWidget(handBox);
        DarkButton addHand = darkButton(panelR - 12 - addW, handY, addW, 14,
                Component.translatable("screen." + Features.MODID + ".tool_restrictions.add"), this::addHand);
        addHand.active = canEdit;
        addRenderableWidget(addHand);

        listTop = handY + 20;
        listBottom = Math.max(listTop + 24, panelBottom - 4);

        addRenderableWidget(darkButton(width / 2 - 45, height - 6 - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    private EditBox field(int x, int y, int w, String value, String hint, java.util.function.Consumer<String> responder) {
        EditBox box = new TextField(font, x, y, Math.max(40, w), 14, Component.empty());
        box.setMaxLength(256);
        box.setValue(value);
        box.setHint(Component.translatable(hint));
        box.setEditable(canEdit);
        if (responder != null) {
            box.setResponder(text -> {
                if (canEdit) {
                    responder.accept(text);
                }
            });
        }
        return box;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        List<String> exceptions = FeaturesConfig.toolExceptions;
        List<String> hand = FeaturesConfig.toolHand;
        List<BlockMatch> catalog = catalog();
        int content = 14 + Math.max(1, catalog.size()) * ROW
                + 12 + Math.max(1, exceptions.size()) * ROW + 14 + Math.max(1, hand.size()) * ROW;
        maxScroll = Math.max(0, content - Math.max(1, listBottom - listTop));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xF0F0F0);
        graphics.fill(panelL, panelTop, panelR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelTop, panelR - panelL, panelBottom - panelTop, ACCENT, 0xFF0A0A0E);

        int x = panelL + 12;
        String hint = canEdit
                ? "screen." + Features.MODID + ".tool_restrictions.hint"
                : "screen." + Features.MODID + ".tool_restrictions.locked";
        HintLines.draw(graphics, font, Component.translatable(hint), x, panelTop + 4, panelR - panelL - 24,
                canEdit ? 0x808088 : 0xE05050, 3);

        Checks.box(graphics, x, panelTop + 36, CHECK, FeaturesConfig.toolSound, ACCENT);
        String sound = FeaturesConfig.toolSound
                ? "screen." + Features.MODID + ".tool_restrictions.sound.on"
                : "screen." + Features.MODID + ".tool_restrictions.sound.off";
        graphics.drawString(font, Component.translatable(sound), x + CHECK + 6, panelTop + 36, canEdit ? 0xF0F0F0 : 0x6A6A72, false);
        tipKey(x, panelTop + 32, panelR - 12 - x, 16, sound);
        if (canEdit) {
            hits.add(new Hit(x, panelTop + 32, panelR - 12 - x, 16, () -> {
                FeaturesConfig.setToolSound(!FeaturesConfig.toolSound);
                push();
            }));
        }

        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".tool_restrictions.message"),
                x, panelTop + 48, 0xC8C8D0, false);
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".tool_restrictions.exceptions"),
                x, panelTop + 84, ACCENT, false);
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".tool_restrictions.hand"),
                x, panelTop + 116, ACCENT, false);

        graphics.enableScissor(panelL + 4, listTop, panelR - 4, listBottom);
        int y = listTop - scroll;
        int rowW = panelR - 28 - x;
        y = catalogSection(graphics, x, y, rowW, catalog);
        y = section(graphics, x, y, exceptions, rowW, true);
        section(graphics, x, y, hand, rowW, false);
        graphics.disableScissor();
        ScrollBar.draw(graphics, panelR, listTop, listBottom, scroll, maxScroll, ACCENT, v -> scroll = v);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private List<BlockMatch> catalog() {
        String query = handBox != null && handBox.isFocused() ? handBox.getValue() : exceptionBox.getValue();
        List<BlockMatch> items = new ArrayList<>();
        List<BlockMatch> blocks = new ArrayList<>();
        for (Item item : net.minecraftforge.registries.ForgeRegistries.ITEMS) {
            ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
            if (id == null || item == Items.AIR) {
                continue;
            }
            ItemStack icon = new ItemStack(item);
            if (icon.isEmpty() || !CatalogSearch.matchesItem(icon, id, query)) {
                continue;
            }
            BlockMatch match = new BlockMatch(id.toString(), icon.getHoverName().getString(), icon);
            if (item instanceof BlockItem) {
                blocks.add(match);
            } else {
                items.add(match);
            }
        }
        items.addAll(blocks);
        return items;
    }

    private int catalogSection(GuiGraphics graphics, int x, int y, int rowW, List<BlockMatch> catalog) {
        if (y + 12 > listTop && y < listBottom) {
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".tool_restrictions.catalog"),
                    x, y + 1, ACCENT, false);
        }
        y += 12;
        if (catalog.isEmpty()) {
            return drawEmpty(graphics, x, y);
        }
        boolean toHand = handBox.isFocused();
        for (BlockMatch match : catalog) {
            if (y + ROW > listTop && y < listBottom) {
                if (!match.icon.isEmpty()) {
                    graphics.renderItem(match.icon, x, y);
                }
                graphics.drawString(font, font.plainSubstrByWidth(match.label, rowW - 22), x + 18, y + 4, 0xFFD0D0D8, false);
                tipKey(x, y, rowW, ROW, "screen." + Features.MODID + ".tool_restrictions.catalog");
                if (canEdit && y >= listTop && y + ROW <= listBottom) {
                    String id = match.id;
                    hits.add(new Hit(x, y, rowW, ROW, () -> addId(id, !toHand)));
                }
            }
            y += ROW;
        }
        return y;
    }

    private int drawEmpty(GuiGraphics graphics, int x, int y) {
        if (y + ROW > listTop && y < listBottom) {
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".tool_restrictions.empty"),
                    x, y + 3, 0x6A6A72, false);
        }
        return y + ROW;
    }

    private int section(GuiGraphics graphics, int x, int y, List<String> entries, int rowW, boolean exceptions) {
        String header = exceptions
                ? "screen." + Features.MODID + ".tool_restrictions.exceptions"
                : "screen." + Features.MODID + ".tool_restrictions.hand";
        if (y + 12 > listTop && y < listBottom) {
            graphics.drawString(font, Component.translatable(header), x, y + 1, 0x808088, false);
        }
        y += 12;
        if (entries.isEmpty()) {
            if (y + ROW > listTop && y < listBottom) {
                graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".tool_restrictions.empty"),
                        x, y + 3, 0x6A6A72, false);
            }
            return y + ROW;
        }
        for (String entry : entries) {
            if (y + ROW > listTop && y < listBottom) {
                ItemStack icon = icon(entry);
                int textX = x;
                if (!icon.isEmpty()) {
                    graphics.renderItem(icon, x, y);
                    textX = x + 18;
                }
                String cut = font.plainSubstrByWidth(entry, x + rowW - textX - 14);
                graphics.drawString(font, cut, textX, y + 4, 0xF0F0F0, false);
                tipKey(x, y, rowW, ROW, header);
                graphics.drawString(font, "x", x + rowW - 8, y + 4, 0xC96060, false);
                if (canEdit && y >= listTop && y + ROW <= listBottom) {
                    String id = entry;
                    hits.add(new Hit(x, y, rowW, ROW, () -> remove(id, exceptions)));
                }
            }
            y += ROW;
        }
        return y;
    }

    private ItemStack icon(String entry) {
        Block block = ToolRestrictions.blockOf(entry);
        if (block == null) {
            return ItemStack.EMPTY;
        }
        Item item = block.asItem();
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void addException() {
        add(exceptionBox, true);
    }

    private void addHand() {
        add(handBox, false);
    }

    private void add(EditBox box, boolean exceptions) {
        if (!canEdit) {
            return;
        }
        String id = ToolRestrictions.normalizeRef(box.getValue());
        if (id == null) {
            return;
        }
        List<String> next = new ArrayList<>(exceptions ? FeaturesConfig.toolExceptions : FeaturesConfig.toolHand);
        for (String existing : next) {
            if (existing.equalsIgnoreCase(id)) {
                box.setValue("");
                return;
            }
        }
        next.add(id);
        if (exceptions) {
            FeaturesConfig.setToolExceptions(next);
        } else {
            FeaturesConfig.setToolHand(next);
        }
        box.setValue("");
        push();
    }

    private void addId(String id, boolean exceptions) {
        if (!canEdit || id == null) {
            return;
        }
        List<String> next = new ArrayList<>(exceptions ? FeaturesConfig.toolExceptions : FeaturesConfig.toolHand);
        for (String existing : next) {
            if (existing.equalsIgnoreCase(id)) {
                return;
            }
        }
        next.add(id);
        if (exceptions) {
            FeaturesConfig.setToolExceptions(next);
        } else {
            FeaturesConfig.setToolHand(next);
        }
        push();
    }

    private void remove(String id, boolean exceptions) {
        if (!canEdit) {
            return;
        }
        List<String> source = exceptions ? FeaturesConfig.toolExceptions : FeaturesConfig.toolHand;
        List<String> next = new ArrayList<>();
        for (String existing : source) {
            if (!existing.equalsIgnoreCase(id)) {
                next.add(existing);
            }
        }
        if (exceptions) {
            FeaturesConfig.setToolExceptions(next);
        } else {
            FeaturesConfig.setToolHand(next);
        }
        push();
    }

    private void push() {
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetwork.sendToServer(ToolRestrictionsPacket.current());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Hit hit : hits) {
                if (mouseX < hit.x || mouseX >= hit.x + hit.w || mouseY < hit.y || mouseY >= hit.y + hit.h) {
                    continue;
                }
                if (hit.y >= listTop && (mouseY < listTop || mouseY >= listBottom)) {
                    continue;
                }
                hit.action.run();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= listTop && mouseY < listBottom) {
            scroll = Mth.clamp(scroll - (int) (delta * ROW), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (exceptionBox.isFocused()) {
                addException();
                return true;
            }
            if (handBox.isFocused()) {
                addHand();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected int buttonAccent() {
        return ACCENT;
    }

    @Override
    protected int buttonHover() {
        return 0xFF3A2A2A;
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }

    private record BlockMatch(String id, String label, ItemStack icon) {
    }
}
