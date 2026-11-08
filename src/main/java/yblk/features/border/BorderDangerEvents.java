package yblk.features.border;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import yblk.features.config.FeaturesConfig;

public class BorderDangerEvents {
    private static final float HEARTS_PER_SECOND = 1.0f;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide || player.isCreative() || player.isSpectator()) {
            return;
        }

        float harm = BorderDanger.harmIntensity(player);
        if (harm > 0.001f) {
            if (FeaturesConfig.borderHarmMode == BorderHarmMode.DAMAGE) {
                if (player.tickCount % 20 == 0) {
                    float damage = HEARTS_PER_SECOND * 2.0f * Math.max(0.25f, harm);
                    player.hurt(player.damageSources().magic(), damage);
                }
            } else {
                applyHiddenEffects(player);
            }
        }

        // Chat / hotbar warnings are sent from the server; CENTER is client HUD only.
        if (!FeaturesConfig.borderShowWarning || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        BorderDanger.WarningStage stage = BorderDanger.warningStage(player);
        if (stage == BorderDanger.WarningStage.NONE) {
            return;
        }
        if (FeaturesConfig.borderWarningStyle == BorderWarningStyle.CENTER) {
            return;
        }
        if (player.tickCount % 40 != 0) {
            return;
        }
        Component message = BorderWarningTexts.forStage(stage);
        if (FeaturesConfig.borderWarningStyle == BorderWarningStyle.CHAT) {
            serverPlayer.sendSystemMessage(message);
        } else {
            serverPlayer.displayClientMessage(message, true);
        }
    }

    private static void applyHiddenEffects(Player player) {
        for (String key : FeaturesConfig.borderEffects) {
            MobEffect effect = BorderEffectOptions.ALL.get(key);
            if (effect == null) {
                continue;
            }
            player.addEffect(new MobEffectInstance(effect, 45, 0, false, false, false));
        }
    }
}
