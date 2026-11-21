package yblk.features;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import yblk.features.border.BorderDangerEvents;
import yblk.features.chat.LocalChatEvents;
import yblk.features.config.FeaturesConfig;
import yblk.features.feature.FeatureRegistry;
import yblk.features.inventory.ExtraInventoryCapability;
import yblk.features.inventory.InventoryRevampEvents;
import yblk.features.network.ModNetwork;
import yblk.features.remover.ItemRemoverEvents;
import yblk.features.restrict.RestrictionsEvents;
import yblk.features.remover.ItemRemoverLoot;
import yblk.features.spawn.MobSpawnEvents;
import yblk.features.wetness.WetnessCapability;
import yblk.features.wetness.WetnessEvents;
import yblk.features.world.FeaturesCommands;
import yblk.features.world.WorldEvents;

@Mod(Features.MODID)
public class Features {
    public static final String MODID = "features";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Features() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        FeatureRegistry.bootstrap();
        ItemRemoverLoot.MODIFIERS.register(modBus);
        modBus.addListener(ItemRemoverEvents::onBuildCreativeTab);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FeaturesConfig.SPEC);
        modBus.addListener(ExtraInventoryCapability::register);
        modBus.addListener(WetnessCapability::register);
        ModNetwork.register();
        MinecraftForge.EVENT_BUS.register(new InventoryRevampEvents());
        registerCuriosInventoryRevamp();
        MinecraftForge.EVENT_BUS.register(new MobSpawnEvents());
        MinecraftForge.EVENT_BUS.register(new BorderDangerEvents());
        MinecraftForge.EVENT_BUS.register(new LocalChatEvents());
        MinecraftForge.EVENT_BUS.register(new WetnessEvents());
        MinecraftForge.EVENT_BUS.register(new ItemRemoverEvents());
        MinecraftForge.EVENT_BUS.register(new RestrictionsEvents());
        MinecraftForge.EVENT_BUS.register(new WorldEvents());
        MinecraftForge.EVENT_BUS.register(new FeaturesCommands());
        LOGGER.info("Features loaded");
    }

    /** Soft: class references Curios API — only load when Curios is present. */
    private static void registerCuriosInventoryRevamp() {
        if (!ModList.get().isLoaded("curios")) {
            return;
        }
        try {
            Class<?> handler = Class.forName("yblk.features.compat.CuriosInventoryRevampEvents");
            MinecraftForge.EVENT_BUS.register(handler.getDeclaredConstructor().newInstance());
            LOGGER.info("Inventory Revamp ↔ Curios equip sync enabled");
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Curios present but Inventory Revamp sync unavailable: {}", e.toString());
        }
    }
}
