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

    private static final ForgeConfigSpec.BooleanValue TRUE_NIGHT_ADMIN_LOCK = BUILDER
            .comment("If true, only operators (permission level 2+) can change True Night.")
            .define("trueNight.adminLocked", false);

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
            .comment("Item IDs banned when item remover is enabled. A plain id removes the item from inventories, recipes, and loot. Suffix # with i, r, l turns those off: minecraft:tnt#i is inventory only.")
            .defineListAllowEmpty(List.of("itemRemover", "bannedItems"), ArrayList::new, FeaturesConfig::validateBannedEntry);

    private static final ForgeConfigSpec.BooleanValue COMMAND_BLOCKER = BUILDER
            .comment("Hide and deny listed commands by permission role.")
            .define("features.commandBlocker", false);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> COMMAND_RULES = BUILDER
            .comment("Blocked commands. Prefix roles with p, m, o and a colon: p:gamemode blocks players, pm:give * blocks players and moderators. A bare pattern defaults to players. * matches one token; a token ending in * is a prefix.")
            .defineListAllowEmpty(List.of("commandBlocker", "rules"), ArrayList::new, FeaturesConfig::validateShortText);

    private static final ForgeConfigSpec.ConfigValue<String> COMMAND_DENY = BUILDER
            .comment("Custom deny message. Empty uses the translation. {command} is replaced with what was typed.")
            .define("commandBlocker.message", "");

    private static final ForgeConfigSpec.BooleanValue INTERACTION_BLOCKER = BUILDER
            .comment("Block opening, using, right-clicking, or breaking listed blocks and entities.")
            .define("features.interactionBlocker", false);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> INTERACTION_RULES = BUILDER
            .comment("b:id, e:id, bt:tag, or et:tag. Optional |open,use,right,break. All four flags when the suffix is omitted.")
            .defineListAllowEmpty(List.of("interactionBlocker", "rules"), ArrayList::new, FeaturesConfig::validateShortText);

    private static final ForgeConfigSpec.ConfigValue<String> INTERACTION_MESSAGE = BUILDER
            .comment("Custom deny message. Empty uses the translation.")
            .define("interactionBlocker.message", "");

    private static final ForgeConfigSpec.BooleanValue INTERACTION_OP_BYPASS = BUILDER
            .comment("Operators (permission level 2+) ignore the interaction blocker.")
            .define("interactionBlocker.opBypass", true);

    private static final ForgeConfigSpec.BooleanValue TOOL_RESTRICTIONS = BUILDER
            .comment("Blocks in mineable/* break only with the matching tool.")
            .define("features.toolRestrictions", false);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> TOOL_EXCEPTIONS = BUILDER
            .comment("Block ids or #tags that ignore the tool rule.")
            .defineListAllowEmpty(List.of("toolRestrictions", "exceptions"), ArrayList::new, FeaturesConfig::validateShortText);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> TOOL_HAND = BUILDER
            .comment("Block ids or #tags an empty hand may break.")
            .defineListAllowEmpty(List.of("toolRestrictions", "hand"), ArrayList::new, FeaturesConfig::validateShortText);

    private static final ForgeConfigSpec.ConfigValue<String> TOOL_MESSAGE = BUILDER
            .comment("Custom deny message. Empty uses the translation. {block} is replaced with the block name.")
            .define("toolRestrictions.message", "");

    private static final ForgeConfigSpec.BooleanValue TOOL_SOUND = BUILDER
            .comment("Play a soft deny sound when the tool rule rejects a break.")
            .define("toolRestrictions.sound", true);

    private static final ForgeConfigSpec.BooleanValue PORTAL_LOCK = BUILDER
            .comment("Lock travel into listed dimensions. Empty list allows every portal.")
            .define("features.portalLock", false);

    private static final ForgeConfigSpec.BooleanValue PORTAL_BLOCK_CREATION = BUILDER
            .comment("Also stop nether portals from lighting and eyes of ender from completing the end portal.")
            .define("portalLock.blockCreation", false);

    private static final ForgeConfigSpec.BooleanValue PORTAL_OP_BYPASS = BUILDER
            .comment("Operators (permission level 2+) ignore portal lock.")
            .define("portalLock.opBypass", true);

    private static final ForgeConfigSpec.ConfigValue<String> PORTAL_MESSAGE = BUILDER
            .comment("Custom deny message. Empty uses the translation. {dimension} is replaced.")
            .define("portalLock.message", "");

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> PORTAL_RULES = BUILDER
            .comment("dimension allow|block|day <n>|date <YYYY-MM-DD>. Shorthand: nether, end, overworld.")
            .defineListAllowEmpty(List.of("portalLock", "rules"), ArrayList::new, FeaturesConfig::validateShortText);

    private static final ForgeConfigSpec.BooleanValue ELYTRA_RULES = BUILDER
            .comment("Per-dimension elytra, firework boost, glide limit, and creative-style flight.")
            .define("features.elytraRules", false);

    private static final ForgeConfigSpec.BooleanValue ELYTRA_OP_BYPASS = BUILDER
            .comment("Operators (permission level 2+) ignore elytra and flight rules.")
            .define("elytraRules.opBypass", true);

    private static final ForgeConfigSpec.ConfigValue<String> ELYTRA_MESSAGE = BUILDER
            .comment("Custom deny message. Empty uses the translation. {dimension} and {seconds} are replaced.")
            .define("elytraRules.message", "");

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ELYTRA_RULE_LINES = BUILDER
            .comment("dimension elytra firework seconds flight. elytra, firework, and flight are allow or deny. 0 seconds means no glide limit.")
            .defineListAllowEmpty(List.of("elytraRules", "rules"), ArrayList::new, FeaturesConfig::validateShortText);

    private static final ForgeConfigSpec.BooleanValue MOB_DROPS = BUILDER
            .comment("Add, remove, or replace mob drops, and scale experience.")
            .define("features.mobDrops", false);

    private static final ForgeConfigSpec.BooleanValue MOB_DROPS_CLEAR = BUILDER
            .comment("Clear the vanilla drop table before the extra rules run.")
            .define("mobDrops.clearVanilla", false);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_DROP_RULES = BUILDER
            .comment("add|remove|replace|xp <entity|#tag|@mod> ... Optional trailing player limits the rule to player kills.")
            .defineListAllowEmpty(List.of("mobDrops", "rules"), ArrayList::new, FeaturesConfig::validateShortText);

    private static final ForgeConfigSpec.BooleanValue VILLAGER_TRADES = BUILDER
            .comment("Block villager and wandering-trader trades, or strip specific results.")
            .define("features.villagerTrades", false);

    private static final ForgeConfigSpec.BooleanValue VILLAGER_BLOCK_ALL = BUILDER
            .define("villagerTrades.blockAll", false);

    private static final ForgeConfigSpec.BooleanValue VILLAGER_BLOCK_WANDERING = BUILDER
            .define("villagerTrades.blockWandering", false);

    private static final ForgeConfigSpec.BooleanValue VILLAGER_FREEZE_PRICES = BUILDER
            .comment("Ignore hero-of-the-village and curing discounts.")
            .define("villagerTrades.freezePrices", false);

    private static final ForgeConfigSpec.ConfigValue<String> VILLAGER_MESSAGE = BUILDER
            .define("villagerTrades.message", "");

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> VILLAGER_PROFESSIONS = BUILDER
            .comment("Profession ids or @mod. Those villagers cannot trade.")
            .defineListAllowEmpty(List.of("villagerTrades", "professions"), ArrayList::new, FeaturesConfig::validateShortText);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> VILLAGER_RESULTS = BUILDER
            .comment("Result item ids, #tags, or @mod removed from every trade.")
            .defineListAllowEmpty(List.of("villagerTrades", "results"), ArrayList::new, FeaturesConfig::validateShortText);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> VILLAGER_CUSTOMS = BUILDER
            .comment("Extra offers: profession costA count costB count result count. costB may be - 0. wandering for the trader.")
            .defineListAllowEmpty(List.of("villagerTrades", "custom"), ArrayList::new, FeaturesConfig::validateShortText);

    private static final ForgeConfigSpec.BooleanValue ITEM_WEIGHT = BUILDER
            .comment("Carried items have weight. Too much slows the player, stops sprinting, and raises hunger.")
            .define("features.itemWeight", false);

    private static final ForgeConfigSpec.DoubleValue ITEM_WEIGHT_CAPACITY = BUILDER
            .defineInRange("itemWeight.capacity", 60.0, 1.0, 10000.0);

    private static final ForgeConfigSpec.DoubleValue ITEM_WEIGHT_SLOW = BUILDER
            .defineInRange("itemWeight.slowAt", 0.45, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue ITEM_WEIGHT_SPRINT = BUILDER
            .defineInRange("itemWeight.sprintAt", 0.70, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue ITEM_WEIGHT_HUNGER = BUILDER
            .defineInRange("itemWeight.hungerAt", 0.85, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue ITEM_WEIGHT_WET = BUILDER
            .comment("Armor weight multiplier at full Wetness, if that module is on.")
            .defineInRange("itemWeight.wetMultiplier", 1.75, 1.0, 5.0);

    private static final ForgeConfigSpec.DoubleValue ITEM_WEIGHT_SLOT = BUILDER
            .comment("Extra carry capacity for each Inventory Revamp slot past the vanilla inventory.")
            .defineInRange("itemWeight.slotBonus", 4.0, 0.0, 100.0);

    private static final ForgeConfigSpec.DoubleValue ITEM_WEIGHT_STRENGTH = BUILDER
            .comment("Extra capacity per Strength level. 0.5 means Strength I carries 50% more.")
            .defineInRange("itemWeight.strength", 0.5, 0.0, 3.0);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_WEIGHT_OVERRIDES = BUILDER
            .comment("id or #tag, then weight per item. Example: minecraft:anvil 12")
            .defineListAllowEmpty(List.of("itemWeight", "overrides"), ArrayList::new, FeaturesConfig::validateShortText);

    private static final ForgeConfigSpec.BooleanValue SERVER_TWEAKS = BUILDER
            .comment("MOTD, tab list, kits, roles, moderation, and basic checks.")
            .define("features.serverTweaks", false);
    private static final ForgeConfigSpec.ConfigValue<String> SERVER_MOTD = BUILDER.define("serverTweaks.motd", "");
    private static final ForgeConfigSpec.ConfigValue<String> SERVER_JOIN = BUILDER.define("serverTweaks.join", "");
    private static final ForgeConfigSpec.ConfigValue<String> SERVER_LEAVE = BUILDER.define("serverTweaks.leave", "");
    private static final ForgeConfigSpec.ConfigValue<String> SERVER_WELCOME = BUILDER.define("serverTweaks.welcome", "");
    private static final ForgeConfigSpec.ConfigValue<String> SERVER_WELCOME_SUB = BUILDER.define("serverTweaks.welcomeSub", "");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SERVER_RULES = BUILDER
            .defineListAllowEmpty(List.of("serverTweaks", "rules"), ArrayList::new, FeaturesConfig::validateShortText);
    private static final ForgeConfigSpec.ConfigValue<String> SERVER_TAB_HEADER = BUILDER.define("serverTweaks.tabHeader", "");
    private static final ForgeConfigSpec.ConfigValue<String> SERVER_TAB_FOOTER = BUILDER.define("serverTweaks.tabFooter", "");
    private static final ForgeConfigSpec.ConfigValue<String> SERVER_STARTER_KIT = BUILDER.define("serverTweaks.starterKit", "");
    private static final ForgeConfigSpec.ConfigValue<String> SERVER_CHECK_ACTION = BUILDER.define("serverTweaks.checkAction", "log");
    private static final ForgeConfigSpec.ConfigValue<String> SERVER_CHECK_ACTIONS = BUILDER
            .comment("Per check: log, notify, or kick. Seven words: fly speed reach fall break xray dupe. Empty uses checkAction.")
            .define("serverTweaks.checkActions", "");
    private static final ForgeConfigSpec.BooleanValue SERVER_CHECK_FLY = BUILDER.define("serverTweaks.checkFly", false);
    private static final ForgeConfigSpec.BooleanValue SERVER_CHECK_SPEED = BUILDER.define("serverTweaks.checkSpeed", false);
    private static final ForgeConfigSpec.BooleanValue SERVER_CHECK_REACH = BUILDER.define("serverTweaks.checkReach", false);
    private static final ForgeConfigSpec.BooleanValue SERVER_CHECK_FALL = BUILDER.define("serverTweaks.checkFall", false);
    private static final ForgeConfigSpec.BooleanValue SERVER_CHECK_BREAK = BUILDER.define("serverTweaks.checkBreak", false);
    private static final ForgeConfigSpec.BooleanValue SERVER_CHECK_XRAY = BUILDER.define("serverTweaks.checkXray", false);
    private static final ForgeConfigSpec.BooleanValue SERVER_CHECK_DUPE = BUILDER.define("serverTweaks.checkDupe", false);
    private static final ForgeConfigSpec.BooleanValue SERVER_HIDE_PING = BUILDER.define("serverTweaks.hidePing", false);
    private static final ForgeConfigSpec.BooleanValue SERVER_SORT_TAB = BUILDER.define("serverTweaks.sortTab", true);
    private static final ForgeConfigSpec.BooleanValue SERVER_ROLE_PREFIX = BUILDER.define("serverTweaks.rolePrefix", true);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SERVER_KITS = BUILDER
            .defineListAllowEmpty(List.of("serverTweaks", "kits"), ArrayList::new, FeaturesConfig::validateShortText);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SERVER_ROLES = BUILDER
            .defineListAllowEmpty(List.of("serverTweaks", "roles"), ArrayList::new, FeaturesConfig::validateShortText);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SERVER_MUTES = BUILDER
            .defineListAllowEmpty(List.of("serverTweaks", "mutes"), ArrayList::new, FeaturesConfig::validateShortText);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SERVER_SEEN = BUILDER
            .defineListAllowEmpty(List.of("serverTweaks", "seen"), ArrayList::new, FeaturesConfig::validateShortText);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SERVER_LOG = BUILDER
            .defineListAllowEmpty(List.of("serverTweaks", "log"), ArrayList::new, FeaturesConfig::validateShortText);

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
    public static boolean trueNightAdminLocked;

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
    public static Map<String, ItemBan> itemBans = new HashMap<>();

    public static boolean commandBlockerEnabled;
    public static List<String> commandRules = new ArrayList<>();
    public static String commandDenyMessage = "";

    public static boolean interactionBlockerEnabled;
    public static List<String> interactionRules = new ArrayList<>();
    public static String interactionMessage = "";
    public static boolean interactionOpBypass = true;

    public static boolean toolRestrictionsEnabled;
    public static List<String> toolExceptions = new ArrayList<>();
    public static List<String> toolHand = new ArrayList<>();
    public static String toolMessage = "";
    public static boolean toolSound = true;

    public static boolean portalLockEnabled;
    public static boolean portalBlockCreation;
    public static boolean portalOpBypass = true;
    public static String portalMessage = "";
    public static List<String> portalRules = new ArrayList<>();

    public static boolean elytraRulesEnabled;
    public static boolean elytraOpBypass = true;
    public static String elytraMessage = "";
    public static List<String> elytraRules = new ArrayList<>();
    public static boolean mobDropsEnabled;
    public static boolean mobDropsClearVanilla;
    public static List<String> mobDropRules = new ArrayList<>();
    public static boolean villagerTradesEnabled;
    public static boolean villagerBlockAll;
    public static boolean villagerBlockWandering;
    public static boolean villagerFreezePrices;
    public static String villagerMessage = "";
    public static List<String> villagerProfessions = new ArrayList<>();
    public static List<String> villagerResults = new ArrayList<>();
    public static List<String> villagerCustoms = new ArrayList<>();
    public static boolean itemWeightEnabled;
    public static float itemWeightCapacity = 60f;
    public static float itemWeightSlow = 0.45f;
    public static float itemWeightSprint = 0.7f;
    public static float itemWeightHunger = 0.85f;
    public static float itemWeightWet = 1.75f;
    public static float itemWeightSlotBonus = 4f;
    public static float itemWeightStrength = 0.5f;
    public static boolean serverTweaksEnabled;
    public static String serverMotd = "";
    public static String serverJoin = "";
    public static String serverLeave = "";
    public static String serverWelcome = "";
    public static String serverWelcomeSub = "";
    public static List<String> serverRules = new ArrayList<>();
    public static String serverTabHeader = "";
    public static String serverTabFooter = "";
    public static String serverStarterKit = "";
    public static String serverCheckAction = "log";
    public static String serverCheckFlyAction = "log";
    public static String serverCheckSpeedAction = "log";
    public static String serverCheckReachAction = "log";
    public static String serverCheckFallAction = "log";
    public static String serverCheckBreakAction = "log";
    public static String serverCheckXrayAction = "log";
    public static String serverCheckDupeAction = "log";
    public static boolean serverCheckFly;
    public static boolean serverCheckSpeed;
    public static boolean serverCheckReach;
    public static boolean serverCheckFall;
    public static boolean serverCheckBreak;
    public static boolean serverCheckXray;
    public static boolean serverCheckDupe;
    public static boolean serverHidePing;
    public static boolean serverSortTab = true;
    public static boolean serverRolePrefix = true;
    public static List<String> serverKits = new ArrayList<>();
    public static List<String> serverRoles = new ArrayList<>();
    public static List<String> serverMutes = new ArrayList<>();
    public static List<String> serverSeen = new ArrayList<>();
    public static List<String> serverLog = new ArrayList<>();
    public static List<String> serverInvView = new ArrayList<>();
    /** Client-only prompt set by the login packet. Never read on the server path. */
    public static boolean serverTweaksPromptRules;
    public static List<String> itemWeightOverrides = new ArrayList<>();

    /** Per banned item. All three are on when the id is stored without a #suffix. */
    public static final class ItemBan {
        public boolean inventory = true;
        public boolean recipes = true;
        public boolean loot = true;

        public ItemBan copy() {
            ItemBan next = new ItemBan();
            next.inventory = inventory;
            next.recipes = recipes;
            next.loot = loot;
            return next;
        }
    }

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

    private static boolean validateShortText(Object raw) {
        return raw instanceof String s && !s.isBlank() && s.length() <= 256;
    }

    private static boolean validateBannedEntry(Object raw) {
        if (!(raw instanceof String s) || s.isBlank()) {
            return false;
        }
        String id = s;
        int hash = id.indexOf('#');
        if (hash >= 0) {
            id = id.substring(0, hash);
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
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

    public static void setTrueNightAdminLocked(boolean locked) {
        TRUE_NIGHT_ADMIN_LOCK.set(locked);
        trueNightAdminLocked = locked;
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

    public static void setCommandBlockerEnabled(boolean enabled) {
        applyCommandBlocker(enabled, commandRules, commandDenyMessage, true);
    }

    public static void setCommandRules(List<String> rules) {
        applyCommandBlocker(commandBlockerEnabled, rules, commandDenyMessage, true);
    }

    public static void setCommandDenyMessage(String message) {
        applyCommandBlocker(commandBlockerEnabled, commandRules, message, true);
    }

    public static void applyCommandBlocker(boolean enabled, List<String> rules, String message, boolean writeSpec) {
        commandBlockerEnabled = enabled;
        commandRules = cleanText(rules);
        commandDenyMessage = clipText(message);
        if (writeSpec) {
            COMMAND_BLOCKER.set(enabled);
            COMMAND_RULES.set(new ArrayList<>(commandRules));
            COMMAND_DENY.set(commandDenyMessage);
        }
    }

    public static void setInteractionBlockerEnabled(boolean enabled) {
        applyInteractionBlocker(enabled, interactionRules, interactionMessage, interactionOpBypass, true);
    }

    public static void setInteractionRules(List<String> rules) {
        applyInteractionBlocker(interactionBlockerEnabled, rules, interactionMessage, interactionOpBypass, true);
    }

    public static void setInteractionMessage(String message) {
        applyInteractionBlocker(interactionBlockerEnabled, interactionRules, message, interactionOpBypass, true);
    }

    public static void setInteractionOpBypass(boolean bypass) {
        applyInteractionBlocker(interactionBlockerEnabled, interactionRules, interactionMessage, bypass, true);
    }

    public static void applyInteractionBlocker(boolean enabled, List<String> rules, String message,
                                               boolean opBypass, boolean writeSpec) {
        interactionBlockerEnabled = enabled;
        interactionRules = cleanText(rules);
        interactionMessage = clipText(message);
        interactionOpBypass = opBypass;
        if (writeSpec) {
            INTERACTION_BLOCKER.set(enabled);
            INTERACTION_RULES.set(new ArrayList<>(interactionRules));
            INTERACTION_MESSAGE.set(interactionMessage);
            INTERACTION_OP_BYPASS.set(opBypass);
        }
    }

    public static void setToolRestrictionsEnabled(boolean enabled) {
        applyToolRestrictions(enabled, toolExceptions, toolHand, toolMessage, toolSound, true);
    }

    public static void setToolExceptions(List<String> exceptions) {
        applyToolRestrictions(toolRestrictionsEnabled, exceptions, toolHand, toolMessage, toolSound, true);
    }

    public static void setToolHand(List<String> hand) {
        applyToolRestrictions(toolRestrictionsEnabled, toolExceptions, hand, toolMessage, toolSound, true);
    }

    public static void setToolMessage(String message) {
        applyToolRestrictions(toolRestrictionsEnabled, toolExceptions, toolHand, message, toolSound, true);
    }

    public static void setToolSound(boolean sound) {
        applyToolRestrictions(toolRestrictionsEnabled, toolExceptions, toolHand, toolMessage, sound, true);
    }

    public static void applyToolRestrictions(boolean enabled, List<String> exceptions, List<String> hand,
                                             String message, boolean sound, boolean writeSpec) {
        toolRestrictionsEnabled = enabled;
        toolExceptions = cleanText(exceptions);
        toolHand = cleanText(hand);
        toolMessage = clipText(message);
        toolSound = sound;
        if (writeSpec) {
            TOOL_RESTRICTIONS.set(enabled);
            TOOL_EXCEPTIONS.set(new ArrayList<>(toolExceptions));
            TOOL_HAND.set(new ArrayList<>(toolHand));
            TOOL_MESSAGE.set(toolMessage);
            TOOL_SOUND.set(sound);
        }
    }

    public static void setPortalLockEnabled(boolean enabled) {
        applyPortalLock(enabled, portalBlockCreation, portalOpBypass, portalMessage, portalRules, true);
    }

    public static void setPortalBlockCreation(boolean blockCreation) {
        applyPortalLock(portalLockEnabled, blockCreation, portalOpBypass, portalMessage, portalRules, true);
    }

    public static void setPortalOpBypass(boolean opBypass) {
        applyPortalLock(portalLockEnabled, portalBlockCreation, opBypass, portalMessage, portalRules, true);
    }

    public static void setPortalMessage(String message) {
        applyPortalLock(portalLockEnabled, portalBlockCreation, portalOpBypass, message, portalRules, true);
    }

    public static void setPortalRules(List<String> rules) {
        applyPortalLock(portalLockEnabled, portalBlockCreation, portalOpBypass, portalMessage, rules, true);
    }

    public static void applyPortalLock(boolean enabled, boolean blockCreation, boolean opBypass, String message,
                                       List<String> rules, boolean writeSpec) {
        portalLockEnabled = enabled;
        portalBlockCreation = blockCreation;
        portalOpBypass = opBypass;
        portalMessage = clipText(message);
        portalRules = cleanText(rules);
        if (writeSpec) {
            PORTAL_LOCK.set(enabled);
            PORTAL_BLOCK_CREATION.set(blockCreation);
            PORTAL_OP_BYPASS.set(opBypass);
            PORTAL_MESSAGE.set(portalMessage);
            PORTAL_RULES.set(new ArrayList<>(portalRules));
        }
    }

    public static void setElytraRulesEnabled(boolean enabled) {
        applyElytraRules(enabled, elytraOpBypass, elytraMessage, elytraRules, true);
    }

    public static void setElytraOpBypass(boolean opBypass) {
        applyElytraRules(elytraRulesEnabled, opBypass, elytraMessage, elytraRules, true);
    }

    public static void setElytraMessage(String message) {
        applyElytraRules(elytraRulesEnabled, elytraOpBypass, message, elytraRules, true);
    }

    public static void setElytraRules(List<String> rules) {
        applyElytraRules(elytraRulesEnabled, elytraOpBypass, elytraMessage, rules, true);
    }

    public static void applyElytraRules(boolean enabled, boolean opBypass, String message, List<String> rules,
                                        boolean writeSpec) {
        elytraRulesEnabled = enabled;
        elytraOpBypass = opBypass;
        elytraMessage = clipText(message);
        elytraRules = cleanText(rules);
        if (writeSpec) {
            ELYTRA_RULES.set(enabled);
            ELYTRA_OP_BYPASS.set(opBypass);
            ELYTRA_MESSAGE.set(elytraMessage);
            ELYTRA_RULE_LINES.set(new ArrayList<>(elytraRules));
        }
    }

    public static void setMobDropsEnabled(boolean enabled) {
        applyMobDrops(enabled, mobDropsClearVanilla, mobDropRules, true);
    }

    public static void setMobDropsClearVanilla(boolean clear) {
        applyMobDrops(mobDropsEnabled, clear, mobDropRules, true);
    }

    public static void setMobDropRules(List<String> rules) {
        applyMobDrops(mobDropsEnabled, mobDropsClearVanilla, rules, true);
    }

    public static void applyMobDrops(boolean enabled, boolean clearVanilla, List<String> rules, boolean writeSpec) {
        mobDropsEnabled = enabled;
        mobDropsClearVanilla = clearVanilla;
        mobDropRules = cleanText(rules);
        if (writeSpec) {
            MOB_DROPS.set(enabled);
            MOB_DROPS_CLEAR.set(clearVanilla);
            MOB_DROP_RULES.set(new ArrayList<>(mobDropRules));
        }
    }

    public static void setVillagerTradesEnabled(boolean enabled) {
        applyVillagerTrades(enabled, villagerBlockAll, villagerBlockWandering, villagerFreezePrices,
                villagerMessage, villagerProfessions, villagerResults, villagerCustoms, true);
    }

    public static void setVillagerBlockAll(boolean blockAll) {
        applyVillagerTrades(villagerTradesEnabled, blockAll, villagerBlockWandering, villagerFreezePrices,
                villagerMessage, villagerProfessions, villagerResults, villagerCustoms, true);
    }

    public static void setVillagerBlockWandering(boolean blockWandering) {
        applyVillagerTrades(villagerTradesEnabled, villagerBlockAll, blockWandering, villagerFreezePrices,
                villagerMessage, villagerProfessions, villagerResults, villagerCustoms, true);
    }

    public static void setVillagerFreezePrices(boolean freezePrices) {
        applyVillagerTrades(villagerTradesEnabled, villagerBlockAll, villagerBlockWandering, freezePrices,
                villagerMessage, villagerProfessions, villagerResults, villagerCustoms, true);
    }

    public static void setVillagerMessage(String message) {
        applyVillagerTrades(villagerTradesEnabled, villagerBlockAll, villagerBlockWandering, villagerFreezePrices,
                message, villagerProfessions, villagerResults, villagerCustoms, true);
    }

    public static void setVillagerProfessions(List<String> professions) {
        applyVillagerTrades(villagerTradesEnabled, villagerBlockAll, villagerBlockWandering, villagerFreezePrices,
                villagerMessage, professions, villagerResults, villagerCustoms, true);
    }

    public static void setVillagerResults(List<String> results) {
        applyVillagerTrades(villagerTradesEnabled, villagerBlockAll, villagerBlockWandering, villagerFreezePrices,
                villagerMessage, villagerProfessions, results, villagerCustoms, true);
    }

    public static void setVillagerCustoms(List<String> customs) {
        applyVillagerTrades(villagerTradesEnabled, villagerBlockAll, villagerBlockWandering, villagerFreezePrices,
                villagerMessage, villagerProfessions, villagerResults, customs, true);
    }

    public static void applyVillagerTrades(boolean enabled, boolean blockAll, boolean blockWandering, boolean freezePrices,
                                           String message, List<String> professions, List<String> results,
                                           List<String> customs, boolean writeSpec) {
        villagerTradesEnabled = enabled;
        villagerBlockAll = blockAll;
        villagerBlockWandering = blockWandering;
        villagerFreezePrices = freezePrices;
        villagerMessage = clipText(message);
        villagerProfessions = cleanText(professions);
        villagerResults = cleanText(results);
        villagerCustoms = cleanText(customs);
        if (writeSpec) {
            VILLAGER_TRADES.set(enabled);
            VILLAGER_BLOCK_ALL.set(blockAll);
            VILLAGER_BLOCK_WANDERING.set(blockWandering);
            VILLAGER_FREEZE_PRICES.set(freezePrices);
            VILLAGER_MESSAGE.set(villagerMessage);
            VILLAGER_PROFESSIONS.set(new ArrayList<>(villagerProfessions));
            VILLAGER_RESULTS.set(new ArrayList<>(villagerResults));
            VILLAGER_CUSTOMS.set(new ArrayList<>(villagerCustoms));
        }
    }

    public static void setItemWeightEnabled(boolean enabled) {
        applyItemWeight(enabled, itemWeightCapacity, itemWeightSlow, itemWeightSprint, itemWeightHunger,
                itemWeightWet, itemWeightSlotBonus, itemWeightStrength, itemWeightOverrides, true);
    }

    public static void setItemWeightOverrides(List<String> overrides) {
        applyItemWeight(itemWeightEnabled, itemWeightCapacity, itemWeightSlow, itemWeightSprint, itemWeightHunger,
                itemWeightWet, itemWeightSlotBonus, itemWeightStrength, overrides, true);
    }

    public static void applyItemWeight(boolean enabled, float capacity, float slowAt, float sprintAt, float hungerAt,
                                       float wet, float slotBonus, float strength, List<String> overrides, boolean writeSpec) {
        itemWeightEnabled = enabled;
        itemWeightCapacity = Math.max(1f, capacity);
        itemWeightSlow = clamp01(slowAt);
        itemWeightSprint = clamp01(sprintAt);
        itemWeightHunger = clamp01(hungerAt);
        itemWeightWet = Math.max(1f, wet);
        itemWeightSlotBonus = Math.max(0f, slotBonus);
        itemWeightStrength = Math.max(0f, strength);
        itemWeightOverrides = cleanText(overrides);
        if (writeSpec) {
            ITEM_WEIGHT.set(enabled);
            ITEM_WEIGHT_CAPACITY.set((double) itemWeightCapacity);
            ITEM_WEIGHT_SLOW.set((double) itemWeightSlow);
            ITEM_WEIGHT_SPRINT.set((double) itemWeightSprint);
            ITEM_WEIGHT_HUNGER.set((double) itemWeightHunger);
            ITEM_WEIGHT_WET.set((double) itemWeightWet);
            ITEM_WEIGHT_SLOT.set((double) itemWeightSlotBonus);
            ITEM_WEIGHT_STRENGTH.set((double) itemWeightStrength);
            ITEM_WEIGHT_OVERRIDES.set(new ArrayList<>(itemWeightOverrides));
        }
    }

    public static void applyServerTweaks(boolean enabled, String motd, String join, String leave, String welcome,
                                         String welcomeSub, List<String> rules, String header, String footer,
                                         String starter, String checkAction, boolean fly, boolean speed, boolean reach,
                                         boolean fall, boolean fastBreak, boolean xray, boolean dupe, List<String> kits, List<String> roles,
                                         List<String> mutes, List<String> seen, List<String> log, boolean hidePing, boolean sortTab,
                                         boolean rolePrefix, String checkActions, boolean writeSpec) {
        serverTweaksEnabled = enabled;
        serverMotd = clipText(motd);
        serverJoin = clipText(join);
        serverLeave = clipText(leave);
        serverWelcome = clipText(welcome);
        serverWelcomeSub = clipText(welcomeSub);
        serverRules = cleanText(rules);
        serverTabHeader = clipText(header);
        serverTabFooter = clipText(footer);
        serverStarterKit = clipText(starter);
        serverCheckAction = normalizeCheckAction(checkAction);
        loadCheckActions(checkActions);
        serverCheckFly = fly;
        serverCheckSpeed = speed;
        serverCheckReach = reach;
        serverCheckFall = fall;
        serverCheckBreak = fastBreak;
        serverCheckXray = xray;
        serverCheckDupe = dupe;
        serverHidePing = hidePing;
        serverSortTab = sortTab;
        serverRolePrefix = rolePrefix;
        serverKits = cleanText(kits);
        serverRoles = cleanText(roles);
        serverMutes = cleanText(mutes);
        serverSeen = cleanText(seen);
        serverLog = cleanText(log);
        if (serverLog.size() > 40) {
            serverLog = new ArrayList<>(serverLog.subList(serverLog.size() - 40, serverLog.size()));
        }
        if (writeSpec) {
            SERVER_TWEAKS.set(enabled);
            SERVER_MOTD.set(serverMotd);
            SERVER_JOIN.set(serverJoin);
            SERVER_LEAVE.set(serverLeave);
            SERVER_WELCOME.set(serverWelcome);
            SERVER_WELCOME_SUB.set(serverWelcomeSub);
            SERVER_RULES.set(new ArrayList<>(serverRules));
            SERVER_TAB_HEADER.set(serverTabHeader);
            SERVER_TAB_FOOTER.set(serverTabFooter);
            SERVER_STARTER_KIT.set(serverStarterKit);
            SERVER_CHECK_ACTION.set(serverCheckAction);
            SERVER_CHECK_ACTIONS.set(joinCheckActions());
            SERVER_CHECK_FLY.set(fly);
            SERVER_CHECK_SPEED.set(speed);
            SERVER_CHECK_REACH.set(reach);
            SERVER_CHECK_FALL.set(fall);
            SERVER_CHECK_BREAK.set(fastBreak);
            SERVER_CHECK_XRAY.set(xray);
            SERVER_CHECK_DUPE.set(dupe);
            SERVER_HIDE_PING.set(hidePing);
            SERVER_SORT_TAB.set(sortTab);
            SERVER_ROLE_PREFIX.set(rolePrefix);
            SERVER_KITS.set(new ArrayList<>(serverKits));
            SERVER_ROLES.set(new ArrayList<>(serverRoles));
            SERVER_MUTES.set(new ArrayList<>(serverMutes));
            SERVER_SEEN.set(new ArrayList<>(serverSeen));
            SERVER_LOG.set(new ArrayList<>(serverLog));
        }
    }

    public static String normalizeCheckAction(String raw) {
        if (raw == null) {
            return "log";
        }
        return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "kick" -> "kick";
            case "notify", "staff" -> "notify";
            default -> "log";
        };
    }

    public static String checkActionFor(String check) {
        String raw = switch (check == null ? "" : check) {
            case "fly" -> serverCheckFlyAction;
            case "speed" -> serverCheckSpeedAction;
            case "reach" -> serverCheckReachAction;
            case "fall", "no-fall" -> serverCheckFallAction;
            case "break", "fast-break" -> serverCheckBreakAction;
            case "xray" -> serverCheckXrayAction;
            case "dupe" -> serverCheckDupeAction;
            default -> serverCheckAction;
        };
        if (raw == null || raw.isBlank()) {
            raw = serverCheckAction;
        }
        return normalizeCheckAction(raw);
    }

    public static void cycleCheckAction(String check) {
        String next = switch (checkActionFor(check)) {
            case "log" -> "notify";
            case "notify" -> "kick";
            default -> "log";
        };
        switch (check == null ? "" : check) {
            case "fly" -> serverCheckFlyAction = next;
            case "speed" -> serverCheckSpeedAction = next;
            case "reach" -> serverCheckReachAction = next;
            case "fall", "no-fall" -> serverCheckFallAction = next;
            case "break", "fast-break" -> serverCheckBreakAction = next;
            case "xray" -> serverCheckXrayAction = next;
            case "dupe" -> serverCheckDupeAction = next;
            default -> {
            }
        }
    }

    public static String joinCheckActions() {
        return checkActionFor("fly") + " " + checkActionFor("speed") + " " + checkActionFor("reach") + " "
                + checkActionFor("fall") + " " + checkActionFor("break") + " " + checkActionFor("xray") + " "
                + checkActionFor("dupe");
    }

    private static void loadCheckActions(String raw) {
        String[] parts = raw == null || raw.isBlank() ? new String[0] : raw.trim().split("\\s+");
        serverCheckFlyAction = actionToken(parts, 0);
        serverCheckSpeedAction = actionToken(parts, 1);
        serverCheckReachAction = actionToken(parts, 2);
        serverCheckFallAction = actionToken(parts, 3);
        serverCheckBreakAction = actionToken(parts, 4);
        serverCheckXrayAction = actionToken(parts, 5);
        serverCheckDupeAction = actionToken(parts, 6);
        serverCheckAction = sharedCheckAction();
    }

    private static String actionToken(String[] parts, int index) {
        if (index >= parts.length || parts[index].isBlank()) {
            return normalizeCheckAction(serverCheckAction);
        }
        return normalizeCheckAction(parts[index]);
    }

    private static String sharedCheckAction() {
        String first = checkActionFor("fly");
        if (first.equals(checkActionFor("speed")) && first.equals(checkActionFor("reach"))
                && first.equals(checkActionFor("fall")) && first.equals(checkActionFor("break"))
                && first.equals(checkActionFor("xray")) && first.equals(checkActionFor("dupe"))) {
            return first;
        }
        return "log";
    }

    public static void setServerTweaksEnabled(boolean enabled) {
        applyServerTweaks(enabled, serverMotd, serverJoin, serverLeave, serverWelcome, serverWelcomeSub, serverRules,
                serverTabHeader, serverTabFooter, serverStarterKit, serverCheckAction, serverCheckFly, serverCheckSpeed,
                serverCheckReach, serverCheckFall, serverCheckBreak, serverCheckXray, serverCheckDupe, serverKits, serverRoles,
                serverMutes, serverSeen, serverLog, serverHidePing, serverSortTab, serverRolePrefix,
                joinCheckActions(), true);
    }

    private static float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        return Math.min(1f, value);
    }

    private static List<String> cleanText(List<? extends String> values) {
        List<String> out = new ArrayList<>();
        if (values == null) {
            return out;
        }
        for (String raw : values) {
            if (raw == null) {
                continue;
            }
            String s = raw.trim();
            if (s.isEmpty()) {
                continue;
            }
            if (s.length() > 256) {
                s = s.substring(0, 256);
            }
            out.add(s);
            if (out.size() >= 512) {
                break;
            }
        }
        return out;
    }

    private static String clipText(String message) {
        if (message == null) {
            return "";
        }
        String s = message;
        if (s.length() > 256) {
            s = s.substring(0, 256);
        }
        return s;
    }

    public static boolean isItemBanned(ResourceLocation id) {
        return id != null && bannedItemIds.contains(id.toString().toLowerCase());
    }

    public static ItemBan itemBan(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        return itemBans.get(id.toString().toLowerCase());
    }

    public static List<String> bannedItemIdList() {
        List<String> list = encodeBans(itemBans);
        Collections.sort(list);
        return list;
    }

    /** Keeps flags on ids that stay banned. Newly banned ids start with every flag on. */
    public static void setBannedItemIds(Set<String> ids) {
        Map<String, ItemBan> next = new HashMap<>();
        for (String entry : ids) {
            String key = normalizeItemId(entry);
            if (key == null) {
                continue;
            }
            ItemBan existing = itemBans.get(key);
            next.put(key, existing == null ? new ItemBan() : existing.copy());
        }
        applyBans(next, true);
    }

    public static void setItemBanned(ResourceLocation id, boolean banned) {
        String key = id == null ? null : normalizeItemId(id.toString());
        if (key == null) {
            return;
        }
        Map<String, ItemBan> next = copyBans();
        if (banned) {
            next.putIfAbsent(key, new ItemBan());
        } else {
            next.remove(key);
        }
        applyBans(next, true);
    }

    public static void setItemFlag(ResourceLocation id, String flag, boolean on) {
        String key = id == null ? null : normalizeItemId(id.toString());
        ItemBan ban = key == null ? null : copyBans().get(key);
        if (ban == null) {
            return;
        }
        switch (flag) {
            case "inventory" -> ban.inventory = on;
            case "recipes" -> ban.recipes = on;
            case "loot" -> ban.loot = on;
            default -> {
                return;
            }
        }
        Map<String, ItemBan> next = copyBans();
        next.put(key, ban);
        applyBans(next, true);
    }

    /** Server writes the spec. Clients only keep the synced map. */
    public static void replaceBans(Map<String, ItemBan> rules, boolean writeSpec) {
        applyBans(rules, writeSpec);
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void reloadBannedItems(List<? extends String> values) {
        Map<String, ItemBan> parsed = new HashMap<>();
        for (String entry : values) {
            ParsedBan parsedBan = parseBan(entry);
            if (parsedBan != null) {
                parsed.put(parsedBan.id, parsedBan.ban);
            }
        }
        applyBans(parsed, false);
    }

    private static Map<String, ItemBan> copyBans() {
        Map<String, ItemBan> next = new HashMap<>();
        for (Map.Entry<String, ItemBan> entry : itemBans.entrySet()) {
            next.put(entry.getKey(), entry.getValue().copy());
        }
        return next;
    }

    private static void applyBans(Map<String, ItemBan> rules, boolean writeSpec) {
        itemBans = rules;
        bannedItemIds = new HashSet<>(rules.keySet());
        if (writeSpec) {
            BANNED_ITEMS.set(encodeBans(rules));
        }
    }

    private static List<String> encodeBans(Map<String, ItemBan> rules) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, ItemBan> entry : rules.entrySet()) {
            ItemBan ban = entry.getValue();
            if (ban.inventory && ban.recipes && ban.loot) {
                list.add(entry.getKey());
                continue;
            }
            StringBuilder flags = new StringBuilder();
            if (ban.inventory) {
                flags.append('i');
            }
            if (ban.recipes) {
                flags.append('r');
            }
            if (ban.loot) {
                flags.append('l');
            }
            list.add(entry.getKey() + "#" + flags);
        }
        Collections.sort(list);
        return list;
    }

    private static String normalizeItemId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toLowerCase();
        int hash = key.indexOf('#');
        if (hash >= 0) {
            key = key.substring(0, hash);
        }
        if (!key.contains(":")) {
            key = "minecraft:" + key;
        }
        return ResourceLocation.tryParse(key) == null ? null : key;
    }

    private static ParsedBan parseBan(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim().toLowerCase();
        boolean inventory = true;
        boolean recipes = true;
        boolean loot = true;
        int hash = text.indexOf('#');
        if (hash >= 0) {
            String flags = text.substring(hash + 1);
            text = text.substring(0, hash);
            inventory = flags.indexOf('i') >= 0;
            recipes = flags.indexOf('r') >= 0;
            loot = flags.indexOf('l') >= 0;
        }
        String id = normalizeItemId(text);
        if (id == null) {
            return null;
        }
        ItemBan ban = new ItemBan();
        ban.inventory = inventory;
        ban.recipes = recipes;
        ban.loot = loot;
        return new ParsedBan(id, ban);
    }

    private record ParsedBan(String id, ItemBan ban) {
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
        reloadFromSpec();
    }

    /** Copy the live spec into the fields the rest of the mod reads. */
    public static void reloadFromSpec() {
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
        trueNightAdminLocked = TRUE_NIGHT_ADMIN_LOCK.get();
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
        commandBlockerEnabled = COMMAND_BLOCKER.get();
        commandRules = cleanText(COMMAND_RULES.get());
        commandDenyMessage = clipText(COMMAND_DENY.get());
        interactionBlockerEnabled = INTERACTION_BLOCKER.get();
        interactionRules = cleanText(INTERACTION_RULES.get());
        interactionMessage = clipText(INTERACTION_MESSAGE.get());
        interactionOpBypass = INTERACTION_OP_BYPASS.get();
        toolRestrictionsEnabled = TOOL_RESTRICTIONS.get();
        toolExceptions = cleanText(TOOL_EXCEPTIONS.get());
        toolHand = cleanText(TOOL_HAND.get());
        toolMessage = clipText(TOOL_MESSAGE.get());
        toolSound = TOOL_SOUND.get();
        portalLockEnabled = PORTAL_LOCK.get();
        portalBlockCreation = PORTAL_BLOCK_CREATION.get();
        portalOpBypass = PORTAL_OP_BYPASS.get();
        portalMessage = clipText(PORTAL_MESSAGE.get());
        portalRules = cleanText(PORTAL_RULES.get());
        elytraRulesEnabled = ELYTRA_RULES.get();
        elytraOpBypass = ELYTRA_OP_BYPASS.get();
        elytraMessage = clipText(ELYTRA_MESSAGE.get());
        elytraRules = cleanText(ELYTRA_RULE_LINES.get());
        mobDropsEnabled = MOB_DROPS.get();
        mobDropsClearVanilla = MOB_DROPS_CLEAR.get();
        mobDropRules = cleanText(MOB_DROP_RULES.get());
        villagerTradesEnabled = VILLAGER_TRADES.get();
        villagerBlockAll = VILLAGER_BLOCK_ALL.get();
        villagerBlockWandering = VILLAGER_BLOCK_WANDERING.get();
        villagerFreezePrices = VILLAGER_FREEZE_PRICES.get();
        villagerMessage = clipText(VILLAGER_MESSAGE.get());
        villagerProfessions = cleanText(VILLAGER_PROFESSIONS.get());
        villagerResults = cleanText(VILLAGER_RESULTS.get());
        villagerCustoms = cleanText(VILLAGER_CUSTOMS.get());
        itemWeightEnabled = ITEM_WEIGHT.get();
        itemWeightCapacity = ITEM_WEIGHT_CAPACITY.get().floatValue();
        itemWeightSlow = ITEM_WEIGHT_SLOW.get().floatValue();
        itemWeightSprint = ITEM_WEIGHT_SPRINT.get().floatValue();
        itemWeightHunger = ITEM_WEIGHT_HUNGER.get().floatValue();
        itemWeightWet = ITEM_WEIGHT_WET.get().floatValue();
        itemWeightSlotBonus = ITEM_WEIGHT_SLOT.get().floatValue();
        itemWeightStrength = ITEM_WEIGHT_STRENGTH.get().floatValue();
        itemWeightOverrides = cleanText(ITEM_WEIGHT_OVERRIDES.get());
        serverTweaksEnabled = SERVER_TWEAKS.get();
        serverMotd = clipText(SERVER_MOTD.get());
        serverJoin = clipText(SERVER_JOIN.get());
        serverLeave = clipText(SERVER_LEAVE.get());
        serverWelcome = clipText(SERVER_WELCOME.get());
        serverWelcomeSub = clipText(SERVER_WELCOME_SUB.get());
        serverRules = cleanText(SERVER_RULES.get());
        serverTabHeader = clipText(SERVER_TAB_HEADER.get());
        serverTabFooter = clipText(SERVER_TAB_FOOTER.get());
        serverStarterKit = clipText(SERVER_STARTER_KIT.get());
        serverCheckAction = normalizeCheckAction(SERVER_CHECK_ACTION.get());
        loadCheckActions(SERVER_CHECK_ACTIONS.get());
        serverCheckFly = SERVER_CHECK_FLY.get();
        serverCheckSpeed = SERVER_CHECK_SPEED.get();
        serverCheckReach = SERVER_CHECK_REACH.get();
        serverCheckFall = SERVER_CHECK_FALL.get();
        serverCheckBreak = SERVER_CHECK_BREAK.get();
        serverCheckXray = SERVER_CHECK_XRAY.get();
        serverCheckDupe = SERVER_CHECK_DUPE.get();
        serverHidePing = SERVER_HIDE_PING.get();
        serverSortTab = SERVER_SORT_TAB.get();
        serverRolePrefix = SERVER_ROLE_PREFIX.get();
        serverKits = cleanText(SERVER_KITS.get());
        serverRoles = cleanText(SERVER_ROLES.get());
        serverMutes = cleanText(SERVER_MUTES.get());
        serverSeen = cleanText(SERVER_SEEN.get());
        serverLog = cleanText(SERVER_LOG.get());
        yblk.features.restrict.RestrictionsSync.afterConfigReload();
        yblk.features.world.WorldSync.afterConfigReload();
        yblk.features.mobs.MobsSync.afterConfigReload();
        net.minecraft.server.MinecraftServer weightServer = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (weightServer != null) {
            for (net.minecraft.server.level.ServerPlayer weightPlayer : weightServer.getPlayerList().getPlayers()) {
                yblk.features.network.ModNetwork.sendToPlayer(weightPlayer, yblk.features.network.ItemWeightPacket.current());
                yblk.features.network.ModNetwork.sendToPlayer(weightPlayer, yblk.features.network.ServerTweaksPacket.current(false));
            }
        }
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
