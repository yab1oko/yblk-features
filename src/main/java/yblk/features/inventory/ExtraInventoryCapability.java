package yblk.features.inventory;

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
public final class ExtraInventoryCapability {
    public static final Capability<ExtraInventory> EXTRA_INVENTORY =
            CapabilityManager.get(new CapabilityToken<>() {
            });
    public static final ResourceLocation KEY = new ResourceLocation(Features.MODID, "extra_inventory");

    private ExtraInventoryCapability() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(ExtraInventory.class);
    }

    public static ExtraInventory get(Player player) {
        return player.getCapability(EXTRA_INVENTORY).orElseGet(ExtraInventory::new);
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) {
            return;
        }
        ExtraInventoryProvider provider = new ExtraInventoryProvider();
        event.addCapability(KEY, provider);
        event.addListener(provider::invalidate);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        ExtraInventory oldInv = get(event.getOriginal());
        ExtraInventory newInv = get(event.getEntity());
        newInv.deserializeNBT(oldInv.serializeNBT());
        event.getOriginal().invalidateCaps();
    }
}
