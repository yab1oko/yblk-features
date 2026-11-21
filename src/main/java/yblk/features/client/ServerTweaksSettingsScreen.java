package yblk.features.client;

import net.minecraft.client.gui.GuiGraphics;
import yblk.features.network.ServerIconPacket;
import yblk.features.server.LegacyText;

import javax.imageio.ImageIO;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.Features;
import yblk.features.client.ui.BaseSettingsScreen;
import yblk.features.client.ui.Checks;
import yblk.features.client.ui.ScrollBar;
import yblk.features.client.ui.TextField;
import yblk.features.config.FeaturesConfig;
import yblk.features.network.ModNetwork;
import yblk.features.network.ServerActionPacket;
import yblk.features.network.ServerTweaksPacket;
import yblk.features.restrict.Restrictions;

import java.util.ArrayList;
import java.util.List;

/** Tabbed Server Tweaks menu. Text is kept until Save; checks and actions send immediately. */
public class ServerTweaksSettingsScreen extends BaseSettingsScreen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFF8E6BBF;
    private static final String[] TABS = {"general", "tab", "kits", "roles", "moderation", "inventory", "checks", "log"};
    private static final String[] PERMS = {"mute", "kick", "freeze", "inventory", "teleport", "vanish"};

    private final List<Hit> hits = new ArrayList<>();
    private int tab;
    private int scroll;
    private int panelL;
    private int panelR;
    private int panelTop;
    private int panelBottom;
    private boolean canEdit;
    private EditBox motd;
    private EditBox motd2;
    private EditBox join;
    private EditBox leave;
    private EditBox welcome;
    private EditBox sub;
    private EditBox rules;
    private EditBox header;
    private EditBox footer;
    private EditBox kitName;
    private EditBox kitRole;
    private EditBox starter;
    private EditBox playerName;
    private EditBox minutes;
    private EditBox reason;
    private final List<ItemStack> kitDraft = new ArrayList<>();
    private String kitNameText = "";
    private String motdText;
    private String motdText2;
    private String kitRoleText = "";
    private String starterText;
    private String selectedName = "";
    private boolean editInv;
    private int invScroll;
    private int clipTop;
    private int clipBottom;

    public ServerTweaksSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".server_tweaks"), parent);
    }

    @Override
    protected void init() {
        clearWidgets();
        canEdit = Restrictions.canEdit(minecraft != null ? minecraft.player : null);
        int panelW = Math.min(520, width - 16);
        panelL = (width - panelW) / 2;
        panelR = panelL + panelW;
        panelTop = 22;
        panelBottom = height - 32;
        int x = panelL + 8;
        int w = (panelR - panelL - 16) / 4;
        for (int i = 0; i < TABS.length; i++) {
            int col = i % 4;
            int row = i / 4;
            int index = i;
            addRenderableWidget(darkButton(x + col * w, panelTop + 4 + row * 16, w - 2, 14,
                    Component.translatable("screen." + Features.MODID + ".server_tweaks.tab." + TABS[i]), () -> select(index)));
        }
        int y = panelTop + 40;
        int fieldW = panelR - panelL - 24;
        if (tab == 0) {
            if (motdText == null) {
                String[] lines = FeaturesConfig.serverMotd.split("\n", 2);
                motdText = lines[0];
                motdText2 = lines.length > 1 ? lines[1] : "";
            }
            int field = Math.min(fieldW, 250);
            motd = line(panelL + 12, y, field, motdText, "server_tweaks.motd");
            motd2 = line(panelL + 12, y + 16, field, motdText2, "server_tweaks.motd2");
            motd.setResponder(v -> {
                motdText = v;
                markDirty();
            });
            motd2.setResponder(v -> {
                motdText2 = v;
                markDirty();
            });
            join = line(panelL + 12, y + 32, field, FeaturesConfig.serverJoin, "server_tweaks.join");
            leave = line(panelL + 12, y + 48, field, FeaturesConfig.serverLeave, "server_tweaks.leave");
            welcome = line(panelL + 12, y + 64, field, FeaturesConfig.serverWelcome, "server_tweaks.welcome");
            sub = line(panelL + 12, y + 80, field, FeaturesConfig.serverWelcomeSub, "server_tweaks.welcome_sub");
            rules = line(panelL + 12, y + 96, field, String.join(" | ", FeaturesConfig.serverRules), "server_tweaks.rules");
            addRenderableWidget(darkButton(panelL + 12, y + 116, 88, 16,
                    Component.translatable("screen." + Features.MODID + ".server_tweaks.icon"), this::pickIcon));
        } else if (tab == 1) {
            header = line(panelL + 12, y, fieldW, FeaturesConfig.serverTabHeader, "server_tweaks.header");
            footer = line(panelL + 12, y + 20, fieldW, FeaturesConfig.serverTabFooter, "server_tweaks.footer");
        } else if (tab == 2) {
            if (starterText == null) {
                starterText = FeaturesConfig.serverStarterKit;
            }
            kitName = line(panelL + 12, y, 120, kitNameText, "server_tweaks.kit");
            kitRole = line(panelL + 136, y, 80, kitRoleText, "server_tweaks.role");
            starter = line(panelL + 12, y + 20, fieldW, starterText, "server_tweaks.starter");
            kitName.setResponder(v -> {
                kitNameText = v == null ? "" : v;
                markDirty();
            });
            kitRole.setResponder(v -> {
                kitRoleText = v == null ? "" : v;
                markDirty();
            });
            starter.setResponder(v -> {
                starterText = v == null ? "" : v;
                markDirty();
            });
        } else if (tab == 4) {
            playerName = line(panelL + 12, y, 110, selectedName, "server_tweaks.player");
            minutes = line(panelL + 126, y, 40, "10", "server_tweaks.minutes");
            reason = line(panelL + 170, y, fieldW - 158, "", "server_tweaks.reason");
        } else if (tab == 5) {
            playerName = line(panelL + 8, panelBottom - 18, 100, selectedName, "server_tweaks.player");
            addRenderableWidget(darkButton(panelL + 112, panelBottom - 18, 56, 14,
                    Component.translatable("screen." + Features.MODID + ".server_tweaks.look"), () -> {
                        selectedName = value(playerName);
                        act("inv " + selectedName);
                    }));
        }
        addRenderableWidget(darkButton(width / 2 - 96, height - 26, 90, 18,
                Component.translatable("screen." + Features.MODID + ".reset_section"), this::resetTab));
        addRenderableWidget(darkButton(width / 2 + 6, height - 26, 90, 18,
                Component.translatable("gui.done"), this::saveAndClose));
    }

    private void select(int next) {
        commit();
        clearDirty();
        tab = next;
        scroll = 0;
        if (minecraft != null) {
            init(minecraft, width, height);
        }
    }

    private EditBox line(int x, int y, int w, String value, String hint) {
        EditBox box = new TextField(font, x, y, Math.max(28, w), 14, Component.empty());
        box.setMaxLength(256);
        box.setValue(value == null ? "" : value);
        box.setHint(Component.translatable("screen." + Features.MODID + "." + hint));
        box.setEditable(canEdit);
        box.setResponder(ignored -> markDirty());
        addRenderableWidget(box);
        return box;
    }

    @Override
    protected void saveUnsaved() {
        commit();
    }

    private void saveAndClose() {
        commit();
        clearDirty();
        onClose();
    }

    private void commit() {
        if (!canEdit) {
            return;
        }
        String rulesRaw = rules == null ? String.join(" | ", FeaturesConfig.serverRules) : rules.getValue();
        List<String> ruleLines = new ArrayList<>();
        if (!rulesRaw.isBlank()) {
            for (String part : rulesRaw.split("\\|")) {
                if (!part.isBlank()) {
                    ruleLines.add(part.trim());
                }
            }
        }
        FeaturesConfig.applyServerTweaks(
                FeaturesConfig.serverTweaksEnabled,
                joinedMotd(),
                text(join, FeaturesConfig.serverJoin),
                text(leave, FeaturesConfig.serverLeave),
                text(welcome, FeaturesConfig.serverWelcome),
                text(sub, FeaturesConfig.serverWelcomeSub),
                ruleLines,
                text(header, FeaturesConfig.serverTabHeader),
                text(footer, FeaturesConfig.serverTabFooter),
                text(starter, FeaturesConfig.serverStarterKit),
                FeaturesConfig.serverCheckAction,
                FeaturesConfig.serverCheckFly,
                FeaturesConfig.serverCheckSpeed,
                FeaturesConfig.serverCheckReach,
                FeaturesConfig.serverCheckFall,
                FeaturesConfig.serverCheckBreak,
                FeaturesConfig.serverCheckXray,
                FeaturesConfig.serverCheckDupe,
                FeaturesConfig.serverKits,
                FeaturesConfig.serverRoles,
                FeaturesConfig.serverMutes,
                FeaturesConfig.serverSeen,
                FeaturesConfig.serverLog,
                FeaturesConfig.serverHidePing,
                FeaturesConfig.serverSortTab,
                FeaturesConfig.serverRolePrefix,
                FeaturesConfig.joinCheckActions(),
                true);
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetwork.sendToServer(ServerTweaksPacket.current(false));
        }
    }

    private String joinedMotd() {
        String first = motdText == null ? "" : motdText.trim();
        String second = motdText2 == null ? "" : motdText2.trim();
        return second.isEmpty() ? first : first + "\n" + second;
    }

    private void pickIcon() {
        if (!canEdit) {
            return;
        }
        EventQueue.invokeLater(() -> {
            FileDialog dialog = new FileDialog((Frame) null, "PNG", FileDialog.LOAD);
            dialog.setFile("*.png");
            dialog.setVisible(true);
            String name = dialog.getFile();
            String dir = dialog.getDirectory();
            dialog.dispose();
            if (name == null || minecraft == null) {
                return;
            }
            File file = new File(dir, name);
            minecraft.execute(() -> uploadIcon(file));
        });
    }

    private void uploadIcon(File file) {
        try {
            BufferedImage source = ImageIO.read(file);
            if (source == null || minecraft == null || minecraft.getConnection() == null) {
                return;
            }
            BufferedImage scaled = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D draw = scaled.createGraphics();
            draw.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            draw.drawImage(source, 0, 0, 64, 64, null);
            draw.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(scaled, "png", out);
            ModNetwork.sendToServer(new ServerIconPacket(out.toByteArray()));
        } catch (Exception ignored) {
        }
    }

    private void resetTab() {
        if (tab == 0) {
            set(motd, "");
            set(motd2, "");
            set(join, "");
            set(leave, "");
            set(welcome, "");
            set(sub, "");
            set(rules, "");
        } else if (tab == 1) {
            set(header, "");
            set(footer, "");
            FeaturesConfig.serverHidePing = false;
            FeaturesConfig.serverSortTab = true;
            FeaturesConfig.serverRolePrefix = true;
        } else if (tab == 2) {
            set(starter, "");
        } else if (tab == 6) {
            FeaturesConfig.serverCheckFly = false;
            FeaturesConfig.serverCheckSpeed = false;
            FeaturesConfig.serverCheckReach = false;
            FeaturesConfig.serverCheckFall = false;
            FeaturesConfig.serverCheckBreak = false;
            FeaturesConfig.serverCheckXray = false;
            FeaturesConfig.serverCheckDupe = false;
            FeaturesConfig.serverCheckFlyAction = "log";
            FeaturesConfig.serverCheckSpeedAction = "log";
            FeaturesConfig.serverCheckReachAction = "log";
            FeaturesConfig.serverCheckFallAction = "log";
            FeaturesConfig.serverCheckBreakAction = "log";
            FeaturesConfig.serverCheckXrayAction = "log";
            FeaturesConfig.serverCheckDupeAction = "log";
            FeaturesConfig.serverCheckAction = "log";
            markDirty();
        }
    }

    private static void set(EditBox box, String value) {
        if (box != null) {
            box.setValue(value);
        }
    }

    private static String text(EditBox box, String fallback) {
        return box == null ? fallback : box.getValue();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xF0F0F0);
        graphics.fill(panelL, panelTop, panelR, panelBottom, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, panelL, panelTop, panelR - panelL, panelBottom - panelTop, ACCENT, 0xFF0A0A0E);
        int y = panelTop + 58;
        if (tab == 0) {
            check(graphics, panelL + 108, y + 116, FeaturesConfig.serverTweaksEnabled, "server_tweaks.enabled",
                    () -> FeaturesConfig.setServerTweaksEnabled(!FeaturesConfig.serverTweaksEnabled));
            String second = motdText2 == null ? "" : motdText2;
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".server_tweaks.preview"),
                    panelL + 274, y - 16, 0x9A9AA4, false);
            graphics.drawString(font, LegacyText.parse(motdText == null ? "" : motdText), panelL + 274, y, 0xFFFFFF, false);
            graphics.drawString(font, LegacyText.parse(second), panelL + 274, y + 12, 0xFFFFFF, false);
            tip(panelL + 12, y - 18, 250, 32, Component.translatable("screen." + Features.MODID + ".server_tweaks.motd.tip"));
        } else if (tab == 1) {
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".server_tweaks.tab_hint"),
                    panelL + 12, y + 28, 0x9A9AA4, false);
            check(graphics, panelL + 12, y + 48, FeaturesConfig.serverHidePing, "server_tweaks.hide_ping",
                    () -> FeaturesConfig.serverHidePing = !FeaturesConfig.serverHidePing);
            check(graphics, panelL + 12, y + 66, FeaturesConfig.serverSortTab, "server_tweaks.sort",
                    () -> FeaturesConfig.serverSortTab = !FeaturesConfig.serverSortTab);
            check(graphics, panelL + 12, y + 84, FeaturesConfig.serverRolePrefix, "server_tweaks.prefix",
                    () -> FeaturesConfig.serverRolePrefix = !FeaturesConfig.serverRolePrefix);
        } else if (tab == 2) {
            drawKitComposer(graphics, mouseX, mouseY, y + 44);
        } else if (tab == 3) {
            drawRoleList(graphics, mouseX, mouseY, y);
        } else if (tab == 4) {
            button(graphics, panelL + 12, y + 22, "server_tweaks.mute", () -> act("mute " + value(playerName) + " " + value(minutes) + " " + value(reason)));
            button(graphics, panelL + 70, y + 22, "server_tweaks.unmute", () -> act("unmute " + value(playerName)));
            button(graphics, panelL + 140, y + 22, "server_tweaks.kick", () -> act("kick " + value(playerName) + " " + value(reason)));
            button(graphics, panelL + 190, y + 22, "server_tweaks.ban", () -> act("ban " + value(playerName) + " " + value(minutes) + " " + value(reason)));
            button(graphics, panelL + 240, y + 22, "server_tweaks.freeze", () -> act("freeze " + value(playerName)));
            drawLines(graphics, FeaturesConfig.serverMutes, y + 44);
        } else if (tab == 5) {
            drawFaceColumn(graphics, mouseX, mouseY, y, panelBottom - 24, true);
            drawInventory(graphics, mouseX, mouseY, panelL + 160, y);
        } else if (tab == 6) {
            drawCheckAction(graphics, mouseX, mouseY, y, FeaturesConfig.serverCheckFly, "server_tweaks.fly", "fly",
                    () -> FeaturesConfig.serverCheckFly = !FeaturesConfig.serverCheckFly);
            drawCheckAction(graphics, mouseX, mouseY, y + 16, FeaturesConfig.serverCheckSpeed, "server_tweaks.speed", "speed",
                    () -> FeaturesConfig.serverCheckSpeed = !FeaturesConfig.serverCheckSpeed);
            drawCheckAction(graphics, mouseX, mouseY, y + 32, FeaturesConfig.serverCheckReach, "server_tweaks.reach", "reach",
                    () -> FeaturesConfig.serverCheckReach = !FeaturesConfig.serverCheckReach);
            drawCheckAction(graphics, mouseX, mouseY, y + 48, FeaturesConfig.serverCheckFall, "server_tweaks.fall", "fall",
                    () -> FeaturesConfig.serverCheckFall = !FeaturesConfig.serverCheckFall);
            drawCheckAction(graphics, mouseX, mouseY, y + 64, FeaturesConfig.serverCheckBreak, "server_tweaks.fast_break", "break",
                    () -> FeaturesConfig.serverCheckBreak = !FeaturesConfig.serverCheckBreak);
            drawCheckAction(graphics, mouseX, mouseY, y + 80, FeaturesConfig.serverCheckXray, "server_tweaks.xray", "xray",
                    () -> FeaturesConfig.serverCheckXray = !FeaturesConfig.serverCheckXray);
            drawCheckAction(graphics, mouseX, mouseY, y + 96, FeaturesConfig.serverCheckDupe, "server_tweaks.dupe", "dupe",
                    () -> FeaturesConfig.serverCheckDupe = !FeaturesConfig.serverCheckDupe);
            tip(panelL + 12, y, 200, 16, Component.translatable("screen." + Features.MODID + ".server_tweaks.checks.tip"));
        } else {
            drawLines(graphics, FeaturesConfig.serverLog, y);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawKitComposer(GuiGraphics graphics, int mouseX, int mouseY, int top) {
        int x = panelL + 12;
        for (int i = 0; i < 9; i++) {
            int sx = x + i * 20;
            ItemStack stack = i < kitDraft.size() ? kitDraft.get(i) : ItemStack.EMPTY;
            boolean hover = mouseX >= sx && mouseX < sx + 18 && mouseY >= top && mouseY < top + 18;
            graphics.fill(sx, top, sx + 18, top + 18, hover ? 0xFF2A2438 : 0xFF101014);
            int index = i;
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, sx + 1, top + 1);
                graphics.renderItemDecorations(font, stack, sx + 1, top + 1);
                if (canEdit) {
                    hits.add(new Hit(sx + 10, top - 2, 10, 10, () -> clearKitSlot(index)));
                }
                graphics.drawString(font, "x", sx + 11, top - 2, 0xC96060, false);
            } else {
                graphics.drawString(font, "+", sx + 5, top + 5, 0xFF8E6BBF, false);
            }
            if (canEdit) {
                hits.add(new Hit(sx, top, 18, 18, () -> editKitSlot(index)));
            }
        }
        press(graphics, x, top + 24, "server_tweaks.save_kit", mouseX, mouseY, this::saveDraft);
        press(graphics, x + 70, top + 24, "server_tweaks.give", mouseX, mouseY, () -> act("kit " + value(kitName)));
        press(graphics, x + 130, top + 24, "server_tweaks.from_inventory", mouseX, mouseY, this::draftFromInventory);
        int y = top + 44 - scroll;
        int bottom = panelBottom - 6;
        graphics.enableScissor(panelL + 4, top + 44, panelR - 4, bottom);
        for (String line : FeaturesConfig.serverKits) {
            if (y + 20 > top + 44 && y < bottom) {
                graphics.drawString(font, font.plainSubstrByWidth(line.split(" ")[0], 72), x, y + 4, 0xF0F0F0, false);
                drawKitIcons(graphics, line, x + 76, y);
                String stored = line;
                press(graphics, panelR - 78, y + 2, "server_tweaks.give", mouseX, mouseY, () -> act("kit " + stored.split(" ")[0]));
                if (canEdit) {
                    hits.add(new Hit(panelR - 22, y, 14, 16, () -> act("kit-del " + stored.split(" ")[0])));
                    hits.add(new Hit(x, y, 70, 18, () -> loadKit(stored)));
                }
                graphics.drawString(font, "x", panelR - 18, y + 4, 0xC96060, false);
            }
            y += 20;
        }
        graphics.disableScissor();
    }

    private void drawKitIcons(GuiGraphics graphics, String line, int x, int y) {
        String[] parts = line.split(" ");
        int drawn = 0;
        for (int i = 1; i + 1 < parts.length && drawn < 8; i += 2) {
            if (parts[i].startsWith("role:")) {
                break;
            }
            ItemStack stack = stackFrom(parts[i], parts[i + 1]);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, x + drawn * 18, y);
                drawn++;
            }
        }
    }

    private void drawRoleList(GuiGraphics graphics, int mouseX, int mouseY, int top) {
        List<PlayerInfo> infos = onlineInfo();
        int y = top - scroll;
        int bottom = panelBottom - 6;
        graphics.enableScissor(panelL + 4, top, panelR - 4, bottom);
        for (PlayerInfo info : infos) {
            if (y + 20 > top && y < bottom) {
                String name = info.getProfile().getName();
                boolean on = name.equals(selectedName);
                if (on) {
                    graphics.fill(panelL + 8, y, panelR - 10, y + 18, 0xFF241C30);
                }
                PlayerFaceRenderer.draw(graphics, info.getSkinLocation(), panelL + 10, y + 3, 12);
                graphics.drawString(font, font.plainSubstrByWidth(name, 90), panelL + 26, y + 5, 0xF0F0F0, false);
                graphics.drawString(font, font.plainSubstrByWidth(roleLabel(info), 52), panelL + 120, y + 5, 0xFFC8B0E0, false);
                String who = name;
                press(graphics, panelL + 176, y + 3, "server_tweaks.op", mouseX, mouseY, () -> chooseRole(who, "operator"));
                press(graphics, panelL + 214, y + 3, "server_tweaks.mod", mouseX, mouseY, () -> chooseRole(who, "moderator"));
                press(graphics, panelL + 258, y + 3, "server_tweaks.player_role", mouseX, mouseY, () -> chooseRole(who, "player"));
                press(graphics, panelL + 320, y + 3, "server_tweaks.tp", mouseX, mouseY, () -> {
                    selectedName = who;
                    act("tp " + who);
                });
                hits.add(new Hit(panelL + 8, y, 160, 18, () -> selectedName = who));
            }
            y += 20;
        }
        y += 6;
        String selectedRole = "";
        for (PlayerInfo info : infos) {
            if (info.getProfile().getName().equals(selectedName)) {
                selectedRole = roleLabel(info);
            }
        }
        if ("moderator".equals(selectedRole)) {
            for (String perm : PERMS) {
                if (y + 14 > top && y < bottom) {
                    boolean on = permOn(selectedName, perm);
                    Checks.box(graphics, panelL + 12, y, 10, on, ACCENT);
                    graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".server_tweaks.perm." + perm),
                            panelL + 26, y + 1, 0xC8C8D0, false);
                    String token = perm;
                    if (canEdit) {
                        hits.add(new Hit(panelL + 10, y, 140, 12, () -> act("perm " + selectedName + " " + token)));
                    }
                }
                y += 14;
            }
        }
        graphics.disableScissor();
        int content = infos.size() * 20 + ("moderator".equals(selectedRole) ? PERMS.length * 14 + 8 : 0);
        int max = Math.max(0, content - Math.max(1, bottom - top));
        scroll = Mth.clamp(scroll, 0, max);
        ScrollBar.draw(graphics, panelR, top, bottom, scroll, max, ACCENT, v -> scroll = v);
    }

    private void drawFaceColumn(GuiGraphics graphics, int mouseX, int mouseY, int top, int bottom, boolean requestInv) {
        List<PlayerInfo> infos = onlineInfo();
        int y = top - scroll;
        graphics.enableScissor(panelL + 4, top, panelL + 156, bottom);
        for (PlayerInfo info : infos) {
            if (y + 20 > top && y < bottom) {
                String name = info.getProfile().getName();
                boolean on = name.equals(selectedName);
                boolean hover = mouseX >= panelL + 8 && mouseX < panelL + 150 && mouseY >= y && mouseY < y + 18;
                if (on || hover) {
                    graphics.fill(panelL + 8, y, panelL + 150, y + 18, on ? 0xFF241C30 : 0xFF1E1A24);
                }
                PlayerFaceRenderer.draw(graphics, info.getSkinLocation(), panelL + 10, y + 3, 12);
                graphics.drawString(font, font.plainSubstrByWidth(name, 110), panelL + 26, y + 5, 0xF0F0F0, false);
                String who = name;
                hits.add(new Hit(panelL + 8, y, 142, 18, () -> choosePlayer(who, requestInv)));
            }
            y += 20;
        }
        graphics.disableScissor();
        int max = Math.max(0, infos.size() * 20 - Math.max(1, bottom - top));
        scroll = Mth.clamp(scroll, 0, max);
        ScrollBar.draw(graphics, panelL + 152, top, bottom, scroll, max, ACCENT, v -> scroll = v);
    }

    private void drawInventory(GuiGraphics graphics, int mouseX, int mouseY, int left, int top) {
        check(graphics, left, top, editInv, "server_tweaks.edit", () -> editInv = !editInv);
        if (FeaturesConfig.serverInvView.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".server_tweaks.pick_player"),
                    left, top + 20, 0x9A9AA4, false);
            return;
        }
        java.util.Map<String, ItemStack> items = parsedInv();
        String title = FeaturesConfig.serverInvView.get(0);
        graphics.drawString(font, font.plainSubstrByWidth(title, 180), left + 70, top, 0xF0F0F0, false);
        int grid = left;
        int y = top + 16 - invScroll;
        int bottom = panelBottom - 28;
        clipTop = top + 14;
        clipBottom = bottom;
        graphics.enableScissor(left - 4, top + 14, panelR - 4, bottom);
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".server_tweaks.armor"), grid, y, 0x9A9AA4, false);
        y += 12;
        for (int i = 0; i < 4; i++) {
            paintSlot(graphics, grid + i * 18, y, "armor " + (3 - i), items);
        }
        paintSlot(graphics, grid + 5 * 18, y, "offhand", items);
        y += 22;
        for (int row = 0; row < 4; row++) {
            int base = row == 3 ? 0 : 9 + row * 9;
            if (row == 3) {
                y += 4;
            }
            for (int col = 0; col < 9; col++) {
                paintSlot(graphics, grid + col * 18, y, Integer.toString(base + col), items);
            }
            y += 18;
        }
        y += 8;
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".server_tweaks.ender"), grid, y, 0x9A9AA4, false);
        y += 12;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                paintSlot(graphics, grid + col * 18, y, "ender " + (row * 9 + col), items);
            }
            y += 18;
        }
        y += 6;
        int cx = grid;
        boolean anyCurio = false;
        for (var entry : items.entrySet()) {
            if (!entry.getKey().startsWith("curios ") || entry.getValue().isEmpty()) {
                continue;
            }
            if (!anyCurio) {
                graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".server_tweaks.curios"), grid, y, 0x9A9AA4, false);
                y += 12;
                anyCurio = true;
            }
            graphics.renderItem(entry.getValue(), cx, y);
            cx += 18;
            if (cx > panelR - 28) {
                cx = grid;
                y += 18;
            }
        }
        graphics.disableScissor();
        int content = 12 + 22 + 4 + 18 * 4 + 8 + 12 + 18 * 3 + 40;
        int max = Math.max(0, content - Math.max(1, bottom - (top + 14)));
        invScroll = Mth.clamp(invScroll, 0, max);
    }

    private void paintSlot(GuiGraphics graphics, int x, int y, String key, java.util.Map<String, ItemStack> items) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF101014);
        ItemStack stack = items.getOrDefault(key, ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(font, stack, x + 1, y + 1);
        }
        boolean live = y + 18 > clipTop && y < clipBottom;
        if (live && canEdit && editInv && !stack.isEmpty()) {
            hits.add(new Hit(x + 10, y - 1, 9, 9, () -> act("set " + selectedName + " " + key.replace(" ", "") + " minecraft:stone 0")));
            graphics.drawString(font, "x", x + 11, y - 1, 0xC96060, false);
        }
        if (live && canEdit && editInv) {
            hits.add(new Hit(x, y, 18, 18, () -> editInvSlot(key)));
        }
    }

    private java.util.Map<String, ItemStack> parsedInv() {
        java.util.Map<String, ItemStack> items = new java.util.LinkedHashMap<>();
        List<String> lines = FeaturesConfig.serverInvView;
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(" ");
            if (parts.length < 3 || !parts[parts.length - 2].contains(":")) {
                continue;
            }
            String id = parts[parts.length - 2];
            String count = parts[parts.length - 1];
            String key = lines.get(i).substring(0, lines.get(i).length() - id.length() - count.length() - 2);
            items.put(key, stackFrom(id, count));
        }
        return items;
    }

    private boolean permOn(String name, String perm) {
        if (name.isBlank() || minecraft == null || minecraft.getConnection() == null) {
            return false;
        }
        for (var info : minecraft.getConnection().getOnlinePlayers()) {
            if (!name.equals(info.getProfile().getName())) {
                continue;
            }
            String prefix = info.getProfile().getId() + " moderator";
            for (String line : FeaturesConfig.serverRoles) {
                if (line.startsWith(prefix) && line.contains(" " + perm)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<PlayerInfo> onlineInfo() {
        List<PlayerInfo> list = new ArrayList<>();
        if (minecraft != null && minecraft.getConnection() != null) {
            list.addAll(minecraft.getConnection().getOnlinePlayers());
        }
        return list;
    }

    private String roleLabel(PlayerInfo info) {
        String prefix = info.getProfile().getId() + " ";
        for (String line : FeaturesConfig.serverRoles) {
            if (!line.startsWith(prefix)) {
                continue;
            }
            String[] parts = line.split(" ");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return "";
    }

    private void chooseRole(String name, String role) {
        selectedName = name;
        act("role " + name + " " + role);
    }

    private void choosePlayer(String name, boolean requestInv) {
        selectedName = name;
        if (playerName != null) {
            playerName.setValue(name);
            clearDirty();
        }
        if (requestInv) {
            act("inv " + name);
        }
    }

    private void editKitSlot(int index) {
        if (minecraft == null) {
            return;
        }
        ItemStack current = index < kitDraft.size() ? kitDraft.get(index) : ItemStack.EMPTY;
        minecraft.setScreen(new ItemPickScreen(this, current.isEmpty() ? 1 : current.getCount(), ACCENT, stack -> {
            while (kitDraft.size() <= index) {
                kitDraft.add(ItemStack.EMPTY);
            }
            kitDraft.set(index, stack.copy());
        }));
    }

    private void clearKitSlot(int index) {
        if (index >= 0 && index < kitDraft.size()) {
            kitDraft.set(index, ItemStack.EMPTY);
        }
    }

    private void saveDraft() {
        String name = value(kitName).replace(' ', '_');
        if (name.isEmpty()) {
            return;
        }
        String role = value(kitRole);
        StringBuilder line = new StringBuilder("kit-set ").append(name).append(' ');
        line.append(role.isEmpty() ? "-" : role.replace(' ', '_'));
        int added = 0;
        for (ItemStack stack : kitDraft) {
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null) {
                continue;
            }
            line.append(' ').append(id).append(' ').append(stack.getCount());
            added++;
        }
        if (added > 0) {
            act(line.toString());
        }
    }

    private void draftFromInventory() {
        kitDraft.clear();
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        var inventory = minecraft.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && kitDraft.size() < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                kitDraft.add(stack.copy());
            }
        }
    }

    private void loadKit(String stored) {
        String[] parts = stored.split(" ");
        if (parts.length == 0) {
            return;
        }
        if (kitName != null) {
            kitName.setValue(parts[0]);
        }
        kitDraft.clear();
        String role = "";
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith("role:")) {
                role = parts[i].substring(5);
                continue;
            }
            if (i + 1 >= parts.length) {
                break;
            }
            ItemStack stack = stackFrom(parts[i], parts[i + 1]);
            i++;
            if (!stack.isEmpty()) {
                kitDraft.add(stack);
            }
        }
        if (kitRole != null) {
            kitRole.setValue(role);
        }
        clearDirty();
    }

    private void editInvSlot(String key) {
        if (minecraft == null || selectedName.isEmpty()) {
            return;
        }
        String slot = key.replace(" ", "");
        minecraft.setScreen(new ItemPickScreen(this, 1, ACCENT, stack -> {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null) {
                act("set " + selectedName + " " + slot + " " + id + " " + stack.getCount());
            }
        }));
    }

    private static ItemStack stackFrom(String id, String countRaw) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null || !ForgeRegistries.ITEMS.containsKey(parsed)) {
            return ItemStack.EMPTY;
        }
        int count = 1;
        try {
            count = Integer.parseInt(countRaw);
        } catch (NumberFormatException ignored) {
            count = 1;
        }
        return new ItemStack(ForgeRegistries.ITEMS.getValue(parsed), Math.max(1, Math.min(64, count)));
    }

    private void drawCheckAction(GuiGraphics graphics, int mouseX, int mouseY, int y, boolean on, String key,
                                 String check, Runnable toggle) {
        check(graphics, panelL + 12, y, on, key, toggle);
        String action = FeaturesConfig.checkActionFor(check);
        String labelKey = "screen." + Features.MODID + ".server_tweaks.action." + action;
        int w = font.width(Component.translatable(labelKey)) + 8;
        press(graphics, panelR - 12 - w, y, "server_tweaks.action." + action, mouseX, mouseY, () -> {
            FeaturesConfig.cycleCheckAction(check);
            commit();
            clearDirty();
        });
    }

    private void press(GuiGraphics graphics, int x, int y, String key, int mouseX, int mouseY, Runnable action) {
        String label = Component.translatable("screen." + Features.MODID + "." + key).getString();
        int w = Math.max(18, font.width(label) + 8);
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 12;
        graphics.fill(x, y, x + w, y + 12, hover ? 0xFF3A3050 : 0xFF1A1A22);
        graphics.drawString(font, label, x + 4, y + 2, hover ? 0xFFFFFFFF : 0xF0F0F0, false);
        tipKey(x, y, w, 12, "screen." + Features.MODID + "." + key);
        if (canEdit) {
            hits.add(new Hit(x, y, w, 12, action));
        }
    }

    private void drawLines(GuiGraphics graphics, List<String> lines, int top) {
        int y = top - scroll;
        int listBottom = panelBottom - 6;
        graphics.enableScissor(panelL + 4, top, panelR - 4, listBottom);
        for (String line : lines) {
            if (y + 12 > top && y < listBottom) {
                graphics.drawString(font, font.plainSubstrByWidth(line, panelR - panelL - 28), panelL + 12, y, 0xC8C8D0, false);
            }
            y += 12;
        }
        graphics.disableScissor();
        int max = Math.max(0, lines.size() * 12 - Math.max(1, listBottom - top));
        scroll = Mth.clamp(scroll, 0, max);
        ScrollBar.draw(graphics, panelR, top, listBottom, scroll, max, ACCENT, v -> scroll = v);
    }

    private void check(GuiGraphics graphics, int x, int y, boolean on, String key, Runnable action) {
        Checks.box(graphics, x, y, 10, on, ACCENT);
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + "." + key), x + 14, y + 1, 0xF0F0F0, false);
        tipKey(x, y, 180, 12, "screen." + Features.MODID + "." + key);
        if (canEdit) {
            hits.add(new Hit(x, y, 160, 12, () -> {
                action.run();
                commit();
                clearDirty();
            }));
        }
    }

    private void button(GuiGraphics graphics, int x, int y, String key, Runnable action) {
        String label = Component.translatable("screen." + Features.MODID + "." + key).getString();
        int w = font.width(label) + 8;
        graphics.fill(x, y, x + w, y + 12, 0xFF1A1A22);
        graphics.drawString(font, label, x + 4, y + 2, 0xF0F0F0, false);
        tipKey(x, y, w, 12, "screen." + Features.MODID + "." + key);
        if (canEdit) {
            hits.add(new Hit(x, y, w, 12, action));
        }
    }

    private void small(GuiGraphics graphics, int x, int y, String label, Runnable action) {
        int w = font.width(label) + 6;
        graphics.fill(x, y, x + w, y + 12, 0xFF1A1A22);
        graphics.drawString(font, label, x + 3, y + 2, 0xF0F0F0, false);
        if (canEdit) {
            hits.add(new Hit(x, y, w, 12, action));
        }
    }

    private void act(String line) {
        if (canEdit && minecraft != null && minecraft.getConnection() != null) {
            ModNetwork.sendToServer(new ServerActionPacket(line.trim()));
        }
    }

    private static String value(EditBox box) {
        return box == null ? "" : box.getValue().trim();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (asking) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0) {
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
        if (tab == 5 && mouseX >= panelL + 160) {
            invScroll = Math.max(0, invScroll - (int) (delta * 12));
            return true;
        }
        scroll = Math.max(0, scroll - (int) (delta * 12));
        return true;
    }

    @Override
    protected int buttonAccent() {
        return ACCENT;
    }

    @Override
    protected int buttonHover() {
        return 0xFF2A2438;
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }
}
