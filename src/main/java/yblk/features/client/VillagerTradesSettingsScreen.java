package yblk.features.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.Features;
import yblk.features.client.ui.BaseSettingsScreen;
import yblk.features.client.ui.Checks;
import yblk.features.client.ui.ScrollBar;
import yblk.features.config.FeaturesConfig;
import yblk.features.mobs.TradeCustoms;
import yblk.features.mobs.TargetMatch;
import yblk.features.network.ModNetwork;
import yblk.features.network.VillagerTradesPacket;
import yblk.features.restrict.Restrictions;

import java.util.ArrayList;
import java.util.List;

/** A trade window: pick a profession, forbid offers, or add one. */
public class VillagerTradesSettingsScreen extends BaseSettingsScreen {
    private static final int PANEL = 0xFF16161C;
    private static final int ACCENT = 0xFF3FA86A;
    private static final int ROW = 22;
    private static final int CHECK = 9;

    private final List<Job> jobs = new ArrayList<>();
    private final List<Shown> offers = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private int selected;
    private int jobScroll;
    private int offerScroll;
    private int listLeft;
    private int listTop;
    private int listWidth;
    private int listHeight;
    private int previewLeft;
    private int previewRight;
    private int offerTop;
    private LivingEntity preview;
    private boolean canEdit;
    private int slot;
    private ItemStack costA = ItemStack.EMPTY;
    private ItemStack costB = ItemStack.EMPTY;
    private ItemStack result = ItemStack.EMPTY;
    public VillagerTradesSettingsScreen(Screen parent) {
        super(Component.translatable("screen." + Features.MODID + ".villager_trades"), parent);
    }

    @Override
    protected void init() {
        clearWidgets();
        canEdit = Restrictions.canEdit(minecraft != null ? minecraft.player : null);
        jobs.clear();
        jobs.add(new Job(TradeCustoms.WANDERING, Component.translatable("entity.minecraft.wandering_trader"), null));
        for (VillagerProfession profession : ForgeRegistries.VILLAGER_PROFESSIONS) {
            ResourceLocation id = ForgeRegistries.VILLAGER_PROFESSIONS.getKey(profession);
            if (id == null || "none".equals(id.getPath()) || "nitwit".equals(id.getPath())) {
                continue;
            }
            String key = "minecraft".equals(id.getNamespace())
                    ? "entity.minecraft.villager." + id.getPath()
                    : id.toString();
            Component name = Component.translatable(key);
            if (name.getString().equals(key)) {
                name = Component.literal(id.getPath());
            }
            jobs.add(new Job(id.toString(), name, profession));
        }

        int contentTop = 32;
        int contentBottom = height - 32;
        listLeft = 12;
        listTop = contentTop;
        listWidth = Math.min(150, width / 2 - 28);
        listHeight = contentBottom - listTop;
        previewLeft = listLeft + listWidth + 10;
        previewRight = width - 12;
        offerTop = listTop + 108;

        addRenderableWidget(darkButton(width / 2 - 45, height - 26, 90, 18,
                Component.translatable("gui.done"), this::onClose));
        rebuild();
    }

    private void rebuild() {
        discard();
        offers.clear();
        if (minecraft == null || minecraft.level == null || jobs.isEmpty()) {
            return;
        }
        Job job = jobs.get(Mth.clamp(selected, 0, jobs.size() - 1));
        RandomSource random = minecraft.level.random;
        if (job.profession == null) {
            Entity created = net.minecraft.world.entity.EntityType.WANDERING_TRADER.create(minecraft.level);
            if (created instanceof WanderingTrader trader) {
                preview = trader;
                fill(VillagerTrades.WANDERING_TRADER_TRADES, trader, random);
            }
        } else {
            Entity created = net.minecraft.world.entity.EntityType.VILLAGER.create(minecraft.level);
            if (created instanceof Villager villager) {
                villager.setVillagerData(villager.getVillagerData().setProfession(job.profession));
                preview = villager;
                var map = VillagerTrades.TRADES.get(job.profession);
                if (map != null) {
                    fill(map, villager, random);
                }
            }
        }
        if (preview != null) {
            preview.yBodyRot = 200f;
            preview.setYRot(200f);
            preview.yHeadRot = 200f;
        }
        for (TradeCustoms.Custom custom : TradeCustoms.forWho(FeaturesConfig.villagerCustoms, job.id)) {
            int cost = custom.costA().getCount();
            offers.add(new Shown(custom.offer(), custom.stored(), true, cost, cost));
        }
    }

