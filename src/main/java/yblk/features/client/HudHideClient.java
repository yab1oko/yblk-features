package yblk.features.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.hud.HudElement;
import yblk.features.hud.HudHide;
import yblk.features.hud.HudHideStyle;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
public final class HudHideClient {
    private static final int REVEAL_TICKS = 30;
    private static final int FADE_TICKS = 24;
    private static final float SLIDE_BOTTOM_PX = 56f;
    private static final float SLIDE_EFFECTS_PX = -64f;

    private static final Map<HudElement, Integer> revealTicksLeft = new EnumMap<>(HudElement.class);
    private static final Map<HudElement, Boolean> posePushed = new EnumMap<>(HudElement.class);

    /** Active element while a Gui / ForgeGui render method runs. */
    private static HudElement activeElement;
    private static float activeProgress = 1f;

    private static int lastSlot = -1;
    private static int lastSelectedHash;
    private static float lastHealth = -1f;
    private static float lastAbsorption = -1f;
    private static int lastFood = -1;
    private static int lastArmor = -1;
    private static int lastXpTotal = -1;
    private static int lastEffectsHash;

    private HudHideClient() {
    }

    /**
     * Reveal progress 0..1. 1 = fully visible, 0 = fully hidden.
     * Uses smoothstep over the fade/slide window after the solid reveal period.
     */
    public static float revealProgress(HudElement element) {
        if (!HudHide.isHidden(element)) {
            return 1f;
        }
        if (!HudHide.showOnChange(element)) {
            return 0f;
        }
        int left = revealTicksLeft.getOrDefault(element, 0);
        if (left <= 0) {
            return 0f;
        }
        if (left > FADE_TICKS) {
            return 1f;
        }
        float t = left / (float) FADE_TICKS;
        return t * t * (3f - 2f * t);
    }

    /** @deprecated use {@link #revealProgress} */
    public static float revealAlpha(HudElement element) {
        return revealProgress(element);
    }

