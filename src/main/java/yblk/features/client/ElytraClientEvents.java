package yblk.features.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.world.FlightRules;

@Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
public final class ElytraClientEvents {
    private ElytraClientEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof LocalPlayer player)) {
            return;
        }
        FlightRules.clientTick(player);
    }
}
