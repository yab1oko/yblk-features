package yblk.features.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.inventory.SlotCalculator;

@Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
public final class ArmorSlotTooltip {
    private ArmorSlotTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!SlotCalculator.isActive()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        int bonus = FeaturesConfig.bonusFor(stack);
        if (bonus <= 0) {
            return;
        }
        event.getToolTip().add(Component.translatable("tooltip." + Features.MODID + ".armor_slots", bonus)
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
