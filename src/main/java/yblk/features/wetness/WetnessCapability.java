package yblk.features.wetness;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;

@Mod.EventBusSubscriber(modid = Features.MODID)
public final class WetnessCapability {
    public static final Capability<Wetness> WETNESS =
            CapabilityManager.get(new CapabilityToken<>() {
            });
    public static final ResourceLocation KEY = new ResourceLocation(Features.MODID, "wetness");

    private WetnessCapability() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(Wetness.class);
    }

    public static Wetness get(Player player) {
        return player.getCapability(WETNESS).orElseGet(Wetness::new);
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) {
            return;
        }
        WetnessProvider provider = new WetnessProvider();
        event.addCapability(KEY, provider);
        event.addListener(provider::invalidate);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            event.getOriginal().reviveCaps();
            Wetness old = get(event.getOriginal());
            Wetness next = get(event.getEntity());
            next.load(old.save());
            event.getOriginal().invalidateCaps();
        }
    }
}