    /** Called from Gui / ForgeGui mixins after vanilla/Forge resets color. */
    public static void applyDrawEffect(GuiGraphics graphics) {
        if (activeElement == null) {
            return;
        }
        float p = Mth.clamp(activeProgress, 0f, 1f);
        if (FeaturesConfig.hudHideStyle == HudHideStyle.FADE) {
            if (p < 0.999f) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                graphics.setColor(1f, 1f, 1f, p);
                RenderSystem.setShaderColor(1f, 1f, 1f, p);
            }
        }
        // SLIDE is applied via pose in overlay Pre (survives setupOverlayRenderState).
    }

    public static void clearDrawEffect(GuiGraphics graphics) {
        graphics.setColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** @deprecated use {@link #applyDrawEffect} */
    public static void applyDrawAlpha(GuiGraphics graphics) {
        applyDrawEffect(graphics);
    }

    /** @deprecated use {@link #clearDrawEffect} */
    public static void clearDrawAlpha(GuiGraphics graphics) {
        clearDrawEffect(graphics);
    }

    private static void bump(HudElement element) {
        if (HudHide.isHidden(element) && HudHide.showOnChange(element)) {
            revealTicksLeft.put(element, REVEAL_TICKS + FADE_TICKS);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !HudHide.isActive()) {
            revealTicksLeft.clear();
            lastSlot = -1;
            lastHealth = -1f;
            lastXpTotal = -1;
            lastEffectsHash = 0;
            return;
        }

        for (HudElement element : HudElement.values()) {
            int left = revealTicksLeft.getOrDefault(element, 0);
            if (left > 0) {
                revealTicksLeft.put(element, left - 1);
            }
        }

        Inventory inv = player.getInventory();
        int slot = inv.selected;
        int selectedHash = System.identityHashCode(inv.getSelected().getItem()) * 31 + inv.getSelected().getCount();
        if (lastSlot >= 0 && (slot != lastSlot || selectedHash != lastSelectedHash)) {
            bump(HudElement.HOTBAR);
        }
        lastSlot = slot;
        lastSelectedHash = selectedHash;

        float health = player.getHealth();
        float absorption = player.getAbsorptionAmount();
        if (lastHealth >= 0f && (Math.abs(health - lastHealth) > 0.01f || Math.abs(absorption - lastAbsorption) > 0.01f)) {
            bump(HudElement.HEARTS);
        }
        lastHealth = health;
        lastAbsorption = absorption;

        int food = player.getFoodData().getFoodLevel();
        if (lastFood >= 0 && food != lastFood) {
            bump(HudElement.HUNGER);
        }
        lastFood = food;

        int armor = player.getArmorValue();
        if (lastArmor >= 0 && armor != lastArmor) {
            bump(HudElement.ARMOR);
        }
        lastArmor = armor;

        int xpTotal = player.experienceLevel * 1000 + Math.round(player.experienceProgress * 100f);
        if (lastXpTotal >= 0 && xpTotal != lastXpTotal) {
            bump(HudElement.EXPERIENCE);
        }
        lastXpTotal = xpTotal;

        int effectsHash = hashEffects(player.getActiveEffects());
        if (lastEffectsHash != 0 && effectsHash != lastEffectsHash) {
            bump(HudElement.EFFECTS);
        }
        lastEffectsHash = effectsHash;
    }

    private static int hashEffects(Collection<MobEffectInstance> effects) {
        int h = 1;
        for (MobEffectInstance effect : effects) {
            h = 31 * h + effect.getEffect().hashCode();
            h = 31 * h + effect.getAmplifier();
            h = 31 * h + effect.getDuration();
        }
        return h;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!HudHide.isActive()) {
            activeElement = null;
            activeProgress = 1f;
            return;
        }
        HudElement element = mapOverlay(event);
        if (element == null) {
            return;
        }

        float progress = revealProgress(element);
        activeElement = element;
        activeProgress = progress;

        if (progress <= 0.001f) {
            event.setCanceled(true);
            activeElement = null;
            return;
        }

        float y = needsBottomShift(element) ? bottomShift() : 0f;
        if (FeaturesConfig.hudHideStyle == HudHideStyle.SLIDE && progress < 0.999f) {
            y += slidePixels(element) * (1f - progress);
        }

        if (Math.abs(y) > 0.01f) {
            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0, y, 0);
            posePushed.put(element, true);
        } else {
            posePushed.remove(element);
        }

        // Pre-apply fade too (ForgeGui methods re-apply after setupOverlayRenderState via mixin).
        if (FeaturesConfig.hudHideStyle == HudHideStyle.FADE) {
            applyDrawEffect(event.getGuiGraphics());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        HudElement element = mapOverlay(event);
        if (element == null) {
            return;
        }
        if (Boolean.TRUE.equals(posePushed.remove(element))) {
            event.getGuiGraphics().pose().popPose();
        }
        clearDrawEffect(event.getGuiGraphics());
        if (activeElement == element) {
            activeElement = null;
            activeProgress = 1f;
        }
    }

    private static float slidePixels(HudElement element) {
        return element == HudElement.EFFECTS ? SLIDE_EFFECTS_PX : SLIDE_BOTTOM_PX;
    }

    private static boolean needsBottomShift(HudElement element) {
        return element == HudElement.HEARTS
                || element == HudElement.HUNGER
                || element == HudElement.ARMOR;
    }

    private static int bottomShift() {
        int shift = 0;
        if (HudHide.isHidden(HudElement.HOTBAR) && revealProgress(HudElement.HOTBAR) <= 0.05f) {
            shift += 22;
        }
        if (HudHide.isHidden(HudElement.EXPERIENCE) && revealProgress(HudElement.EXPERIENCE) <= 0.05f) {
            if (shift == 0) {
                shift += 7;
            }
        }
        return shift;
    }

    private static HudElement mapOverlay(RenderGuiOverlayEvent event) {
        var overlay = event.getOverlay();
        if (overlay == VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            return HudElement.HEARTS;
        }
        if (overlay == VanillaGuiOverlay.FOOD_LEVEL.type() || overlay == VanillaGuiOverlay.AIR_LEVEL.type()) {
            return HudElement.HUNGER;
        }
        if (overlay == VanillaGuiOverlay.ARMOR_LEVEL.type()) {
            return HudElement.ARMOR;
        }
        if (overlay == VanillaGuiOverlay.HOTBAR.type() || overlay == VanillaGuiOverlay.ITEM_NAME.type()) {
            return HudElement.HOTBAR;
        }
        if (overlay == VanillaGuiOverlay.EXPERIENCE_BAR.type()) {
            return HudElement.EXPERIENCE;
        }
        if (overlay == VanillaGuiOverlay.POTION_ICONS.type()) {
            return HudElement.EFFECTS;
        }
        if (overlay == VanillaGuiOverlay.CROSSHAIR.type()) {
            return HudElement.CROSSHAIR;
        }
        return null;
    }
}