    private void fill(it.unimi.dsi.fastutil.ints.Int2ObjectMap<VillagerTrades.ItemListing[]> map, LivingEntity entity, RandomSource random) {
        for (int level : map.keySet()) {
            VillagerTrades.ItemListing[] listings = map.get(level);
            if (listings == null) {
                continue;
            }
            for (VillagerTrades.ItemListing listing : listings) {
                addListing(listing, entity, random);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xF0F0F0);
        graphics.fill(listLeft - 2, listTop - 2, listLeft + listWidth + 2, listTop + listHeight + 2, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, listLeft - 2, listTop - 2, listWidth + 4, listHeight + 4, ACCENT, 0xFF0A0A0E);
        int visible = Math.max(1, listHeight / ROW);
        jobScroll = Mth.clamp(jobScroll, 0, Math.max(0, jobs.size() - visible));
        for (int row = 0; row < visible; row++) {
            int index = jobScroll + row;
            if (index >= jobs.size()) {
                break;
            }
            int y = listTop + row * ROW;
            if (index == selected) {
                graphics.fill(listLeft, y, listLeft + listWidth, y + ROW, 0xFF163028);
            }
            graphics.drawString(font, font.plainSubstrByWidth(jobs.get(index).name.getString(), listWidth - 8),
                    listLeft + 4, y + 7, index == selected ? 0xF0F0F0 : 0xC8C8D0, false);
        }
        ScrollBar.draw(graphics, listLeft + listWidth + 8, listTop, listTop + listHeight, jobScroll,
                Math.max(0, jobs.size() - visible), ACCENT, v -> jobScroll = v);

        graphics.fill(previewLeft, listTop - 2, previewRight, listTop + listHeight + 2, PANEL);
        FeaturesScreen.drawGradientBorder(graphics, previewLeft, listTop - 2, previewRight - previewLeft, listHeight + 4, ACCENT, 0xFF0A0A0E);
        if (!jobs.isEmpty()) {
            Job job = jobs.get(selected);
            graphics.drawString(font, job.name, previewLeft + 6, listTop + 4, 0xF0F0F0, false);
            boolean blocked = blocked(job);
            Checks.box(graphics, previewLeft + 6, listTop + 18, CHECK, blocked, ACCENT);
            graphics.drawString(font, Component.translatable(job.profession == null
                            ? "screen." + Features.MODID + ".villager_trades.wandering"
                            : "screen." + Features.MODID + ".villager_trades.block_job"),
                    previewLeft + 18, listTop + 18, 0xFFD0D0D8, false);
            tipKey(previewLeft + 4, listTop + 16, 180, 14, job.profession == null
                    ? "screen." + Features.MODID + ".villager_trades.wandering"
                    : "screen." + Features.MODID + ".villager_trades.block_job");
            if (canEdit) {
                hits.add(new Hit(previewLeft + 4, listTop + 16, 160, 14, () -> toggleJob(job)));
            }
            Checks.box(graphics, previewLeft + 6, listTop + 34, CHECK, FeaturesConfig.villagerFreezePrices, ACCENT);
            graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".villager_trades.freeze"),
                    previewLeft + 18, listTop + 34, 0xFFD0D0D8, false);
            tipKey(previewLeft + 4, listTop + 32, 180, 14, "screen." + Features.MODID + ".villager_trades.freeze");
            if (canEdit) {
                hits.add(new Hit(previewLeft + 4, listTop + 32, 160, 14, () -> {
                    FeaturesConfig.setVillagerFreezePrices(!FeaturesConfig.villagerFreezePrices);
                    push();
                }));
            }
            drawSlots(graphics, mouseX, mouseY);
            drawOffers(graphics);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (preview != null) {
            int px = previewRight - 28;
            int py = listTop + 52;
            graphics.enableScissor(previewRight - 64, listTop + 2, previewRight - 4, listTop + 58);
            graphics.pose().pushPose();
            graphics.pose().translate(0f, 0f, 200f);
            RenderSystem.enableDepthTest();
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, px, py, 24, px - mouseX, py - 28 - mouseY, preview);
            graphics.pose().popPose();
            graphics.disableScissor();
        }
    }

    private void drawSlots(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = previewLeft + 6;
        int y = listTop + 62;
        drawSlot(graphics, x, y, costA, 0);
        drawCount(graphics, x + 18, y, 0);
        drawSlot(graphics, x + 36, y, costB, 1);
        drawCount(graphics, x + 54, y, 1);
        graphics.drawString(font, ">", x + 68, y + 5, 0xFF3FA86A, false);
        drawSlot(graphics, x + 80, y, result, 2);
        drawCount(graphics, x + 98, y, 2);
        boolean ready = canEdit && !costA.isEmpty() && !result.isEmpty();
        hits.add(new Hit(x + 114, y, 52, 18, this::addCustom));
        graphics.fill(x + 114, y, x + 166, y + 16, ready ? 0xFF163028 : 0xFF121216);
        graphics.drawString(font, Component.translatable("screen." + Features.MODID + ".villager_trades.add"),
                x + 122, y + 4, ready ? 0xF0F0F0 : 0xFF6A6A72, false);
    }

    private void drawCount(GuiGraphics graphics, int x, int y, int which) {
        graphics.drawString(font, "+", x, y, 0xFF3FA86A, false);
        graphics.drawString(font, "-", x, y + 9, 0xFFC96060, false);
        if (canEdit) {
            hits.add(new Hit(x, y, 10, 9, () -> changeCount(which, 1)));
            hits.add(new Hit(x, y + 9, 10, 9, () -> changeCount(which, -1)));
        }
    }

    private void changeCount(int which, int delta) {
        ItemStack stack = which == 0 ? costA : which == 1 ? costB : result;
        if (stack.isEmpty()) {
            return;
        }
        stack.setCount(Mth.clamp(stack.getCount() + delta, 1, stack.getMaxStackSize()));
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, ItemStack stack, int which) {
        boolean on = slot == which;
        graphics.fill(x, y, x + 18, y + 18, on ? 0xFF163028 : 0xFF101014);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF0C0C10);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(font, stack, x + 1, y + 1);
            graphics.drawString(font, "x", x + 12, y - 1, 0xC96060, false);
        }
        if (canEdit && !stack.isEmpty()) {
            hits.add(new Hit(x + 11, y - 2, 10, 10, () -> clearSlot(which)));
        }
        if (canEdit) {
            hits.add(new Hit(x, y, 18, 18, () -> openPick(which)));
        }
    }

    private void openPick(int which) {
        slot = which;
        ItemStack current = which == 0 ? costA : which == 1 ? costB : result;
        int start = current.isEmpty() ? 1 : current.getCount();
        minecraft.setScreen(new ItemPickScreen(this, start, ACCENT, stack -> {
            if (which == 0) {
                costA = stack;
            } else if (which == 1) {
                costB = stack;
            } else {
                result = stack;
            }
        }));
    }

    private void clearSlot(int which) {
        if (which == 0) {
            costA = ItemStack.EMPTY;
        } else if (which == 1) {
            costB = ItemStack.EMPTY;
        } else {
            result = ItemStack.EMPTY;
        }
    }

    private void drawOffers(GuiGraphics graphics) {
        int y0 = offerTop;
        int y1 = listTop + listHeight - 22;
        int view = Math.max(1, y1 - y0);
        offerScroll = Mth.clamp(offerScroll, 0, Math.max(0, offers.size() * 28 - view));
        graphics.enableScissor(previewLeft + 2, y0, previewRight - 4, y1);
        int y = y0 - offerScroll;
        int x = previewLeft + 6;
        for (Shown shown : offers) {
            if (y + 28 > y0 && y < y1) {
                boolean allowed = shown.custom || !forbidden(shown.offer.getResult());
                drawTrade(graphics, x, y, shown, allowed);
                if (canEdit && !shown.custom && y >= y0 && y + 28 <= y1) {
                    Shown picked = shown;
                    hits.add(new Hit(x, y, previewRight - x - 14, 28, () -> toggleOffer(picked)));
                }
            }
            y += 28;
        }
        graphics.disableScissor();
        ScrollBar.draw(graphics, previewRight, y0, y1, offerScroll,
                Math.max(0, offers.size() * 28 - view), ACCENT, v -> offerScroll = v);
    }

    private void drawTrade(GuiGraphics graphics, int x, int y, Shown shown, boolean allowed) {
        MerchantOffer offer = shown.offer;
        int min = shown.costMin;
        int max = shown.costMax;
        int color = allowed ? 0x00000000 : 0x66301010;
        if (!allowed) {
            graphics.fill(x, y, x + 120, y + 17, color);
        }
        int rowW = Math.max(80, previewRight - x - 14);
        graphics.renderItem(offer.getCostA(), x, y + 1);
        if (min == max) {
            graphics.renderItemDecorations(font, offer.getCostA(), x, y + 1);
        } else {
            graphics.drawString(font, min + "-" + max, x, y + 16, 0xFF3FA86A, false);
        }
        int next = x + 20;
        if (!offer.getCostB().isEmpty()) {
            graphics.renderItem(offer.getCostB(), next, y + 1);
            graphics.renderItemDecorations(font, offer.getCostB(), next, y + 1);
            next += 20;
        }
        graphics.drawString(font, ">", next, y + 6, allowed ? 0xFF3FA86A : 0xFF806060, false);
        next += 10;
        graphics.renderItem(offer.getResult(), next, y + 1);
        graphics.renderItemDecorations(font, offer.getResult(), next, y + 1);
        next += 20;
        String label = enchantLabel(offer.getResult());
        if (label.isEmpty()) {
            label = offer.getResult().getHoverName().getString();
        }
        graphics.drawString(font, font.plainSubstrByWidth(label, Math.max(16, rowW - (next - x) - 16)),
                next, y + 6, allowed ? 0xF0F0F0 : 0xFF806060, false);
        if (shown.custom) {
            graphics.drawString(font, "x", x + rowW - 8, y + 6, 0xC96060, false);
            if (canEdit) {
                String stored = shown.stored;
                hits.add(new Hit(x + rowW - 12, y, 14, 20, () -> removeCustom(stored)));
            }
        } else {
            Checks.box(graphics, x + rowW - 10, y + 6, CHECK, allowed, ACCENT);
        }
    }

    private void removeCustom(String stored) {
        FeaturesConfig.setVillagerCustoms(TradeCustoms.remove(FeaturesConfig.villagerCustoms, stored));
        rebuild();
        push();
    }

    private boolean blocked(Job job) {
        if (job.profession == null) {
            return FeaturesConfig.villagerBlockWandering || FeaturesConfig.villagerBlockAll;
        }
        if (FeaturesConfig.villagerBlockAll) {
            return true;
        }
        for (String raw : FeaturesConfig.villagerProfessions) {
            if (TargetMatch.id(ResourceLocation.tryParse(job.id), TargetMatch.entity(raw))) {
                return true;
            }
        }
        return false;
    }

    private boolean forbidden(ItemStack result) {
        String key = resultKey(result);
        for (String raw : FeaturesConfig.villagerResults) {
            if (raw == null) {
                continue;
            }
            if (key != null && raw.trim().equalsIgnoreCase(key)) {
                return true;
            }
            if (!raw.contains("~") && TargetMatch.item(result, TargetMatch.item(raw))) {
                return true;
            }
        }
        return false;
    }

    private void addListing(VillagerTrades.ItemListing listing, LivingEntity entity, RandomSource random) {
        String kind = listing.getClass().getSimpleName();
        if ("EnchantBookForEmeralds".equals(kind)) {
            for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS) {
                if (enchantment == null || !enchantment.isTradeable()) {
                    continue;
                }
                int level = enchantment.getMaxLevel();
                ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level));
                int low = bookPrice(enchantment, enchantment.getMinLevel(), true);
                int high = bookPrice(enchantment, level, false);
                offers.add(new Shown(new MerchantOffer(new ItemStack(Items.EMERALD), new ItemStack(Items.BOOK), book, 0, 12, 1, 0.2f),
                        null, false, low, high));
            }
            return;
        }
        if ("EnchantedItemForEmeralds".equals(kind)) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (int i = 0; i < 12; i++) {
                MerchantOffer offer = listing.getOffer(entity, RandomSource.create(i * 9176L + 11L));
                if (offer == null || !seen.add(enchantLabel(offer.getResult()))) {
                    continue;
                }
                int cost = offer.getCostA().getCount();
                offers.add(new Shown(offer, null, false, cost, cost));
            }
            return;
        }
        MerchantOffer offer = listing.getOffer(entity, random);
        if (offer != null) {
            int cost = offer.getCostA().getCount();
            offers.add(new Shown(offer, null, false, cost, cost));
        }
    }

    private static int bookPrice(Enchantment enchantment, int level, boolean low) {
        int roll = low ? 0 : Math.max(0, 5 + level * 10 - 1);
        int cost = 2 + roll + 3 * level;
        if (enchantment.isTreasureOnly()) {
            cost *= 2;
        }
        return Mth.clamp(cost, 1, 64);
    }

    private static String enchantLabel(ItemStack stack) {
        var map = EnchantmentHelper.getEnchantments(stack);
        if (map.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (var entry : map.entrySet()) {
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(Component.translatable(entry.getKey().getDescriptionId()).getString());
            text.append(' ').append(entry.getValue());
        }
        return text.toString();
    }

    private static String resultKey(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return null;
        }
        var map = EnchantmentHelper.getEnchantments(stack);
        if (map.size() == 1) {
            ResourceLocation enchantment = EnchantmentHelper.getEnchantmentId(map.keySet().iterator().next());
            if (enchantment != null) {
                return id + "~" + enchantment;
            }
        }
        return id.toString();
    }

    private void toggleJob(Job job) {
        if (job.profession == null) {
            FeaturesConfig.setVillagerBlockWandering(!FeaturesConfig.villagerBlockWandering);
        } else if (blocked(job) && !FeaturesConfig.villagerBlockAll) {
            List<String> next = new ArrayList<>();
            for (String raw : FeaturesConfig.villagerProfessions) {
                if (!job.id.equals(TargetMatch.entity(raw))) {
                    next.add(raw);
                }
            }
            FeaturesConfig.setVillagerProfessions(next);
        } else if (!FeaturesConfig.villagerBlockAll) {
            List<String> next = new ArrayList<>(FeaturesConfig.villagerProfessions);
            next.add(job.id);
            FeaturesConfig.setVillagerProfessions(next);
        }
        push();
    }

    private void toggleOffer(Shown shown) {
        if (shown.custom) {
            FeaturesConfig.setVillagerCustoms(TradeCustoms.remove(FeaturesConfig.villagerCustoms, shown.stored));
            rebuild();
            push();
            return;
        }
        String key = resultKey(shown.offer.getResult());
        if (key == null) {
            return;
        }
        if (forbidden(shown.offer.getResult())) {
            List<String> next = new ArrayList<>();
            for (String raw : FeaturesConfig.villagerResults) {
                boolean sameKey = raw != null && raw.trim().equalsIgnoreCase(key);
                boolean plain = raw != null && !raw.contains("~")
                        && TargetMatch.item(shown.offer.getResult(), TargetMatch.item(raw));
                if (!sameKey && !plain) {
                    next.add(raw);
                }
            }
            FeaturesConfig.setVillagerResults(next);
        } else {
            List<String> next = new ArrayList<>(FeaturesConfig.villagerResults);
            next.add(key);
            FeaturesConfig.setVillagerResults(next);
        }
        push();
    }

    private void addCustom() {
        if (jobs.isEmpty()) {
            return;
        }
        TradeCustoms.Custom custom = TradeCustoms.build(jobs.get(selected).id, costA, costB, result);
        if (custom == null) {
            return;
        }
        FeaturesConfig.setVillagerCustoms(TradeCustoms.add(FeaturesConfig.villagerCustoms, custom));
        costA = ItemStack.EMPTY;
        costB = ItemStack.EMPTY;
        result = ItemStack.EMPTY;
        rebuild();
        push();
    }

    private void push() {
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetwork.sendToServer(VillagerTradesPacket.current());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= listLeft && mouseX < listLeft + listWidth
                && mouseY >= listTop && mouseY < listTop + listHeight) {
            int index = jobScroll + (int) ((mouseY - listTop) / ROW);
            if (index >= 0 && index < jobs.size()) {
                selected = index;
                offerScroll = 0;
                rebuild();
                return true;
            }
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
        if (mouseX < listLeft + listWidth) {
            jobScroll = Mth.clamp(jobScroll - (int) Math.signum(delta), 0, Math.max(0, jobs.size() - Math.max(1, listHeight / ROW)));
            return true;
        }
        offerScroll = Math.max(0, offerScroll - (int) Math.signum(delta) * 28);
        return true;
    }

    @Override
    public void removed() {
        discard();
        super.removed();
    }

    @Override
    public void onClose() {
        discard();
        super.onClose();
    }

    private void discard() {
        if (preview != null) {
            preview.discard();
            preview = null;
        }
    }

    @Override
    protected int buttonAccent() {
        return ACCENT;
    }

    @Override
    protected int buttonHover() {
        return 0xFF163028;
    }

    private record Job(String id, Component name, VillagerProfession profession) {
    }

    private record Shown(MerchantOffer offer, String stored, boolean custom, int costMin, int costMax) {
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }
}
