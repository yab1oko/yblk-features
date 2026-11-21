package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.Features;
import yblk.features.client.ui.HintLines;
import yblk.features.client.ui.BaseSettingsScreen;
import yblk.features.client.ui.Checks;
import yblk.features.client.ui.DarkButton;
import yblk.features.client.ui.ScrollBar;
import yblk.features.client.ui.TextField;
import yblk.features.config.FeaturesConfig;
import yblk.features.network.InteractionBlockerPacket;
import yblk.features.network.ModNetwork;
import yblk.features.restrict.InteractionRules;
import yblk.features.restrict.Restrictions;

import java.util.ArrayList;
import java.util.List;

public class InteractionBlockerSettingsScreen extends BaseSettingsScreen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFFC96060;
    private static final int ROW = 18;
    private static final int CHECK = 9;

    private final List<Hit> hits = new ArrayList<>();
    private EditBox searchBox;
    private EditBox messageBox;
    private DarkButton blocksButton;
    private DarkButton entitiesButton;
    private boolean blocksMode = true;
    private boolean canEdit;
    private String selected;
    private String cacheQuery = "\u0000";
    private boolean cacheBlocks = true;
    private List<Match> matches = List.of();
    private int panelL;
    private int panelR;
    private int panelTop;
    private int panelBottom;
    private int searchY;
    private int chipY;
    private int bypassY;
    private int messageY;
    private int flagY;
    private int listTop;
    private int listBottom;
    private int scroll;
    private int maxScroll;

    public InteractionBlockerSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".interaction_blocker"), parent);
    }

    @Override
    protected void init() {
        clearWidgets();
        hits.clear();
        canEdit = Restrictions.canEdit(minecraft != null ? minecraft.player : null);
        int btnH = 18;
        panelL = (width - Math.min(380, width - 16)) / 2;
        panelR = width - panelL;
        panelTop = 22;
        panelBottom = height - 6 - btnH - 6;
        searchY = panelTop + 36;
        chipY = searchY + 18;
        bypassY = chipY + 18;
        messageY = bypassY + 18;
        flagY = messageY + 18;
        listTop = flagY + 16;
        listBottom = Math.max(listTop + 24, panelBottom - 4);

        int innerW = panelR - panelL - 24;
        searchBox = new TextField(font, panelL + 12, searchY, innerW, 14, Component.empty());
        searchBox.setMaxLength(80);
        searchBox.setHint(Component.translatable("screen." + Features.MODID + ".interaction_blocker.search"));
        searchBox.setEditable(canEdit);
        addRenderableWidget(searchBox);

        int blocksW = font.width(Component.translatable("screen." + Features.MODID + ".interaction_blocker.blocks")) + 12;
        int entitiesW = font.width(Component.translatable("screen." + Features.MODID + ".interaction_blocker.entities")) + 12;
        blocksButton = darkButton(panelL + 12, chipY, blocksW, 16,
                Component.translatable("screen." + Features.MODID + ".interaction_blocker.blocks"), () -> blocksMode = true);
        entitiesButton = darkButton(panelL + 16 + blocksW, chipY, entitiesW, 16,
                Component.translatable("screen." + Features.MODID + ".interaction_blocker.entities"), () -> blocksMode = false);
        blocksButton.active = canEdit;
        entitiesButton.active = canEdit;
        addRenderableWidget(blocksButton);
        addRenderableWidget(entitiesButton);

        messageBox = new TextField(font, panelL + 12, messageY, innerW, 14, Component.empty());
        messageBox.setMaxLength(256);
        messageBox.setValue(FeaturesConfig.interactionMessage);
        messageBox.setHint(Component.translatable("screen." + Features.MODID + ".interaction_blocker.message"));
        messageBox.setResponder(value -> {
            if (canEdit && !value.equals(FeaturesConfig.interactionMessage)) {
                FeaturesConfig.setInteractionMessage(value);
                push();
            }
        });
        messageBox.setEditable(canEdit);
        addRenderableWidget(messageBox);

        addRenderableWidget(darkButton(width / 2 - 45, height - 6 - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        List<InteractionRules.Rule> blocked = InteractionRules.all();
        List<Match> found = currentMatches();
        String query = searchBox.getValue().trim();
        int content = 12 + Math.max(1, blocked.size()) * ROW;
        content += 14 + Math.max(1, found.size()) * ROW;
        int view = Math.max(1, listBottom - listTop);
        maxScroll = Math.max(0, content - view);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xF0F0F0);
        graphics.fill(panelL, panelTop, panelR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelTop, panelR - panelL, panelBottom - panelTop, ACCENT, 0xFF0A0A0E);

        int x = panelL + 12;
        String hint = canEdit
                ? "screen." + Features.MODID + ".interaction_blocker.hint"
                : "screen." + Features.MODID + ".interaction_blocker.locked";
        HintLines.draw(graphics, font, Component.translatable(hint), x, panelTop + 4, panelR - panelL - 24,
                canEdit ? 0x808088 : 0xE05050, 3);

        drawCheck(graphics, x, bypassY + 3, FeaturesConfig.interactionOpBypass);
        graphics.drawString(font,
                Component.translatable("screen." + Features.MODID + ".interaction_blocker.op_bypass"),
                x + CHECK + 6, bypassY + 3, canEdit ? 0xF0F0F0 : 0x6A6A72, false);
        tipKey(x, bypassY, panelR - 12 - x, 16, "screen." + Features.MODID + ".interaction_blocker.op_bypass");
        if (canEdit) {
            hits.add(new Hit(x, bypassY, panelR - 12 - x, 16, () -> {
                FeaturesConfig.setInteractionOpBypass(!FeaturesConfig.interactionOpBypass);
                push();
            }));
        }

        InteractionRules.Rule selectedRule = InteractionRules.find(selected);
        int fx = x;
        fx = drawFlag(graphics, fx, flagY, "open", selectedRule != null && selectedRule.open, selectedRule);
        fx = drawFlag(graphics, fx, flagY, "use", selectedRule != null && selectedRule.use, selectedRule);
        fx = drawFlag(graphics, fx, flagY, "right", selectedRule != null && selectedRule.right, selectedRule);
        drawFlag(graphics, fx, flagY, "break", selectedRule != null && selectedRule.breakBlock, selectedRule);

        graphics.enableScissor(panelL + 4, listTop, panelR - 4, listBottom);
        int y = listTop - scroll;
        int rowW = panelR - 28 - x;
        y = drawHeader(graphics, x, y, "screen." + Features.MODID + ".interaction_blocker.blocked");
        if (blocked.isEmpty()) {
            y = drawMuted(graphics, x, y, "screen." + Features.MODID + ".interaction_blocker.empty");
        }
        for (InteractionRules.Rule rule : blocked) {
            y = drawRuleRow(graphics, x, y, rowW, rule.key(), labelOf(rule), iconOf(rule), true, rule);
        }
        y = drawHeader(graphics, x, y, "screen." + Features.MODID + ".interaction_blocker.matches");
        if (found.isEmpty()) {
            drawMuted(graphics, x, y, "screen." + Features.MODID + ".interaction_blocker.none");
        }
        for (Match match : found) {
            boolean on = InteractionRules.find(match.key) != null;
            y = drawRuleRow(graphics, x, y, rowW, match.key, match.label, match.icon, on, match.template);
        }
        graphics.disableScissor();
        ScrollBar.draw(graphics, panelR, listTop, listBottom, scroll, maxScroll, ACCENT, v -> scroll = v);

        if (blocksMode) {
            graphics.fill(blocksButton.getX(), blocksButton.getY() + 14, blocksButton.getX() + blocksButton.getWidth(), blocksButton.getY() + 16, ACCENT);
        } else {
            graphics.fill(entitiesButton.getX(), entitiesButton.getY() + 14, entitiesButton.getX() + entitiesButton.getWidth(), entitiesButton.getY() + 16, ACCENT);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int drawHeader(GuiGraphics graphics, int x, int y, String key) {
        if (y + 12 > listTop && y < listBottom) {
            graphics.drawString(font, Component.translatable(key), x, y + 2, ACCENT, false);
        }
        return y + 12;
    }

    private int drawMuted(GuiGraphics graphics, int x, int y, String key) {
        if (y + ROW > listTop && y < listBottom) {
            graphics.drawString(font, Component.translatable(key), x, y + 4, 0x6A6A72, false);
        }
        return y + ROW;
    }

    private int drawRuleRow(GuiGraphics graphics, int x, int y, int rowW, String key, Component label,
                            ItemStack icon, boolean on, InteractionRules.Rule template) {
        if (y + ROW > listTop && y < listBottom) {
            if (key.equals(selected)) {
                graphics.fill(x, y, x + rowW, y + ROW, 0xFF2A1820);
            }
            drawCheck(graphics, x, y + 4, on);
            int textX = x + 14;
            if (icon != null && !icon.isEmpty()) {
                graphics.renderItem(icon, textX, y);
                textX += 18;
            }
            String cut = font.plainSubstrByWidth(label.getString(), x + rowW - textX - 4);
            graphics.drawString(font, cut, textX, y + 4, on ? 0xF0F0F0 : 0xC8C8D0, false);
            if (canEdit && y >= listTop && y + ROW <= listBottom) {
                hits.add(new Hit(x, y, 14, ROW, () -> toggle(key, template)));
                hits.add(new Hit(x + 14, y, rowW - 14, ROW, () -> selected = key));
            }
        }
        return y + ROW;
    }

    private int drawFlag(GuiGraphics graphics, int x, int y, String flag, boolean on, InteractionRules.Rule rule) {
        boolean active = canEdit && rule != null;
        Component label = Component.translatable("screen." + Features.MODID + ".interaction_blocker." + flag);
        int w = CHECK + 6 + font.width(label);
        drawCheck(graphics, x, y + 3, on);
        graphics.drawString(font, label, x + CHECK + 4, y + 3, active ? 0xF0F0F0 : 0x6A6A72, false);
        tipKey(x, y, w, 14, "screen." + Features.MODID + ".interaction_blocker." + flag);
        if (active) {
            hits.add(new Hit(x, y, w, 14, () -> toggleFlag(rule, flag)));
        }
        return x + w + 8;
    }

    private void drawCheck(GuiGraphics graphics, int x, int y, boolean on) {
        Checks.box(graphics, x, y, CHECK, on, ACCENT);
    }

    private void toggle(String key, InteractionRules.Rule template) {
        if (!canEdit) {
            return;
        }
        if (InteractionRules.find(key) != null) {
            InteractionRules.remove(key);
            if (key.equals(selected)) {
                selected = null;
            }
        } else if (template != null) {
            InteractionRules.put(template);
            selected = key;
        }
        push();
    }

    private void toggleFlag(InteractionRules.Rule rule, String flag) {
        if (!canEdit || rule == null) {
            return;
        }
        boolean open = "open".equals(flag) ? !rule.open : rule.open;
        boolean use = "use".equals(flag) ? !rule.use : rule.use;
        boolean right = "right".equals(flag) ? !rule.right : rule.right;
        boolean breakBlock = "break".equals(flag) ? !rule.breakBlock : rule.breakBlock;
        if (!open && !use && !right && !breakBlock) {
            InteractionRules.remove(rule.key());
            selected = null;
        } else {
            InteractionRules.put(rule.with(open, use, right, breakBlock));
        }
        push();
    }

    private List<Match> currentMatches() {
        String query = searchBox.getValue();
        if (!query.equals(cacheQuery) || blocksMode != cacheBlocks) {
            cacheQuery = query;
            cacheBlocks = blocksMode;
            matches = buildMatches(query.trim());
        }
        return matches;
    }

    private List<Match> buildMatches(String query) {
        List<Match> out = new ArrayList<>();
        ResourceLocation tag = CatalogSearch.tagId(query);
        if (blocksMode) {
            if (tag != null) {
                InteractionRules.Rule rule = InteractionRules.blockTag(tag);
                out.add(new Match(rule.key(), Component.translatable("screen." + Features.MODID + ".interaction_blocker.tag", "#" + tag), ItemStack.EMPTY, rule));
            }
            for (Block block : ForgeRegistries.BLOCKS) {
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
                if (id == null || isAir(id)) {
                    continue;
                }
                if (!CatalogSearch.matchesBlock(block.defaultBlockState(), id, query)) {
                    continue;
                }
                Item item = block.asItem();
                ItemStack icon = item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
                InteractionRules.Rule rule = InteractionRules.block(id);
                out.add(new Match(rule.key(), block.getName(), icon, rule));
            }
        } else {
            if (tag != null) {
                InteractionRules.Rule rule = InteractionRules.entityTag(tag);
                out.add(new Match(rule.key(), Component.translatable("screen." + Features.MODID + ".interaction_blocker.tag", "#" + tag), ItemStack.EMPTY, rule));
            }
            for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
                ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
                if (id == null || !CatalogSearch.matchesEntity(type, id, query)) {
                    continue;
                }
                InteractionRules.Rule rule = InteractionRules.entity(id);
                out.add(new Match(rule.key(), type.getDescription(), ItemStack.EMPTY, rule));
            }
        }
        return out;
    }

    private static boolean isAir(ResourceLocation id) {
        return "minecraft".equals(id.getNamespace())
                && (id.getPath().equals("air") || id.getPath().equals("cave_air") || id.getPath().equals("void_air"));
    }

    private Component labelOf(InteractionRules.Rule rule) {
        return switch (rule.kind) {
            case BLOCK -> {
                Block block = InteractionRules.namedBlock(rule.id);
                yield block != null && rule.id.equals(ForgeRegistries.BLOCKS.getKey(block))
                        ? block.getName()
                        : Component.literal(rule.id.toString());
            }
            case ENTITY -> {
                EntityType<?> type = InteractionRules.namedEntity(rule.id);
                yield type != null && rule.id.equals(ForgeRegistries.ENTITY_TYPES.getKey(type))
                        ? type.getDescription()
                        : Component.literal(rule.id.toString());
            }
            case BLOCK_TAG, ENTITY_TAG -> Component.translatable(
                    "screen." + Features.MODID + ".interaction_blocker.tag", "#" + rule.id);
        };
    }

    private ItemStack iconOf(InteractionRules.Rule rule) {
        if (rule.kind != InteractionRules.Kind.BLOCK) {
            return ItemStack.EMPTY;
        }
        Block block = InteractionRules.namedBlock(rule.id);
        if (block == null || !rule.id.equals(ForgeRegistries.BLOCKS.getKey(block))) {
            return ItemStack.EMPTY;
        }
        Item item = block.asItem();
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void push() {
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetwork.sendToServer(InteractionBlockerPacket.current());
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
    protected int buttonAccent() {
        return ACCENT;
    }

    @Override
    protected int buttonHover() {
        return 0xFF3A2A2A;
    }

    private record Match(String key, Component label, ItemStack icon, InteractionRules.Rule template) {
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }
}
