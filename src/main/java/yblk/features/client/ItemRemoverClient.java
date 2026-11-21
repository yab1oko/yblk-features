package yblk.features.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client follow-up after an item-remover sync: creative tabs. */
public final class ItemRemoverClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("Features");

    private ItemRemoverClient() {
    }

    public static void onSynced() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }
        CreativeModeTabs.tryRebuildTabContents(
                minecraft.getConnection().enabledFeatures(),
                minecraft.player.canUseGameMasterBlocks(),
                minecraft.player.level().registryAccess());
        refreshViewer("jei", "yblk.features.compat.jei.FeaturesJeiPlugin");
        refreshViewer("roughlyenoughitems", "yblk.features.compat.rei.FeaturesReiPlugin");
        refreshViewer("emi", "yblk.features.compat.emi.FeaturesEmiPlugin");
    }

    private static void refreshViewer(String modId, String className) {
        if (!ModList.get().isLoaded(modId)) {
            return;
        }
        try {
            Class.forName(className).getMethod("refresh").invoke(null);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Could not refresh {} item list: {}", modId, e.toString());
        }
    }
}
