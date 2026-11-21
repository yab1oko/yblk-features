package yblk.features.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.weight.ItemWeights;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
public final class ItemWeightHud {
    private ItemWeightHud() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!FeaturesConfig.itemWeightEnabled) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        float one = ItemWeights.per(stack);
        event.getToolTip().add(Component.translatable("tooltip." + Features.MODID + ".item_weight", trim(one))
                .withStyle(ChatFormatting.GRAY));
        if (stack.getCount() > 1) {
            event.getToolTip().add(Component.translatable("tooltip." + Features.MODID + ".item_weight_stack", trim(ItemWeights.of(stack)))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Render.Post event) {
        if (!FeaturesConfig.itemWeightEnabled || !(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }
        if (screen.getMinecraft().player == null) {
            return;
        }
        float total = ItemWeights.total(screen.getMinecraft().player);
        float capacity = ItemWeights.capacity(screen.getMinecraft().player);
        String text = Component.translatable("screen." + Features.MODID + ".item_weight.hud",
                trim(total), trim(capacity)).getString();
        int color = total / capacity >= FeaturesConfig.itemWeightSprint ? 0xFFC96060 : 0xFFE6E6EA;
        event.getGuiGraphics().drawString(screen.getMinecraft().font, text, 8, 6, color, false);
    }

    private static String trim(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
