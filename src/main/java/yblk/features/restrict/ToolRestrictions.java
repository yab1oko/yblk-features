package yblk.features.restrict;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;

import java.util.List;
import java.util.Locale;

public final class ToolRestrictions {
    private ToolRestrictions() {
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || event.getLevel().isClientSide() || !FeaturesConfig.toolRestrictionsEnabled) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!denies(player, event.getLevel(), event.getPos(), event.getState())) {
            return;
        }
        event.setCanceled(true);
        Component message = denyText(event.getState());
        if (RestrictionMessages.actionBar(player, message) && FeaturesConfig.toolSound) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE,
                    SoundSource.PLAYERS, 0.55f, 1.0f);
        }
    }

    public static boolean denies(Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        if (!FeaturesConfig.toolRestrictionsEnabled || player.getAbilities().instabuild) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0f || listed(state, FeaturesConfig.toolExceptions)) {
            return false;
        }
        boolean axe = state.is(BlockTags.MINEABLE_WITH_AXE);
        boolean pick = state.is(BlockTags.MINEABLE_WITH_PICKAXE);
        boolean shovel = state.is(BlockTags.MINEABLE_WITH_SHOVEL);
        boolean hoe = state.is(BlockTags.MINEABLE_WITH_HOE);
        if (!axe && !pick && !shovel && !hoe) {
            return false;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return !listed(state, FeaturesConfig.toolHand);
        }
        boolean ok = (axe && isTool(stack, AxeItem.class, ItemTags.AXES))
                || (pick && isTool(stack, PickaxeItem.class, ItemTags.PICKAXES))
                || (shovel && isTool(stack, ShovelItem.class, ItemTags.SHOVELS))
                || (hoe && isTool(stack, HoeItem.class, ItemTags.HOES))
                || stack.getItem().isCorrectToolForDrops(state);
        return !ok;
    }

    private static boolean isTool(ItemStack stack, Class<?> type, TagKey<net.minecraft.world.item.Item> tag) {
        return type.isInstance(stack.getItem()) || stack.is(tag);
    }

    public static boolean listed(BlockState state, List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        for (String raw : entries) {
            if (raw == null) {
                continue;
            }
            String s = raw.trim().toLowerCase(Locale.ROOT);
            if (s.startsWith("#")) {
                ResourceLocation tagId = parseId(s.substring(1));
                if (tagId != null && state.is(TagKey.create(Registries.BLOCK, tagId))) {
                    return true;
                }
            } else if (id != null && id.equals(parseId(s))) {
                return true;
            }
        }
        return false;
    }

    /** Block id or {@code #tag}. Returns null when the text is not an id. */
    public static String normalizeRef(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return null;
        }
        if (s.startsWith("#")) {
            ResourceLocation tag = parseId(s.substring(1));
            return tag == null ? null : "#" + tag;
        }
        ResourceLocation id = parseId(s);
        return id == null ? null : id.toString();
    }

    private static ResourceLocation parseId(String raw) {
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (!s.contains(":")) {
            s = "minecraft:" + s;
        }
        return ResourceLocation.tryParse(s);
    }

    private static Component denyText(BlockState state) {
        String name = state.getBlock().getName().getString();
        String custom = FeaturesConfig.toolMessage;
        if (custom == null || custom.isBlank()) {
            return Component.translatable("screen." + Features.MODID + ".tool_restrictions.denied", name);
        }
        return Component.literal(custom.replace("{block}", name));
    }

    public static Block blockOf(String ref) {
        if (ref == null || ref.startsWith("#")) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(ref);
        return id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
    }
}
