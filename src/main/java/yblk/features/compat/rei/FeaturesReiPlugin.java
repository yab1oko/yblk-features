package yblk.features.compat.rei;

import me.shedaniel.rei.api.client.entry.filtering.base.BasicFilteringRule;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.remover.ItemRemover;

import java.util.ArrayList;
import java.util.Collection;

/** Hides recipe-banned items from REI. Loaded only when REI is installed. */
@REIPluginClient
public class FeaturesReiPlugin implements REIClientPlugin {
    private static BasicFilteringRule.MarkDirty dirty;

    @Override
    public void registerBasicEntryFiltering(BasicFilteringRule<?> rule) {
        dirty = rule.hide(FeaturesReiPlugin::hidden);
    }

    public static void refresh() {
        if (dirty != null) {
            dirty.markDirty();
        }
    }

    private static Collection<EntryStack<?>> hidden() {
        Collection<EntryStack<?>> stacks = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            ItemStack stack = new ItemStack(item);
            if (ItemRemover.hidesFromRecipes(stack)) {
                stacks.add(EntryStacks.of(stack));
            }
        }
        return stacks;
    }
}
