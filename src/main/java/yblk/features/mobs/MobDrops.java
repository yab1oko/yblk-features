package yblk.features.mobs;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;

import java.util.List;

@Mod.EventBusSubscriber(modid = Features.MODID)
public final class MobDrops {
    private MobDrops() {
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!FeaturesConfig.mobDropsEnabled || event.getEntity().level().isClientSide()) {
            return;
        }
        List<MobDropRules.Rule> rules = MobDropRules.compile(FeaturesConfig.mobDropRules);
        boolean playerKill = event.getSource().getEntity() instanceof Player;
        if (FeaturesConfig.mobDropsClearVanilla) {
            event.getDrops().clear();
        }
        for (MobDropRules.Rule rule : rules) {
            if (rule.kind() == MobDropRules.Kind.XP || !TargetMatch.entity(event.getEntity().getType(), rule.target())) {
                continue;
            }
            if (rule.playerOnly() && !playerKill) {
                continue;
            }
            switch (rule.kind()) {
                case REMOVE -> event.getDrops().removeIf(drop -> TargetMatch.item(drop.getItem(), rule.item()));
                case REPLACE -> replace(event, rule);
                case ADD -> add(event, rule);
                case XP -> {
                }
            }
        }
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        if (!FeaturesConfig.mobDropsEnabled || event.getEntity().level().isClientSide()) {
            return;
        }
        for (MobDropRules.Rule rule : MobDropRules.compile(FeaturesConfig.mobDropRules)) {
            if (rule.kind() != MobDropRules.Kind.XP || !TargetMatch.entity(event.getEntity().getType(), rule.target())) {
                continue;
            }
            if (rule.playerOnly() && event.getAttackingPlayer() == null) {
                continue;
            }
            int next = Math.round(event.getDroppedExperience() * rule.xp());
            event.setDroppedExperience(Math.max(0, next));
            return;
        }
    }

    private static void replace(LivingDropsEvent event, MobDropRules.Rule rule) {
        boolean removed = "*".equals(rule.item());
        var it = event.getDrops().iterator();
        while (it.hasNext()) {
            if (TargetMatch.item(it.next().getItem(), rule.item())) {
                it.remove();
                removed = true;
            }
        }
        if (removed) {
            add(event, rule.chance(), rule.item2(), rule.min(), rule.max(), rule.looting(), event.getLootingLevel());
        }
    }

    private static void add(LivingDropsEvent event, MobDropRules.Rule rule) {
        if (!rule.enabled() || event.getEntity().getRandom().nextFloat() > rule.chance()) {
            return;
        }
        add(event, 1f, rule.item(), rule.min(), rule.max(), rule.looting(), event.getLootingLevel());
    }

    private static void add(LivingDropsEvent event, float chance, String itemId, int min, int max, int looting, int lootingLevel) {
        if (chance < 1f && event.getEntity().getRandom().nextFloat() > chance) {
            return;
        }
        ItemStack stack = stack(itemId, count(event, min, max, looting, lootingLevel));
        if (stack.isEmpty()) {
            return;
        }
        Level level = event.getEntity().level();
        ItemEntity drop = new ItemEntity(level, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), stack);
        drop.setPickUpDelay(10);
        event.getDrops().add(drop);
    }

    private static int count(LivingDropsEvent event, int min, int max, int looting, int lootingLevel) {
        int count = min;
        if (max > min) {
            count += event.getEntity().getRandom().nextInt(max - min + 1);
        }
        if (looting > 0 && lootingLevel > 0) {
            count += looting * lootingLevel;
        }
        return Math.max(0, Math.min(count, 64));
    }

    private static ItemStack stack(String itemId, int count) {
        if (count <= 0 || itemId.startsWith("#") || itemId.startsWith("@") || "*".equals(itemId)) {
            return ItemStack.EMPTY;
        }
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(itemId));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, Math.min(count, item.getMaxStackSize()));
    }
}
