package yblk.features.weight;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Features.MODID)
public final class ItemWeightEvents {
    public static final UUID MODIFIER = UUID.fromString("6f4c1a90-7b2e-4d1a-9c55-0a1e8b7c4d21");

    private ItemWeightEvents() {
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player == null) {
            return;
        }
        var player = event.player;
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (player.level().isClientSide() && !player.isLocalPlayer()) {
            return;
        }
        if (!FeaturesConfig.itemWeightEnabled) {
            if (speed.getModifier(MODIFIER) != null) {
                speed.removeModifier(MODIFIER);
            }
            return;
        }
        float ratio = ItemWeights.ratio(player);
        if (ratio >= FeaturesConfig.itemWeightSprint) {
            player.setSprinting(false);
        }
        if (!player.level().isClientSide() && ratio >= FeaturesConfig.itemWeightHunger && player.tickCount % 20 == 0) {
            player.causeFoodExhaustion(0.12f);
        }
        double slow = ItemWeights.slowAmount(ratio);
        AttributeModifier current = speed.getModifier(MODIFIER);
        if (slow <= 0.001d) {
            if (current != null) {
                speed.removeModifier(MODIFIER);
            }
            return;
        }
        if (current != null && Math.abs(current.getAmount() + slow) < 0.02d) {
            return;
        }
        if (current != null) {
            speed.removeModifier(MODIFIER);
        }
        speed.addTransientModifier(new AttributeModifier(MODIFIER, "features_item_weight", -slow, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
