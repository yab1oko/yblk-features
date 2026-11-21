package yblk.features.client;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import yblk.features.Features;
import yblk.features.client.ui.BaseSettingsScreen;
import yblk.features.client.ui.DarkButton;
import yblk.features.client.ui.HintLines;
import yblk.features.client.ui.ScrollBar;
import yblk.features.client.ui.TextField;
import yblk.features.config.FeaturesConfig;
import yblk.features.network.CommandBlockerPacket;
import yblk.features.network.ModNetwork;
import yblk.features.restrict.CommandRules;
import yblk.features.restrict.Restrictions;

import java.util.ArrayList;
import java.util.List;

public class CommandBlockerSettingsScreen extends BaseSettingsScreen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFFC96060;
    private static final int ROW = 16;

    private final List<Hit> hits = new ArrayList<>();
    private EditBox messageBox;
    private EditBox commandBox;
    private DarkButton roleButton;
    private int audience;
    private boolean canEdit;
    private int panelL;
    private int panelR;
    private int panelTop;
    private int panelBottom;
    private int listTop;
    private int listBottom;
    private int scroll;
    private int maxScroll;

    public CommandBlockerSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".command_blocker"), parent);
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
        listTop = panelTop + 108;
        listBottom = panelBottom - 6;

        messageBox = new TextField(font, panelL + 12, panelTop + 48, panelR - panelL - 24, 14, Component.empty());
        messageBox.setMaxLength(256);
        messageBox.setValue(FeaturesConfig.commandDenyMessage);
        messageBox.setHint(Component.translatable("screen." + Features.MODID + ".command_blocker.message"));
        messageBox.setResponder(value -> {
            if (canEdit && !value.equals(FeaturesConfig.commandDenyMessage)) {
                FeaturesConfig.setCommandDenyMessage(value);
                push();
            }
        });
        messageBox.setEditable(canEdit);
        addRenderableWidget(messageBox);

        int roleW = 24 + Math.max(
                font.width(roleText(0)),
                Math.max(font.width(roleText(1)), font.width(roleText(2))));
        int addW = font.width(Component.translatable("screen." + Features.MODID + ".command_blocker.add")) + 12;
        int rowY = panelTop + 80;
        roleButton = darkButton(panelL + 12, rowY, roleW, 16, roleText(audience), () -> {
            if (!canEdit) {
                return;
            }
            audience = (audience + 1) % 3;
            roleButton.setMessage(roleText(audience));
        });
        roleButton.active = canEdit;
        addRenderableWidget(roleButton);

        int fieldX = panelL + 12 + roleW + 4;
        int addX = panelR - 12 - addW;
        commandBox = new TextField(font, fieldX, rowY, Math.max(40, addX - 4 - fieldX), 16, Component.empty());
        commandBox.setMaxLength(200);
        commandBox.setHint(Component.translatable("screen." + Features.MODID + ".command_blocker.command"));
        commandBox.setEditable(canEdit);
        addRenderableWidget(commandBox);

        DarkButton add = darkButton(addX, rowY, addW, 16,
                Component.translatable("screen." + Features.MODID + ".command_blocker.add"), this::addRule);
        add.active = canEdit;
        addRenderableWidget(add);

        addRenderableWidget(darkButton(width / 2 - 45, height - 6 - btnH, 90, btnH,
                Component.translatable("gui.done"), this::onClose));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        List<CommandRules.Rule> rules = CommandRules.compile();
        List<String> suggestions = commandSuggestions();
        int content = 14 + Math.max(1, suggestions.size()) * ROW + 14 + Math.max(1, rules.size()) * ROW;
        maxScroll = Math.max(0, content - Math.max(1, listBottom - listTop));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xF0F0F0);
        graphics.fill(panelL, panelTop, panelR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelTop, panelR - panelL, panelBottom - panelTop, ACCENT, 0xFF0A0A0E);

        int x = panelL + 12;
        String hint = canEdit
                ? "screen." + Features.MODID + ".command_blocker.hint"
                : "screen." + Features.MODID + ".command_blocker.locked";
        HintLines.draw(graphics, font, Component.translatable(hint), x, panelTop + 4, panelR - panelL - 24,
                canEdit ? 0x808088 : 0xE05050, 3);
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".command_blocker.message"),
                x, panelTop + 36, 0xC8C8D0, false);
        tipKey(x, panelTop + 34, panelR - 12 - x, 14, "screen." + Features.MODID + ".command_blocker.message");

        graphics.enableScissor(panelL + 4, listTop, panelR - 4, listBottom);
        int y = listTop - scroll;
        int rowW = panelR - 28 - x;
        if (y + 12 > listTop && y < listBottom) {
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".command_blocker.blocked"),
                    x, y + 2, ACCENT, false);
        }
        y += 12;
        if (rules.isEmpty()) {
            y = muted(graphics, x, y, "screen." + Features.MODID + ".command_blocker.empty");
        }
        for (CommandRules.Rule rule : rules) {
            if (y + ROW > listTop && y < listBottom) {
                String who = audienceText(rule);
                String line = who + "  " + String.join(" ", rule.tokens());
                String cut = font.plainSubstrByWidth(line, rowW - 16);
                graphics.drawString(font, cut, x, y + 4, 0xF0F0F0, false);
                tipKey(x, y, rowW, ROW, "screen." + Features.MODID + ".command_blocker.blocked");
                graphics.drawString(font, "x", x + rowW - 8, y + 4, 0xC96060, false);
                if (y >= listTop && y + ROW <= listBottom) {
                    hits.add(new Hit(x, y, rowW, ROW, () -> removeRule(rule.stored())));
                }
            }
            y += ROW;
        }
        if (y + 12 > listTop && y < listBottom) {
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".command_blocker.suggestions"),
                    x, y + 2, ACCENT, false);
        }
        y += 12;
        if (suggestions.isEmpty()) {
            y = muted(graphics, x, y, "screen." + Features.MODID + ".command_blocker.none");
        }
        for (String suggestion : suggestions) {
            if (y + ROW > listTop && y < listBottom) {
                boolean hover = mouseX >= x && mouseX < x + rowW && mouseY >= y && mouseY < y + ROW;
                if (hover) {
                    graphics.fill(x, y, x + rowW, y + ROW, 0xFF2A2020);
                }
                graphics.drawString(font, font.plainSubstrByWidth(suggestion, rowW - 4), x + 2, y + 4, 0xFFE8E0E0, false);
                tipKey(x, y, rowW, ROW, "screen." + Features.MODID + ".command_blocker.suggestions");
                if (canEdit && y >= listTop && y + ROW <= listBottom) {
                    String chosen = suggestion;
                    hits.add(new Hit(x, y, rowW, ROW, () -> commandBox.setValue(chosen)));
                }
            }
            y += ROW;
        }
        graphics.disableScissor();
        ScrollBar.draw(graphics, panelR, listTop, listBottom, scroll, maxScroll, ACCENT, v -> scroll = v);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int muted(GuiGraphics graphics, int x, int y, String key) {
        if (y + ROW > listTop && y < listBottom) {
            graphics.drawString(font, Component.translatable(key), x, y + 4, 0x6A6A72, false);
        }
        return y + ROW;
    }

    private List<String> commandSuggestions() {
        List<String> out = new ArrayList<>();
        if (minecraft == null || minecraft.getConnection() == null || commandBox == null) {
            return out;
        }
        String typed = commandBox.getValue().trim();
        if (typed.startsWith("/")) {
            typed = typed.substring(1);
        }
        var dispatcher = minecraft.getConnection().getCommands();
        if (!typed.contains(" ")) {
            for (var node : dispatcher.getRoot().getChildren()) {
                if (typed.isEmpty() || node.getName().startsWith(typed)) {
                    out.add(node.getName());
                }
            }
            out.sort(String::compareTo);
            return out;
        }
        try {
            var parsed = dispatcher.parse(new StringReader(typed), minecraft.getConnection().getSuggestionsProvider());
            var future = dispatcher.getCompletionSuggestions(parsed);
            if (!future.isDone()) {
                return out;
            }
            for (var suggestion : future.join().getList()) {
                int start = Math.min(Math.max(0, suggestion.getRange().getStart()), typed.length());
                out.add(typed.substring(0, start) + suggestion.getText());
            }
        } catch (Exception ignored) {
            return out;
        }
        return out;
    }

    private void addRule() {
        if (!canEdit) {
            return;
        }
        String stored = CommandRules.normalize(commandBox.getValue(), audience);
        if (stored == null) {
            return;
        }
        List<String> next = new ArrayList<>();
        for (String raw : FeaturesConfig.commandRules) {
            CommandRules.Rule rule = CommandRules.parse(raw);
            if (rule == null) {
                continue;
            }
            if (rule.stored().equalsIgnoreCase(stored)) {
                commandBox.setValue("");
                return;
            }
            next.add(rule.stored());
        }
        next.add(stored);
        FeaturesConfig.setCommandRules(next);
        commandBox.setValue("");
        push();
    }

    private void removeRule(String stored) {
        if (!canEdit) {
            return;
        }
        List<String> next = new ArrayList<>();
        for (String raw : FeaturesConfig.commandRules) {
            CommandRules.Rule rule = CommandRules.parse(raw);
            if (rule != null && !rule.stored().equalsIgnoreCase(stored)) {
                next.add(rule.stored());
            }
        }
        FeaturesConfig.setCommandRules(next);
        push();
    }

    private void push() {
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetwork.sendToServer(CommandBlockerPacket.current());
        }
    }

    private Component roleText(int mode) {
        String key = switch (mode) {
            case 1 -> "screen." + Features.MODID + ".command_blocker.role.mods";
            case 2 -> "screen." + Features.MODID + ".command_blocker.role.all";
            default -> "screen." + Features.MODID + ".command_blocker.role.players";
        };
        return Component.translatable(key);
    }

    private String audienceText(CommandRules.Rule rule) {
        if (rule.player() && rule.moderator() && rule.operator()) {
            return roleText(2).getString();
        }
        if (rule.player() && rule.moderator() && !rule.operator()) {
            return roleText(1).getString();
        }
        if (rule.player() && !rule.moderator() && !rule.operator()) {
            return roleText(0).getString();
        }
        StringBuilder sb = new StringBuilder();
        if (rule.player()) {
            sb.append('p');
        }
        if (rule.moderator()) {
            sb.append('m');
        }
        if (rule.operator()) {
            sb.append('o');
        }
        return sb.toString();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseY >= listTop && mouseY < listBottom) {
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
        if (mouseY >= listTop && mouseY < listBottom) {
            scroll = Mth.clamp(scroll - (int) (delta * ROW), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && commandBox.isFocused()) {
            addRule();
            return true;
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
}
