package yblk.features.mobs;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TradeLock {
    private static final Map<UUID, Integer> SPOKEN = new HashMap<>();

    private TradeLock() {
    }

    public static boolean blocks(Villager villager) {
        if (!FeaturesConfig.villagerTradesEnabled || villager == null) {
            return false;
        }
        if (FeaturesConfig.villagerBlockAll) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.VILLAGER_PROFESSIONS.getKey(villager.getVillagerData().getProfession());
        if (id == null) {
            return false;
        }
        for (String raw : FeaturesConfig.villagerProfessions) {
            String target = TargetMatch.entity(raw);
            if (TargetMatch.id(id, target)) {
                return true;
            }
        }
        return false;
    }

    public static boolean blocksTrader(WanderingTrader trader) {
        return FeaturesConfig.villagerTradesEnabled && trader != null
                && (FeaturesConfig.villagerBlockAll || FeaturesConfig.villagerBlockWandering);
    }

    public static void strip(AbstractVillager villager) {
        if (!FeaturesConfig.villagerTradesEnabled || villager == null || FeaturesConfig.villagerResults.isEmpty()) {
            return;
        }
        villager.getOffers().removeIf(offer -> removes(offer));
    }

    public static void decorate(AbstractVillager villager) {
        strip(villager);
        if (!FeaturesConfig.villagerTradesEnabled || villager == null) {
            return;
        }
        String who = who(villager);
        if (who == null) {
            return;
        }
        for (TradeCustoms.Custom custom : TradeCustoms.forWho(FeaturesConfig.villagerCustoms, who)) {
            villager.getOffers().add(custom.offer());
        }
    }

    public static String who(AbstractVillager villager) {
        if (villager instanceof WanderingTrader) {
            return TradeCustoms.WANDERING;
        }
        if (villager instanceof Villager trader) {
            ResourceLocation id = ForgeRegistries.VILLAGER_PROFESSIONS.getKey(trader.getVillagerData().getProfession());
            return id == null ? null : id.toString();
        }
        return null;
    }

    public static boolean emptied(AbstractVillager villager) {
        return FeaturesConfig.villagerTradesEnabled
                && !FeaturesConfig.villagerResults.isEmpty()
                && villager.getOffers().isEmpty();
    }

    public static boolean freeze(Villager villager) {
        if (!FeaturesConfig.villagerTradesEnabled || !FeaturesConfig.villagerFreezePrices || villager == null) {
            return false;
        }
        for (MerchantOffer offer : villager.getOffers()) {
            offer.setSpecialPriceDiff(0);
        }
        return true;
    }

    public static void tell(Player player) {
        if (!(player instanceof ServerPlayer server)) {
            return;
        }
        int now = server.tickCount;
        Integer last = SPOKEN.get(server.getUUID());
        if (last != null && now - last < 15) {
            return;
        }
        SPOKEN.put(server.getUUID(), now);
        String custom = FeaturesConfig.villagerMessage;
        Component message = custom != null && !custom.isBlank()
                ? Component.literal(custom)
                : Component.translatable("screen." + Features.MODID + ".villager_trades.denied");
        server.displayClientMessage(message, true);
    }

    public static void forget(UUID id) {
        if (id != null) {
            SPOKEN.remove(id);
        }
    }

    private static boolean removes(MerchantOffer offer) {
        ItemStack result = offer.getResult();
        for (String raw : FeaturesConfig.villagerResults) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String target = raw.trim().toLowerCase(Locale.ROOT);
            int cut = target.indexOf('~');
            if (cut > 0) {
                String item = TargetMatch.item(target.substring(0, cut));
                if (TargetMatch.item(result, item) && hasEnchant(result, target.substring(cut + 1))) {
                    return true;
                }
                continue;
            }
            String plain = TargetMatch.item(raw);
            if (!plain.isEmpty() && TargetMatch.item(result, plain)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEnchant(ItemStack stack, String id) {
        for (Enchantment enchantment : EnchantmentHelper.getEnchantments(stack).keySet()) {
            ResourceLocation key = EnchantmentHelper.getEnchantmentId(enchantment);
            if (key != null && key.toString().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
