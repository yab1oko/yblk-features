package yblk.features.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.Features;
import yblk.features.border.BorderBarrierVisual;
import yblk.features.border.BorderEffectOptions;
import yblk.features.border.BorderHarmMode;
import yblk.features.border.BorderWarningStyle;
import yblk.features.border.BorderZoneFx;
import yblk.features.f3.F3Category;
import yblk.features.f3.F3Element;
import yblk.features.hud.HudElement;
import yblk.features.hud.HudHideStyle;
import yblk.features.names.HideNamesMode;
import yblk.features.rfp.RfpHandMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Features.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FeaturesConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue INVENTORY_REVAMP = BUILDER
            .comment("Enable inventory revamp feature (hotbar-only until armor grants slots).")
            .define("features.inventoryRevamp", true);

    private static final ForgeConfigSpec.BooleanValue MOB_SPAWN_BLOCK = BUILDER
            .comment("Enable mob spawn block feature (prevent selected mobs from natural spawning).")
            .define("features.mobSpawnBlock", false);

    private static final ForgeConfigSpec.BooleanValue BORDER_DANGER = BUILDER
            .comment("Enable deadly zone near the world border.")
            .define("features.borderDanger", false);

    private static final ForgeConfigSpec.BooleanValue MOB_SPAWN_BLOCK_ALL = BUILDER
            .comment("If true, also block spawn eggs, commands, buckets, dispensers, breeding and conversion.",
                    "If false, only natural/spawner/structure-style spawns are blocked.")
            .define("mobSpawnBlock.blockAllSources", false);

    private static final ForgeConfigSpec.IntValue BORDER_DISTANCE = BUILDER
            .comment("Blocks from the border where damage/hidden effects start.")
            .defineInRange("borderDanger.distance", 16, 1, 512);

    private static final ForgeConfigSpec.IntValue BORDER_VISUAL_DISTANCE = BUILDER
            .comment("Blocks from the border where weather/fog FX begin (should be >= distance).")
            .defineInRange("borderDanger.visualDistance", 24, 1, 512);

    private static final ForgeConfigSpec.IntValue BORDER_APPROACH_DISTANCE = BUILDER
            .comment("Blocks from the border where the approach warning starts (should be >= visualDistance).")
            .defineInRange("borderDanger.approachDistance", 32, 1, 512);

    private static final ForgeConfigSpec.ConfigValue<String> BORDER_HARM_MODE = BUILDER
            .comment("DAMAGE = ~1 heart/sec, EFFECTS = hidden status effects.")
            .define("borderDanger.harmMode", "DAMAGE");

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BORDER_EFFECTS = BUILDER
            .comment("Effect keys when harmMode=EFFECTS: nausea, wither, poison, blindness, slowness, hunger, weakness.")
            .defineListAllowEmpty(List.of("borderDanger", "effects"),
                    () -> List.of("nausea"),
                    o -> o instanceof String s && BorderEffectOptions.ALL.containsKey(s.trim().toLowerCase()));

    private static final ForgeConfigSpec.BooleanValue BORDER_WARNING = BUILDER
            .comment("Show warnings while approaching / inside the danger zone.")
            .define("borderDanger.showWarning", true);

    private static final ForgeConfigSpec.ConfigValue<String> BORDER_WARN_STYLE = BUILDER
            .comment("Warning display style: CHAT, HOTBAR, CENTER.")
            .define("borderDanger.warningStyle", "CENTER");

    private static final ForgeConfigSpec.BooleanValue BORDER_HIDE_VANILLA_RED = BUILDER
            .comment("Hide the vanilla red screen vignette near the world border.")
            .define("borderDanger.hideVanillaRedTint", false);

    private static final ForgeConfigSpec.ConfigValue<String> BORDER_WARN_COLOR_APPROACH_A = BUILDER
            .comment("Approach warning color A (hex RGB), blinks toward color B.")
            .define("borderDanger.warnColorApproachA", "FFFF55");

    private static final ForgeConfigSpec.ConfigValue<String> BORDER_WARN_COLOR_APPROACH_B = BUILDER
            .comment("Approach warning color B (hex RGB).")
            .define("borderDanger.warnColorApproachB", "FFD700");

    private static final ForgeConfigSpec.ConfigValue<String> BORDER_WARN_COLOR_DANGER_A = BUILDER
            .comment("Danger-zone warning color A (hex RGB).")
            .define("borderDanger.warnColorDangerA", "FFD700");

    private static final ForgeConfigSpec.ConfigValue<String> BORDER_WARN_COLOR_DANGER_B = BUILDER
            .comment("Danger-zone warning color B (hex RGB).")
            .define("borderDanger.warnColorDangerB", "FF3333");

    private static final ForgeConfigSpec.ConfigValue<String> BORDER_WARN_TEXT_APPROACH = BUILDER
            .comment("Custom approach warning text. Empty = language default.")
            .define("borderDanger.warnTextApproach", "");

    private static final ForgeConfigSpec.ConfigValue<String> BORDER_WARN_TEXT_DANGER = BUILDER
            .comment("Custom in-zone warning text. Empty = language default.")
            .define("borderDanger.warnTextDanger", "");

    private static final ForgeConfigSpec.ConfigValue<String> BORDER_BARRIER = BUILDER
            .comment("World border visual: VANILLA, HIDDEN, CUSTOM.")
            .define("borderDanger.barrierVisual", "VANILLA");

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BORDER_ZONE_FX = BUILDER
            .comment("Zone visuals (multi-select): haze, fog, snow, rain.")
            .defineListAllowEmpty(List.of("borderDanger", "zoneFxList"), ArrayList::new,
                    o -> o instanceof String s && BorderZoneFx.isValidKey(s));

    private static final ForgeConfigSpec.BooleanValue KEEP_ITEMS = BUILDER
            .comment("When storage slots shrink, keep items in locked slots instead of dropping them.")
            .define("inventoryRevamp.keepItemsWhenSlotsShrink", false);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ARMOR_SLOTS = BUILDER
            .comment("Bonus storage slots granted by items worn in armor slots. Format: namespaceID:path=slots",
                    "Scaled by armor tier — only a full netherite set reaches 27.")
            .defineList("inventoryRevamp.armorSlotBonuses", defaultArmorBonusStrings(), FeaturesConfig::validateArmorEntry);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCKED_MOBS = BUILDER
            .comment("Entity IDs whose spawning is blocked when mob spawn block is enabled.",
                    "Whether eggs/commands are included depends on mobSpawnBlock.blockAllSources.")
            .defineListAllowEmpty(List.of("mobSpawnBlock", "blockedMobs"), ArrayList::new, FeaturesConfig::validateEntityId);

    private static final ForgeConfigSpec.BooleanValue HUD_HIDE = BUILDER
            .comment("Enable modular HUD hide (hearts, hunger, armor, hotbar, crosshair).")
            .define("features.hudHide", false);

    private static final ForgeConfigSpec.BooleanValue HUD_HIDE_ADMIN_LOCK = BUILDER
            .comment("If true, only operators (permission level 2+) can change HUD hide settings.")
            .define("hudHide.adminLocked", false);

    private static final ForgeConfigSpec.BooleanValue HUD_HIDE_HEARTS = BUILDER
            .define("hudHide.hideHearts", false);
    private static final ForgeConfigSpec.BooleanValue HUD_HIDE_HUNGER = BUILDER
            .define("hudHide.hideHunger", false);
    private static final ForgeConfigSpec.BooleanValue HUD_HIDE_ARMOR = BUILDER
            .define("hudHide.hideArmor", false);
    private static final ForgeConfigSpec.BooleanValue HUD_HIDE_HOTBAR = BUILDER
            .define("hudHide.hideHotbar", false);
    private static final ForgeConfigSpec.BooleanValue HUD_HIDE_CROSSHAIR = BUILDER
            .define("hudHide.hideCrosshair", false);
    private static final ForgeConfigSpec.BooleanValue HUD_HIDE_EXPERIENCE = BUILDER
            .define("hudHide.hideExperience", false);
    private static final ForgeConfigSpec.BooleanValue HUD_HIDE_EFFECTS = BUILDER
            .define("hudHide.hideEffects", false);

    private static final ForgeConfigSpec.BooleanValue HUD_SOC_HEARTS = BUILDER
            .comment("Temporarily show hidden hearts when health changes.")
            .define("hudHide.showOnChangeHearts", true);
    private static final ForgeConfigSpec.BooleanValue HUD_SOC_HUNGER = BUILDER
            .define("hudHide.showOnChangeHunger", true);
    private static final ForgeConfigSpec.BooleanValue HUD_SOC_ARMOR = BUILDER
            .define("hudHide.showOnChangeArmor", true);
    private static final ForgeConfigSpec.BooleanValue HUD_SOC_HOTBAR = BUILDER
            .define("hudHide.showOnChangeHotbar", true);
    private static final ForgeConfigSpec.BooleanValue HUD_SOC_EXPERIENCE = BUILDER
            .define("hudHide.showOnChangeExperience", true);
    private static final ForgeConfigSpec.BooleanValue HUD_SOC_EFFECTS = BUILDER
            .define("hudHide.showOnChangeEffects", true);

    private static final ForgeConfigSpec.ConfigValue<String> HUD_HIDE_STYLE = BUILDER
            .comment("Hide animation: FADE (alpha) or SLIDE (move off-screen).")
            .define("hudHide.style", "FADE");

    private static final ForgeConfigSpec.BooleanValue F3_HIDE = BUILDER
            .comment("Enable modular F3 debug screen filtering.")
            .define("features.f3Hide", false);

    private static final ForgeConfigSpec.BooleanValue F3_HIDE_ADMIN_LOCK = BUILDER
            .comment("If true, only operators (permission level 2+) can change F3 hide settings.")
            .define("f3Hide.adminLocked", false);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> F3_HIDDEN = BUILDER
            .comment("F3 line groups to hide. Keys: coordinates, light, day, entities, version, fps, biome, ...")
            .defineListAllowEmpty(List.of("f3Hide", "hidden"), ArrayList::new, o -> o instanceof String);

    private static final ForgeConfigSpec.BooleanValue HIDE_NAMES = BUILDER
            .comment("Enable hide player name tags feature.")
            .define("features.hideNames", false);

    private static final ForgeConfigSpec.ConfigValue<String> HIDE_NAMES_MODE = BUILDER
            .comment("Reveal mode: FULL, PROXIMITY_FADE, LOOK_CLOSE, MASKS.")
            .define("hideNames.mode", "MASKS");

    /** Legacy — migrated into hideNames.mode on load. */
    private static final ForgeConfigSpec.BooleanValue HIDE_NAMES_ALL = BUILDER
            .comment("Legacy. Prefer hideNames.mode=FULL.")
            .define("hideNames.hideAll", false);

    /** Legacy — migrated into hideNames.mode on load. */
    private static final ForgeConfigSpec.BooleanValue HIDE_NAMES_BY_ITEMS = BUILDER
            .comment("Legacy. Prefer hideNames.mode=MASKS.")
            .define("hideNames.byItems", true);

    private static final ForgeConfigSpec.DoubleValue HIDE_NAMES_FADE_NEAR = BUILDER
            .comment("PROXIMITY_FADE: distance (blocks) where the name is fully visible.")
            .defineInRange("hideNames.fadeNear", 8.0, 1.0, 64.0);

    private static final ForgeConfigSpec.DoubleValue HIDE_NAMES_FADE_FAR = BUILDER
            .comment("PROXIMITY_FADE: distance (blocks) where the name is fully hidden.")
            .defineInRange("hideNames.fadeFar", 24.0, 2.0, 128.0);

    private static final ForgeConfigSpec.DoubleValue HIDE_NAMES_LOOK_DISTANCE = BUILDER
            .comment("LOOK_CLOSE: max distance (blocks) to show a name while looking at the player.")
            .defineInRange("hideNames.lookDistance", 6.0, 1.0, 32.0);

    private static final ForgeConfigSpec.DoubleValue HIDE_NAMES_LOOK_ANGLE = BUILDER
            .comment("LOOK_CLOSE: minimum look-dot (1=dead center, 0.9≈25°).")
            .defineInRange("hideNames.lookAngle", 0.92, 0.5, 1.0);

    private static final ForgeConfigSpec.BooleanValue HIDE_NAMES_ADMIN_LOCK = BUILDER
            .comment("If true, only operators may change the mask item list / admin lock.")
            .define("hideNames.adminLocked", false);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> HIDE_NAMES_ITEMS = BUILDER
            .comment("Head-slot item IDs that hide the wearer's name tag when mode is MASKS.")
            .defineListAllowEmpty(List.of("hideNames", "items"), FeaturesConfig::defaultHideNamesItems,
                    FeaturesConfig::validateItemId);

    private static final ForgeConfigSpec.BooleanValue REAL_FIRST_PERSON = BUILDER
            .comment("Real First Person: render your third-person body in first person.")
            .define("features.realFirstPerson", false);

    private static final ForgeConfigSpec.BooleanValue RFP_HIDE_HEAD = BUILDER
            .comment("Hide the head (and hat layer) in the FP body pass to avoid camera clipping.")
            .define("realFirstPerson.hideHead", true);

    private static final ForgeConfigSpec.BooleanValue RFP_RENDER_STUCK = BUILDER
            .comment("Show stuck arrows / bee stings on the FP body when the renderer provides them.")
            .define("realFirstPerson.renderStuck", true);

    private static final ForgeConfigSpec.ConfigValue<String> RFP_HAND_MODE = BUILDER
            .comment("Vanilla FP hands: OFF, DYNAMIC (keep while swimming/elytra), ALWAYS.")
            .define("realFirstPerson.handMode", "OFF");

    /** Body Z offset range for Real First Person (positive = away from camera). */
    public static final float RFP_OFFSET_MIN = -2.0f;
    public static final float RFP_OFFSET_MAX = 1.0f;

    public static final float RFP_DEFAULT_STAND = -0.25f;
    public static final float RFP_DEFAULT_SNEAK = -0.25f;
    public static final float RFP_DEFAULT_SIT = -0.25f;
    public static final float RFP_DEFAULT_SWIM = -0.50f;
    public static final float RFP_DEFAULT_ELYTRA = -1.30f;

    private static final ForgeConfigSpec.DoubleValue RFP_OFFSET_STAND = BUILDER
            .comment("Body Z offset while standing (-2..1, positive pushes body away from camera).")
            .defineInRange("realFirstPerson.offsetStand", RFP_DEFAULT_STAND, -2.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue RFP_OFFSET_SNEAK = BUILDER
            .comment("Body Z offset while sneaking (-2..1).")
            .defineInRange("realFirstPerson.offsetSneak", RFP_DEFAULT_SNEAK, -2.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue RFP_OFFSET_SIT = BUILDER
            .comment("Body Z offset while sitting / riding (-2..1).")
            .defineInRange("realFirstPerson.offsetSit", RFP_DEFAULT_SIT, -2.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue RFP_OFFSET_SWIM = BUILDER
            .comment("Body Z offset while swimming (-2..1). Separate from elytra.")
            .defineInRange("realFirstPerson.offsetSwim", RFP_DEFAULT_SWIM, -2.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue RFP_OFFSET_ELYTRA = BUILDER
            .comment("Body Z offset while gliding with elytra (-2..1). Separate from swimming.")
            .defineInRange("realFirstPerson.offsetElytra", RFP_DEFAULT_ELYTRA, -2.0, 1.0);

    private static final ForgeConfigSpec.BooleanValue TRUE_NIGHT = BUILDER
            .comment("True Night: moon-phase outdoor darkness + random night fog.")
            .define("features.trueNight", false);

    private static final ForgeConfigSpec.BooleanValue TRUE_NIGHT_MOON_PHASES = BUILDER
            .comment("If true, sky darkness follows moon phase (True Darkness). If false, every night uses darkness strength.")
            .define("trueNight.moonPhases", true);

    private static final ForgeConfigSpec.DoubleValue TRUE_NIGHT_DARKNESS = BUILDER
            .comment("How hard nights crush outdoor sky light (0..1). With moon phases, scales the new-moon extreme.")
            .defineInRange("trueNight.darkness", 1.0, 0.0, 1.0);

    private static final ForgeConfigSpec.BooleanValue TRUE_NIGHT_FOG = BUILDER
            .comment("Random fog banks during night.")
            .define("trueNight.fogEnabled", true);

    private static final ForgeConfigSpec.DoubleValue TRUE_NIGHT_FOG_CHANCE = BUILDER
            .comment("Approximate chance per minute to start a fog bank while it is night (0..1).")
            .defineInRange("trueNight.fogChancePerMinute", 0.22, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue TRUE_NIGHT_FOG_MIN = BUILDER
            .comment("Minimum fog bank intensity (0..1).")
            .defineInRange("trueNight.fogMin", 0.35, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue TRUE_NIGHT_FOG_MAX = BUILDER
            .comment("Maximum fog bank intensity (0..1).")
            .defineInRange("trueNight.fogMax", 0.95, 0.0, 1.0);

    private static final ForgeConfigSpec.BooleanValue LOCAL_CHAT = BUILDER
            .comment("Local chat: default messages only reach nearby players; prefix ! for global. Ops only.")
            .define("features.localChat", false);

    private static final ForgeConfigSpec.IntValue LOCAL_CHAT_RANGE = BUILDER
            .comment("Local chat hear distance in blocks.")
            .defineInRange("localChat.range", 100, 1, 512);

    private static final ForgeConfigSpec.BooleanValue LOCAL_CHAT_ADMIN_BYPASS = BUILDER
            .comment("If true, operators (permission 2+) hear all local chat map-wide.")
            .define("localChat.adminBypass", true);

    private static final ForgeConfigSpec.BooleanValue WETNESS = BUILDER
            .comment("Wetness system: rain/water soak, drying, heat, freeze, armor slow.")
            .define("features.wetness", false);

    private static final ForgeConfigSpec.DoubleValue WETNESS_RAIN = BUILDER
            .comment("Wetness %/s in open-sky rain/snow. Wading (not fully submerged) uses 2× this rate.")
            .defineInRange("wetness.rainRate", 1.0, 0.0, 100.0);

    private static final ForgeConfigSpec.DoubleValue WETNESS_DRY = BUILDER
            .comment("Wetness % lost per second on dry land (no rain/water).")
            .defineInRange("wetness.dryRate", 1.0, 0.0, 100.0);

    private static final ForgeConfigSpec.DoubleValue WETNESS_WATER_ENTER_BARE = BUILDER
            .comment("Unused (kept for config compatibility). Full submersion is always instant.")
            .defineInRange("wetness.waterEnterBare", 8.0, 0.0, 100.0);

    private static final ForgeConfigSpec.DoubleValue WETNESS_WATER_ENTER_FULL = BUILDER
            .comment("Unused (kept for config compatibility). Full submersion is always instant.")
            .defineInRange("wetness.waterEnterFull", 40.0, 0.0, 100.0);

    private static final ForgeConfigSpec.DoubleValue WETNESS_WATER_RATE_BARE = BUILDER
            .comment("Unused — wading uses rainRate. Kept for config compatibility.")
            .defineInRange("wetness.waterRateBare", 4.0, 0.0, 100.0);

    private static final ForgeConfigSpec.DoubleValue WETNESS_WATER_RATE_FULL = BUILDER
            .comment("Unused — wading uses rainRate. Kept for config compatibility.")
            .defineInRange("wetness.waterRateFull", 10.0, 0.0, 100.0);

    private static final ForgeConfigSpec.DoubleValue WETNESS_HEAT_TORCH = BUILDER
            .comment("Dry %/s near torches/lanterns.")
            .defineInRange("wetness.heatTorch", 1.5, 0.0, 100.0);

    private static final ForgeConfigSpec.DoubleValue WETNESS_HEAT_FIRE = BUILDER
            .comment("Dry %/s near campfire/fire.")
            .defineInRange("wetness.heatFire", 2.0, 0.0, 100.0);

    private static final ForgeConfigSpec.DoubleValue WETNESS_HEAT_LAVA = BUILDER
            .comment("Dry %/s near lava/magma.")
            .defineInRange("wetness.heatLava", 3.0, 0.0, 100.0);

    private static final ForgeConfigSpec.IntValue WETNESS_HEAT_RADIUS = BUILDER
            .comment("Block radius to scan for heat sources.")
            .defineInRange("wetness.heatRadius", 4, 1, 8);

    private static final ForgeConfigSpec.IntValue WETNESS_FREEZE_SECONDS = BUILDER
            .comment("Seconds wet on land before freeze/slowness applies (~4 min default).")
            .defineInRange("wetness.freezeSeconds", 240, 30, 1800);

    private static final ForgeConfigSpec.DoubleValue WETNESS_ARMOR_SLOW = BUILDER
            .comment("Movement speed multiplier penalty while wet + armored (0.20 = -20%).")
            .defineInRange("wetness.armorSlow", 0.20, 0.0, 1.0);

    private static final ForgeConfigSpec.BooleanValue ITEM_REMOVER = BUILDER
            .comment("Item remover: ban items from crafts, loot, and inventories.")
            .define("features.itemRemover", false);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BANNED_ITEMS = BUILDER
            .comment("Item IDs banned when item remover is enabled.")
            .defineListAllowEmpty(List.of("itemRemover", "bannedItems"), ArrayList::new, FeaturesConfig::validateItemId);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean inventoryRevampEnabled = true;
    public static boolean mobSpawnBlockEnabled;
    /** When true, block eggs/commands/etc.; when false, only natural-like spawns. */
    public static boolean mobSpawnBlockAllSources;
    public static boolean borderDangerEnabled;
    public static int borderDangerDistance = 16;
    public static int borderVisualDistance = 24;
    public static int borderApproachDistance = 32;
    public static BorderHarmMode borderHarmMode = BorderHarmMode.DAMAGE;
    public static Set<String> borderEffects = new HashSet<>(List.of("nausea"));
    public static boolean borderShowWarning = true;
    public static BorderWarningStyle borderWarningStyle = BorderWarningStyle.CENTER;
    public static boolean borderHideVanillaRedTint;
    public static int borderWarnColorApproachA = 0xFFFF55;
    public static int borderWarnColorApproachB = 0xFFD700;
    public static int borderWarnColorDangerA = 0xFFD700;
    public static int borderWarnColorDangerB = 0xFF3333;
    public static String borderWarnTextApproach = "";
    public static String borderWarnTextDanger = "";
    public static BorderBarrierVisual borderBarrierVisual = BorderBarrierVisual.VANILLA;
    public static Set<String> borderZoneFx = new HashSet<>();
    public static boolean keepItemsWhenSlotsShrink;
    public static Map<ResourceLocation, Integer> armorBonuses = new HashMap<>();
    public static Set<String> blockedMobIds = new HashSet<>();

    public static boolean hudHideEnabled;
    public static boolean hudHideAdminLocked;
    public static boolean hudHideHearts;
    public static boolean hudHideHunger;
    public static boolean hudHideArmor;
    public static boolean hudHideHotbar;
    public static boolean hudHideCrosshair;
    public static boolean hudHideExperience;
    public static boolean hudHideEffects;
    public static boolean hudShowOnChangeHearts = true;
    public static boolean hudShowOnChangeHunger = true;
    public static boolean hudShowOnChangeArmor = true;
    public static boolean hudShowOnChangeHotbar = true;
    public static boolean hudShowOnChangeExperience = true;
    public static boolean hudShowOnChangeEffects = true;
    public static HudHideStyle hudHideStyle = HudHideStyle.FADE;

    public static boolean f3HideEnabled;
    public static boolean f3HideAdminLocked;
    public static Set<String> f3HiddenElements = new HashSet<>();

    public static boolean hideNamesEnabled;
    public static HideNamesMode hideNamesMode = HideNamesMode.MASKS;
    /** @deprecated legacy mirror of mode == FULL */
    public static boolean hideNamesAll;
    /** @deprecated legacy mirror of mode == MASKS */
    public static boolean hideNamesByItems = true;
    public static float hideNamesFadeNear = 8F;
    public static float hideNamesFadeFar = 24F;
    public static float hideNamesLookDistance = 6F;
    public static float hideNamesLookAngle = 0.92F;
    public static boolean hideNamesAdminLocked;
    public static Set<String> hideNamesItems = new HashSet<>();

    public static boolean realFirstPersonEnabled;
    public static boolean rfpHideHead = true;
    public static boolean rfpRenderStuck = true;
    public static RfpHandMode rfpHandMode = RfpHandMode.OFF;
    public static float rfpOffsetStand = RFP_DEFAULT_STAND;
    public static float rfpOffsetSneak = RFP_DEFAULT_SNEAK;
    public static float rfpOffsetSit = RFP_DEFAULT_SIT;
    public static float rfpOffsetSwim = RFP_DEFAULT_SWIM;
    public static float rfpOffsetElytra = RFP_DEFAULT_ELYTRA;

    public static boolean trueNightEnabled;
    public static boolean trueNightMoonPhases = true;
    public static float trueNightDarkness = 1f;
    public static boolean trueNightFogEnabled = true;
    public static float trueNightFogChance = 0.22f;
    public static float trueNightFogMin = 0.35f;
    public static float trueNightFogMax = 0.95f;

    public static boolean localChatEnabled;
    public static int localChatRange = 100;
    public static boolean localChatAdminBypass = true;

    public static boolean wetnessEnabled;
    public static float wetnessRainRate = 1f;
    public static float wetnessDryRate = 1f;
    public static float wetnessWaterEnterBare = 8f;
    public static float wetnessWaterEnterFull = 40f;
    public static float wetnessWaterRateBare = 4f;
    public static float wetnessWaterRateFull = 10f;
    public static float wetnessHeatTorch = 1.5f;
    public static float wetnessHeatFire = 2f;
    public static float wetnessHeatLava = 3f;
    public static int wetnessHeatRadius = 4;
    public static int wetnessFreezeSeconds = 240;
    public static float wetnessArmorSlow = 0.20f;

    public static boolean itemRemoverEnabled;
    public static Set<String> bannedItemIds = new HashSet<>();

    private FeaturesConfig() {
    }

    public static List<String> defaultHideNamesItems() {
        return new ArrayList<>(List.of(
                "minecraft:carved_pumpkin",
                "minecraft:skeleton_skull",
                "minecraft:wither_skeleton_skull",
                "minecraft:zombie_head",
                "minecraft:player_head",
                "minecraft:creeper_head",
                "minecraft:dragon_head",
                "minecraft:piglin_head"
        ));
    }

    private static boolean validateItemId(Object raw) {
        if (!(raw instanceof String s) || s.isBlank()) {
            return false;
        }
        String id = s.contains(":") ? s : "minecraft:" + s;
        return ResourceLocation.tryParse(id) != null;
    }

    public static void setHudHideStyle(HudHideStyle style) {
        HudHideStyle resolved = style == null ? HudHideStyle.FADE : style;
        HUD_HIDE_STYLE.set(resolved.name());
        hudHideStyle = resolved;
    }

    /**
     * Piece bonuses by tier (helmet / chest / legs / boots). Full-set totals:
     * leather 9, chain 12, iron 15, gold 15, diamond 21, netherite 27.
     */
    public static List<String> defaultArmorBonusStrings() {
        List<String> list = new ArrayList<>();
        addSet(list, "leather", 2, 3, 2, 2);       // 9
        addSet(list, "chainmail", 2, 4, 3, 3);    // 12
        addSet(list, "iron", 3, 5, 4, 3);          // 15
        addSet(list, "golden", 3, 5, 4, 3);        // 15
        addSet(list, "diamond", 5, 7, 5, 4);       // 21
        addSet(list, "netherite", 6, 9, 7, 5);     // 27
        list.add("minecraft:turtle_helmet=3");
        return list;
    }

    private static void addSet(List<String> list, String material, int helm, int chest, int legs, int boots) {
        list.add("minecraft:" + material + "_helmet=" + helm);
        list.add("minecraft:" + material + "_chestplate=" + chest);
        list.add("minecraft:" + material + "_leggings=" + legs);
        list.add("minecraft:" + material + "_boots=" + boots);
    }

    private static boolean validateArmorEntry(Object obj) {
        if (!(obj instanceof String raw)) {
            return false;
        }
        String[] parts = raw.split("=");
        if (parts.length != 2) {
            return false;
        }
        if (ResourceLocation.tryParse(parts[0].trim()) == null) {
            return false;
        }
        try {
            int value = Integer.parseInt(parts[1].trim());
            return value >= 0 && value <= 45;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static boolean validateEntityId(Object obj) {
        return obj instanceof String raw && ResourceLocation.tryParse(raw.trim()) != null;
    }

    public static void setInventoryRevampEnabled(boolean enabled) {
        INVENTORY_REVAMP.set(enabled);
        inventoryRevampEnabled = enabled;
    }

    public static void setMobSpawnBlockEnabled(boolean enabled) {
        MOB_SPAWN_BLOCK.set(enabled);
        mobSpawnBlockEnabled = enabled;
    }

    public static void setMobSpawnBlockAllSources(boolean allSources) {
        MOB_SPAWN_BLOCK_ALL.set(allSources);
        mobSpawnBlockAllSources = allSources;
    }

    public static void setHudHideEnabled(boolean enabled) {
        HUD_HIDE.set(enabled);
        hudHideEnabled = enabled;
    }

    public static void setHudHideAdminLocked(boolean locked) {
        HUD_HIDE_ADMIN_LOCK.set(locked);
        hudHideAdminLocked = locked;
    }

    public static void setF3HideEnabled(boolean enabled) {
        F3_HIDE.set(enabled);
        f3HideEnabled = enabled;
    }

    public static void setHideNamesEnabled(boolean enabled) {
        HIDE_NAMES.set(enabled);
        hideNamesEnabled = enabled;
    }

    public static void setRealFirstPersonEnabled(boolean enabled) {
        REAL_FIRST_PERSON.set(enabled);
        realFirstPersonEnabled = enabled;
    }

    public static void setRfpHideHead(boolean hide) {
        RFP_HIDE_HEAD.set(hide);
        rfpHideHead = hide;
    }

    public static void setRfpRenderStuck(boolean render) {
        RFP_RENDER_STUCK.set(render);
        rfpRenderStuck = render;
    }

    public static void setRfpHandMode(RfpHandMode mode) {
        if (mode == null) {
            mode = RfpHandMode.OFF;
        }
        RFP_HAND_MODE.set(mode.name());
        rfpHandMode = mode;
    }

    public static void setRfpOffsetStand(float value) {
        float v = clampRfpOffset(value);
        RFP_OFFSET_STAND.set((double) v);
        rfpOffsetStand = v;
    }

    public static void setRfpOffsetSneak(float value) {
        float v = clampRfpOffset(value);
        RFP_OFFSET_SNEAK.set((double) v);
        rfpOffsetSneak = v;
    }

    public static void setRfpOffsetSit(float value) {
        float v = clampRfpOffset(value);
        RFP_OFFSET_SIT.set((double) v);
        rfpOffsetSit = v;
    }

    public static void setRfpOffsetSwim(float value) {
        float v = clampRfpOffset(value);
        RFP_OFFSET_SWIM.set((double) v);
        rfpOffsetSwim = v;
    }

    public static void setRfpOffsetElytra(float value) {
        float v = clampRfpOffset(value);
        RFP_OFFSET_ELYTRA.set((double) v);
        rfpOffsetElytra = v;
    }

    /** Restore recommended body-offset defaults. */
    public static void resetRfpOffsets() {
        setRfpOffsetStand(RFP_DEFAULT_STAND);
        setRfpOffsetSneak(RFP_DEFAULT_SNEAK);
        setRfpOffsetSit(RFP_DEFAULT_SIT);
        setRfpOffsetSwim(RFP_DEFAULT_SWIM);
        setRfpOffsetElytra(RFP_DEFAULT_ELYTRA);
    }

    private static float clampRfpOffset(float value) {
        return Math.max(RFP_OFFSET_MIN, Math.min(RFP_OFFSET_MAX, value));
    }

    public static void setTrueNightEnabled(boolean enabled) {
        TRUE_NIGHT.set(enabled);
        trueNightEnabled = enabled;
    }

    public static void setTrueNightMoonPhases(boolean moonPhases) {
        TRUE_NIGHT_MOON_PHASES.set(moonPhases);
        trueNightMoonPhases = moonPhases;
    }

    public static void setTrueNightDarkness(float value) {
        float v = Math.max(0f, Math.min(1f, value));
        TRUE_NIGHT_DARKNESS.set((double) v);
        trueNightDarkness = v;
    }

    public static void setTrueNightFogEnabled(boolean enabled) {
        TRUE_NIGHT_FOG.set(enabled);
        trueNightFogEnabled = enabled;
    }

    public static void setTrueNightFogChance(float value) {
        float v = Math.max(0f, Math.min(1f, value));
        TRUE_NIGHT_FOG_CHANCE.set((double) v);
        trueNightFogChance = v;
    }

    public static void setTrueNightFogMin(float value) {
        float v = Math.max(0f, Math.min(1f, value));
        TRUE_NIGHT_FOG_MIN.set((double) v);
        trueNightFogMin = v;
    }

    public static void setTrueNightFogMax(float value) {
        float v = Math.max(0f, Math.min(1f, value));
        TRUE_NIGHT_FOG_MAX.set((double) v);
        trueNightFogMax = v;
    }

    public static void setLocalChatEnabled(boolean enabled) {
        LOCAL_CHAT.set(enabled);
        localChatEnabled = enabled;
    }

    public static void setLocalChatRange(int blocks) {
        int v = Math.max(1, Math.min(512, blocks));
        LOCAL_CHAT_RANGE.set(v);
        localChatRange = v;
    }

    public static void setLocalChatAdminBypass(boolean bypass) {
        LOCAL_CHAT_ADMIN_BYPASS.set(bypass);
        localChatAdminBypass = bypass;
    }

    public static void setWetnessEnabled(boolean enabled) {
        WETNESS.set(enabled);
        wetnessEnabled = enabled;
    }

    public static void setWetnessRainRate(float value) {
        float v = clampFloat(value, 0f, 100f);
        WETNESS_RAIN.set((double) v);
        wetnessRainRate = v;
    }

    public static void setWetnessDryRate(float value) {
        float v = clampFloat(value, 0f, 100f);
        WETNESS_DRY.set((double) v);
        wetnessDryRate = v;
    }

    public static void setWetnessWaterEnterBare(float value) {
        float v = clampFloat(value, 0f, 100f);
        WETNESS_WATER_ENTER_BARE.set((double) v);
        wetnessWaterEnterBare = v;
    }

    public static void setWetnessWaterEnterFull(float value) {
        float v = clampFloat(value, 0f, 100f);
        WETNESS_WATER_ENTER_FULL.set((double) v);
        wetnessWaterEnterFull = v;
    }

    public static void setWetnessWaterRateBare(float value) {
        float v = clampFloat(value, 0f, 100f);
        WETNESS_WATER_RATE_BARE.set((double) v);
        wetnessWaterRateBare = v;
    }

    public static void setWetnessWaterRateFull(float value) {
        float v = clampFloat(value, 0f, 100f);
        WETNESS_WATER_RATE_FULL.set((double) v);
        wetnessWaterRateFull = v;
    }

    public static void setWetnessHeatTorch(float value) {
        float v = clampFloat(value, 0f, 100f);
        WETNESS_HEAT_TORCH.set((double) v);
        wetnessHeatTorch = v;
    }

    public static void setWetnessHeatFire(float value) {
        float v = clampFloat(value, 0f, 100f);
        WETNESS_HEAT_FIRE.set((double) v);
        wetnessHeatFire = v;
    }

    public static void setWetnessHeatLava(float value) {
        float v = clampFloat(value, 0f, 100f);
        WETNESS_HEAT_LAVA.set((double) v);
        wetnessHeatLava = v;
    }

    public static void setWetnessHeatRadius(int value) {
        int v = Math.max(1, Math.min(8, value));
        WETNESS_HEAT_RADIUS.set(v);
        wetnessHeatRadius = v;
    }

    public static void setWetnessFreezeSeconds(int value) {
        int v = Math.max(30, Math.min(1800, value));
        WETNESS_FREEZE_SECONDS.set(v);
        wetnessFreezeSeconds = v;
    }

    public static void setWetnessArmorSlow(float value) {
        float v = clampFloat(value, 0f, 1f);
        WETNESS_ARMOR_SLOW.set((double) v);
        wetnessArmorSlow = v;
    }

    public static void setItemRemoverEnabled(boolean enabled) {
        ITEM_REMOVER.set(enabled);
        itemRemoverEnabled = enabled;
    }

    public static boolean isItemBanned(ResourceLocation id) {
        return id != null && bannedItemIds.contains(id.toString().toLowerCase());
    }

    public static List<String> bannedItemIdList() {
        List<String> list = new ArrayList<>(bannedItemIds);
        Collections.sort(list);
        return list;
    }

    public static void setBannedItemIds(Set<String> ids) {
        Set<String> set = new HashSet<>();
        for (String entry : ids) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String key = entry.trim().toLowerCase();
            if (!key.contains(":")) {
                key = "minecraft:" + key;
            }
            if (ResourceLocation.tryParse(key) != null) {
                set.add(key);
            }
        }
        List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        BANNED_ITEMS.set(list);
        bannedItemIds = set;
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void reloadBannedItems(List<? extends String> values) {
        Set<String> set = new HashSet<>();
        for (String entry : values) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String key = entry.trim().toLowerCase();
            if (!key.contains(":")) {
                key = "minecraft:" + key;
            }
            if (ResourceLocation.tryParse(key) != null) {
                set.add(key);
            }
        }
        bannedItemIds = set;
    }

    public static void setHideNamesMode(HideNamesMode mode) {
        HideNamesMode resolved = mode == null ? HideNamesMode.MASKS : mode;
        HIDE_NAMES_MODE.set(resolved.name());
        hideNamesMode = resolved;
        hideNamesAll = resolved == HideNamesMode.FULL;
        hideNamesByItems = resolved == HideNamesMode.MASKS;
        HIDE_NAMES_ALL.set(hideNamesAll);
        HIDE_NAMES_BY_ITEMS.set(hideNamesByItems);
    }

    public static void setHideNamesAll(boolean on) {
        setHideNamesMode(on ? HideNamesMode.FULL : HideNamesMode.MASKS);
    }

    public static void setHideNamesByItems(boolean on) {
        if (on) {
            setHideNamesMode(HideNamesMode.MASKS);
        } else if (hideNamesMode == HideNamesMode.MASKS) {
            setHideNamesMode(HideNamesMode.FULL);
        }
    }

    public static void setHideNamesAdminLocked(boolean locked) {
        HIDE_NAMES_ADMIN_LOCK.set(locked);
        hideNamesAdminLocked = locked;
    }

    public static void addHideNamesItem(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        String key = id.trim().toLowerCase();
        if (!key.contains(":")) {
            key = "minecraft:" + key;
        }
        if (ResourceLocation.tryParse(key) == null) {
            return;
        }
        hideNamesItems.add(key);
        persistHideNamesItems();
    }

    public static void removeHideNamesItem(String id) {
        if (id == null) {
            return;
        }
        hideNamesItems.remove(id.trim().toLowerCase());
        persistHideNamesItems();
    }

    private static void persistHideNamesItems() {
        List<String> list = new ArrayList<>(hideNamesItems);
        list.sort(String::compareToIgnoreCase);
        HIDE_NAMES_ITEMS.set(list);
    }

    private static void reloadHideNamesItems(List<? extends String> values) {
        Set<String> set = new HashSet<>();
        for (String entry : values) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String key = entry.trim().toLowerCase();
            if (!key.contains(":")) {
                key = "minecraft:" + key;
            }
            if (ResourceLocation.tryParse(key) != null) {
                set.add(key);
            }
        }
        hideNamesItems = set;
    }

    public static void setF3HideAdminLocked(boolean locked) {
        F3_HIDE_ADMIN_LOCK.set(locked);
        f3HideAdminLocked = locked;
    }

    public static boolean isF3ElementHidden(F3Element element) {
        return f3HiddenElements.contains(element.configKey());
    }

    public static void setF3ElementHidden(F3Element element, boolean hidden) {
        if (hidden) {
            f3HiddenElements.add(element.configKey());
        } else {
            f3HiddenElements.remove(element.configKey());
        }
        persistF3Hidden();
    }

    /** Hide or show every F3 line group in one write. */
    public static void setAllF3ElementsHidden(boolean hidden) {
        if (hidden) {
            for (F3Element element : F3Element.values()) {
                f3HiddenElements.add(element.configKey());
            }
        } else {
            f3HiddenElements.clear();
        }
        persistF3Hidden();
    }

    public static void setF3CategoryHidden(F3Category category, boolean hidden) {
        for (F3Element element : F3Element.values()) {
            if (element.category() == category) {
                if (hidden) {
                    f3HiddenElements.add(element.configKey());
                } else {
                    f3HiddenElements.remove(element.configKey());
                }
            }
        }
        persistF3Hidden();
    }

    private static void persistF3Hidden() {
        List<String> list = new ArrayList<>(f3HiddenElements);
        list.sort(String::compareTo);
        F3_HIDDEN.set(list);
    }

    private static void reloadF3Hidden(List<? extends String> values) {
        Set<String> set = new HashSet<>();
        for (String entry : values) {
            F3Element element = F3Element.byConfigKey(entry);
            if (element != null) {
                set.add(element.configKey());
            }
        }
        f3HiddenElements = set;
    }

    public static boolean isHudElementHidden(HudElement element) {
        return switch (element) {
            case HEARTS -> hudHideHearts;
            case HUNGER -> hudHideHunger;
            case ARMOR -> hudHideArmor;
            case HOTBAR -> hudHideHotbar;
            case EXPERIENCE -> hudHideExperience;
            case EFFECTS -> hudHideEffects;
            case CROSSHAIR -> hudHideCrosshair;
        };
    }

    public static void setHudElementHidden(HudElement element, boolean hidden) {
        switch (element) {
            case HEARTS -> {
                HUD_HIDE_HEARTS.set(hidden);
                hudHideHearts = hidden;
            }
            case HUNGER -> {
                HUD_HIDE_HUNGER.set(hidden);
                hudHideHunger = hidden;
            }
            case ARMOR -> {
                HUD_HIDE_ARMOR.set(hidden);
                hudHideArmor = hidden;
            }
            case HOTBAR -> {
                HUD_HIDE_HOTBAR.set(hidden);
                hudHideHotbar = hidden;
            }
            case EXPERIENCE -> {
                HUD_HIDE_EXPERIENCE.set(hidden);
                hudHideExperience = hidden;
            }
            case EFFECTS -> {
                HUD_HIDE_EFFECTS.set(hidden);
                hudHideEffects = hidden;
            }
            case CROSSHAIR -> {
                HUD_HIDE_CROSSHAIR.set(hidden);
                hudHideCrosshair = hidden;
            }
        }
    }

    public static boolean isHudShowOnChange(HudElement element) {
        return switch (element) {
            case HEARTS -> hudShowOnChangeHearts;
            case HUNGER -> hudShowOnChangeHunger;
            case ARMOR -> hudShowOnChangeArmor;
            case HOTBAR -> hudShowOnChangeHotbar;
            case EXPERIENCE -> hudShowOnChangeExperience;
            case EFFECTS -> hudShowOnChangeEffects;
            case CROSSHAIR -> false;
        };
    }

    public static void setHudShowOnChange(HudElement element, boolean on) {
        switch (element) {
            case HEARTS -> {
                HUD_SOC_HEARTS.set(on);
                hudShowOnChangeHearts = on;
            }
            case HUNGER -> {
                HUD_SOC_HUNGER.set(on);
                hudShowOnChangeHunger = on;
            }
            case ARMOR -> {
                HUD_SOC_ARMOR.set(on);
                hudShowOnChangeArmor = on;
            }
            case HOTBAR -> {
                HUD_SOC_HOTBAR.set(on);
                hudShowOnChangeHotbar = on;
            }
            case EXPERIENCE -> {
                HUD_SOC_EXPERIENCE.set(on);
                hudShowOnChangeExperience = on;
            }
            case EFFECTS -> {
                HUD_SOC_EFFECTS.set(on);
                hudShowOnChangeEffects = on;
            }
            case CROSSHAIR -> {
            }
        }
    }

    public static void setBorderDangerEnabled(boolean enabled) {
        BORDER_DANGER.set(enabled);
        borderDangerEnabled = enabled;
    }

    public static void setBorderDangerDistance(int distance) {
        int clamped = Math.max(1, Math.min(512, distance));
        BORDER_DISTANCE.set(clamped);
        borderDangerDistance = clamped;
        if (borderVisualDistance < clamped) {
            setBorderVisualDistance(clamped);
        }
        if (borderApproachDistance < borderVisualDistance) {
            setBorderApproachDistance(borderVisualDistance);
        }
    }

    public static void setBorderVisualDistance(int distance) {
        int clamped = Math.max(1, Math.min(512, distance));
        if (clamped < borderDangerDistance) {
            clamped = borderDangerDistance;
        }
        BORDER_VISUAL_DISTANCE.set(clamped);
        borderVisualDistance = clamped;
        if (borderApproachDistance < clamped) {
            setBorderApproachDistance(clamped);
        }
    }

    public static void setBorderApproachDistance(int distance) {
        int clamped = Math.max(1, Math.min(512, distance));
        if (clamped < borderVisualDistance) {
            clamped = borderVisualDistance;
        }
        BORDER_APPROACH_DISTANCE.set(clamped);
        borderApproachDistance = clamped;
    }

    public static void setBorderHarmMode(BorderHarmMode mode) {
        BORDER_HARM_MODE.set(mode.name());
        borderHarmMode = mode;
    }

    public static void setBorderEffects(Set<String> effects) {
        List<String> list = new ArrayList<>();
        for (String key : effects) {
            String normalized = key.trim().toLowerCase();
            if (BorderEffectOptions.ALL.containsKey(normalized)) {
                list.add(normalized);
            }
        }
        Collections.sort(list);
        BORDER_EFFECTS.set(list);
        borderEffects = new HashSet<>(list);
    }

    public static void toggleBorderEffect(String key) {
        String normalized = key.trim().toLowerCase();
        if (!BorderEffectOptions.ALL.containsKey(normalized)) {
            return;
        }
        Set<String> next = new HashSet<>(borderEffects);
        if (!next.add(normalized)) {
            next.remove(normalized);
        }
        setBorderEffects(next);
    }

    public static void setBorderShowWarning(boolean show) {
        BORDER_WARNING.set(show);
        borderShowWarning = show;
    }

    public static void setBorderWarningStyle(BorderWarningStyle style) {
        BORDER_WARN_STYLE.set(style.name());
        borderWarningStyle = style;
    }

    public static void setBorderHideVanillaRedTint(boolean hide) {
        BORDER_HIDE_VANILLA_RED.set(hide);
        borderHideVanillaRedTint = hide;
    }

    public static void setBorderWarnColorApproachA(int rgb) {
        borderWarnColorApproachA = rgb & 0xFFFFFF;
        BORDER_WARN_COLOR_APPROACH_A.set(toHex(borderWarnColorApproachA));
    }

    public static void setBorderWarnColorApproachB(int rgb) {
        borderWarnColorApproachB = rgb & 0xFFFFFF;
        BORDER_WARN_COLOR_APPROACH_B.set(toHex(borderWarnColorApproachB));
    }

    public static void setBorderWarnColorDangerA(int rgb) {
        borderWarnColorDangerA = rgb & 0xFFFFFF;
        BORDER_WARN_COLOR_DANGER_A.set(toHex(borderWarnColorDangerA));
    }

    public static void setBorderWarnColorDangerB(int rgb) {
        borderWarnColorDangerB = rgb & 0xFFFFFF;
        BORDER_WARN_COLOR_DANGER_B.set(toHex(borderWarnColorDangerB));
    }

    public static void setBorderWarnTextApproach(String text) {
        borderWarnTextApproach = text == null ? "" : text;
        BORDER_WARN_TEXT_APPROACH.set(borderWarnTextApproach);
    }

    public static void setBorderWarnTextDanger(String text) {
        borderWarnTextDanger = text == null ? "" : text;
        BORDER_WARN_TEXT_DANGER.set(borderWarnTextDanger);
    }

    public static void setBorderBarrierVisual(BorderBarrierVisual visual) {
        BORDER_BARRIER.set(visual.name());
        borderBarrierVisual = visual;
    }

    public static void setBorderZoneFx(Set<String> fx) {
        List<String> list = new ArrayList<>();
        for (String key : fx) {
            if (BorderZoneFx.isValidKey(key)) {
                list.add(key.trim().toLowerCase());
            }
        }
        Collections.sort(list);
        BORDER_ZONE_FX.set(list);
        borderZoneFx = new HashSet<>(list);
    }

    public static void toggleBorderZoneFx(String key) {
        String normalized = key.trim().toLowerCase();
        if (!BorderZoneFx.isValidKey(normalized)) {
            return;
        }
        Set<String> next = new HashSet<>(borderZoneFx);
        if (!next.add(normalized)) {
            next.remove(normalized);
        }
        setBorderZoneFx(next);
    }

    public static void setKeepItemsWhenSlotsShrink(boolean keep) {
        KEEP_ITEMS.set(keep);
        keepItemsWhenSlotsShrink = keep;
    }

    public static List<String> armorBonusStrings() {
        List<String> list = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Integer> entry : armorBonuses.entrySet()) {
            list.add(entry.getKey() + "=" + entry.getValue());
        }
        if (list.isEmpty()) {
            return defaultArmorBonusStrings();
        }
        list.sort(String::compareTo);
        return list;
    }

    public static void setArmorBonusStrings(List<String> values) {
        ARMOR_SLOTS.set(values);
        reloadArmorMap(values);
    }

    public static void setArmorBonus(String itemId, int slots) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        String key = itemId.trim().toLowerCase();
        if (!key.contains(":")) {
            key = "minecraft:" + key;
        }
        ResourceLocation id = ResourceLocation.tryParse(key);
        if (id == null) {
            return;
        }
        int clamped = Math.max(0, Math.min(27, slots));
        Map<ResourceLocation, Integer> next = new HashMap<>(armorBonuses);
        if (clamped <= 0) {
            next.remove(id);
        } else {
            next.put(id, clamped);
        }
        armorBonuses = next;
        ARMOR_SLOTS.set(armorBonusStrings());
    }

    public static void removeArmorBonus(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        String key = itemId.trim().toLowerCase();
        if (!key.contains(":")) {
            key = "minecraft:" + key;
        }
        ResourceLocation id = ResourceLocation.tryParse(key);
        if (id == null) {
            return;
        }
        Map<ResourceLocation, Integer> next = new HashMap<>(armorBonuses);
        if (next.remove(id) != null) {
            armorBonuses = next;
            ARMOR_SLOTS.set(armorBonusStrings());
        }
    }

    public static void resetArmorBonusesToDefault() {
        setArmorBonusStrings(defaultArmorBonusStrings());
    }

    public static boolean isMobSpawnBlocked(ResourceLocation id) {
        return blockedMobIds.contains(id.toString());
    }

    public static List<String> blockedMobIdList() {
        List<String> list = new ArrayList<>(blockedMobIds);
        Collections.sort(list);
        return list;
    }

    public static void setBlockedMobIds(Set<String> ids) {
        List<String> list = new ArrayList<>(ids);
        Collections.sort(list);
        BLOCKED_MOBS.set(list);
        blockedMobIds = new HashSet<>(list);
    }

    public static int bonusFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return bonusFor(stack.getItem()) + yblk.features.compat.FashionClothingModBonus.bonus(stack);
    }

    public static int bonusFor(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null) {
            return 0;
        }
        return armorBonuses.getOrDefault(id, 0);
    }

    private static void reloadArmorMap(List<? extends String> values) {
        Map<ResourceLocation, Integer> map = new HashMap<>();
        for (String entry : values) {
            String[] parts = entry.split("=");
            if (parts.length != 2) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
            if (id == null) {
                continue;
            }
            try {
                map.put(id, Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        armorBonuses = map;
    }

    private static void reloadBlockedMobs(List<? extends String> values) {
        Set<String> set = new HashSet<>();
        for (String entry : values) {
            ResourceLocation id = ResourceLocation.tryParse(entry.trim());
            if (id != null) {
                set.add(id.toString());
            }
        }
        blockedMobIds = set;
    }

    private static void reloadBorderEffects(List<? extends String> values) {
        Set<String> set = new HashSet<>();
        for (String entry : values) {
            String key = entry.trim().toLowerCase();
            if (BorderEffectOptions.ALL.containsKey(key)) {
                set.add(key);
            }
        }
        borderEffects = set;
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        inventoryRevampEnabled = INVENTORY_REVAMP.get();
        mobSpawnBlockEnabled = MOB_SPAWN_BLOCK.get();
        mobSpawnBlockAllSources = MOB_SPAWN_BLOCK_ALL.get();
        borderDangerEnabled = BORDER_DANGER.get();
        borderDangerDistance = BORDER_DISTANCE.get();
        borderVisualDistance = Math.max(BORDER_VISUAL_DISTANCE.get(), borderDangerDistance);
        borderApproachDistance = Math.max(BORDER_APPROACH_DISTANCE.get(), borderVisualDistance);
        borderHarmMode = BorderHarmMode.fromConfig(BORDER_HARM_MODE.get());
        reloadBorderEffects(BORDER_EFFECTS.get());
        borderWarningStyle = BorderWarningStyle.fromConfig(BORDER_WARN_STYLE.get());
        borderShowWarning = BORDER_WARNING.get();
        // Old configs stored OFF in warningStyle — treat as warnings disabled.
        if ("OFF".equalsIgnoreCase(BORDER_WARN_STYLE.get())) {
            borderShowWarning = false;
        }
        borderHideVanillaRedTint = BORDER_HIDE_VANILLA_RED.get();
        borderWarnColorApproachA = parseHex(BORDER_WARN_COLOR_APPROACH_A.get(), 0xFFFF55);
        borderWarnColorApproachB = parseHex(BORDER_WARN_COLOR_APPROACH_B.get(), 0xFFD700);
        borderWarnColorDangerA = parseHex(BORDER_WARN_COLOR_DANGER_A.get(), 0xFFD700);
        borderWarnColorDangerB = parseHex(BORDER_WARN_COLOR_DANGER_B.get(), 0xFF3333);
        borderWarnTextApproach = BORDER_WARN_TEXT_APPROACH.get() == null ? "" : BORDER_WARN_TEXT_APPROACH.get();
        borderWarnTextDanger = BORDER_WARN_TEXT_DANGER.get() == null ? "" : BORDER_WARN_TEXT_DANGER.get();
        borderBarrierVisual = BorderBarrierVisual.fromConfig(BORDER_BARRIER.get());
        reloadBorderZoneFx(BORDER_ZONE_FX.get());
        keepItemsWhenSlotsShrink = KEEP_ITEMS.get();
        reloadArmorMap(ARMOR_SLOTS.get());
        reloadBlockedMobs(BLOCKED_MOBS.get());
        hudHideEnabled = HUD_HIDE.get();
        hudHideAdminLocked = HUD_HIDE_ADMIN_LOCK.get();
        hudHideHearts = HUD_HIDE_HEARTS.get();
        hudHideHunger = HUD_HIDE_HUNGER.get();
        hudHideArmor = HUD_HIDE_ARMOR.get();
        hudHideHotbar = HUD_HIDE_HOTBAR.get();
        hudHideCrosshair = HUD_HIDE_CROSSHAIR.get();
        hudHideExperience = HUD_HIDE_EXPERIENCE.get();
        hudHideEffects = HUD_HIDE_EFFECTS.get();
        hudShowOnChangeHearts = HUD_SOC_HEARTS.get();
        hudShowOnChangeHunger = HUD_SOC_HUNGER.get();
        hudShowOnChangeArmor = HUD_SOC_ARMOR.get();
        hudShowOnChangeHotbar = HUD_SOC_HOTBAR.get();
        hudShowOnChangeExperience = HUD_SOC_EXPERIENCE.get();
        hudShowOnChangeEffects = HUD_SOC_EFFECTS.get();
        hudHideStyle = HudHideStyle.fromConfig(HUD_HIDE_STYLE.get());
        f3HideEnabled = F3_HIDE.get();
        f3HideAdminLocked = F3_HIDE_ADMIN_LOCK.get();
        reloadF3Hidden(F3_HIDDEN.get());
        hideNamesEnabled = HIDE_NAMES.get();
        hideNamesMode = HideNamesMode.fromConfig(HIDE_NAMES_MODE.get());
        hideNamesAll = HIDE_NAMES_ALL.get();
        hideNamesByItems = HIDE_NAMES_BY_ITEMS.get();
        // Migrate legacy toggles when mode still at default MASKS but hideAll was on.
        if (hideNamesAll && hideNamesMode == HideNamesMode.MASKS
                && "MASKS".equalsIgnoreCase(HIDE_NAMES_MODE.get())) {
            hideNamesMode = HideNamesMode.FULL;
            HIDE_NAMES_MODE.set(HideNamesMode.FULL.name());
        } else if (!hideNamesByItems && hideNamesMode == HideNamesMode.MASKS
                && !hideNamesAll && "MASKS".equalsIgnoreCase(HIDE_NAMES_MODE.get())) {
            // Old configs with byItems=false and hideAll=false → keep MASKS list ready, mode FULL off.
            // Leave as MASKS (feature off until enabled); no change.
        }
        hideNamesFadeNear = HIDE_NAMES_FADE_NEAR.get().floatValue();
        hideNamesFadeFar = HIDE_NAMES_FADE_FAR.get().floatValue();
        if (hideNamesFadeFar <= hideNamesFadeNear) {
            hideNamesFadeFar = hideNamesFadeNear + 1F;
        }
        hideNamesLookDistance = HIDE_NAMES_LOOK_DISTANCE.get().floatValue();
        hideNamesLookAngle = HIDE_NAMES_LOOK_ANGLE.get().floatValue();
        hideNamesAdminLocked = HIDE_NAMES_ADMIN_LOCK.get();
        reloadHideNamesItems(HIDE_NAMES_ITEMS.get());
        realFirstPersonEnabled = REAL_FIRST_PERSON.get();
        rfpHideHead = RFP_HIDE_HEAD.get();
        rfpRenderStuck = RFP_RENDER_STUCK.get();
        rfpHandMode = RfpHandMode.fromConfig(RFP_HAND_MODE.get());
        rfpOffsetStand = RFP_OFFSET_STAND.get().floatValue();
        rfpOffsetSneak = RFP_OFFSET_SNEAK.get().floatValue();
        rfpOffsetSit = RFP_OFFSET_SIT.get().floatValue();
        rfpOffsetSwim = RFP_OFFSET_SWIM.get().floatValue();
        rfpOffsetElytra = RFP_OFFSET_ELYTRA.get().floatValue();
        trueNightEnabled = TRUE_NIGHT.get();
        trueNightMoonPhases = TRUE_NIGHT_MOON_PHASES.get();
        trueNightDarkness = TRUE_NIGHT_DARKNESS.get().floatValue();
        trueNightFogEnabled = TRUE_NIGHT_FOG.get();
        trueNightFogChance = TRUE_NIGHT_FOG_CHANCE.get().floatValue();
        trueNightFogMin = TRUE_NIGHT_FOG_MIN.get().floatValue();
        trueNightFogMax = TRUE_NIGHT_FOG_MAX.get().floatValue();
        localChatEnabled = LOCAL_CHAT.get();
        localChatRange = LOCAL_CHAT_RANGE.get();
        localChatAdminBypass = LOCAL_CHAT_ADMIN_BYPASS.get();
        wetnessEnabled = WETNESS.get();
        wetnessRainRate = WETNESS_RAIN.get().floatValue();
        wetnessDryRate = WETNESS_DRY.get().floatValue();
        wetnessWaterEnterBare = WETNESS_WATER_ENTER_BARE.get().floatValue();
        wetnessWaterEnterFull = WETNESS_WATER_ENTER_FULL.get().floatValue();
        wetnessWaterRateBare = WETNESS_WATER_RATE_BARE.get().floatValue();
        wetnessWaterRateFull = WETNESS_WATER_RATE_FULL.get().floatValue();
        wetnessHeatTorch = WETNESS_HEAT_TORCH.get().floatValue();
        wetnessHeatFire = WETNESS_HEAT_FIRE.get().floatValue();
        wetnessHeatLava = WETNESS_HEAT_LAVA.get().floatValue();
        wetnessHeatRadius = WETNESS_HEAT_RADIUS.get();
        wetnessFreezeSeconds = WETNESS_FREEZE_SECONDS.get();
        wetnessArmorSlow = WETNESS_ARMOR_SLOW.get().floatValue();
        itemRemoverEnabled = ITEM_REMOVER.get();
        reloadBannedItems(BANNED_ITEMS.get());
    }

    private static void reloadBorderZoneFx(List<? extends String> values) {
        Set<String> set = new HashSet<>();
        for (String entry : values) {
            String key = entry.trim().toLowerCase();
            if (BorderZoneFx.isValidKey(key)) {
                set.add(key);
            }
        }
        borderZoneFx = set;
    }

    public static String toHex(int rgb) {
        return String.format("%06X", rgb & 0xFFFFFF);
    }

    public static int parseHex(String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
