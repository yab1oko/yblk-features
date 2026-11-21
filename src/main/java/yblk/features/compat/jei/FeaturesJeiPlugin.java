package yblk.features.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.Features;
import yblk.features.remover.ItemRemover;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Hides recipe-banned items from JEI. Loaded only when JEI is installed. */
@JeiPlugin
public class FeaturesJeiPlugin implements IModPlugin {
    private static IJeiRuntime runtime;
    private static final Set<Item> hidden = new HashSet<>();

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Features.MODID, "item_remover");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        refresh();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        hidden.clear();
    }

    public static void refresh() {
        if (runtime == null) {
            return;
        }
        IIngredientManager manager = runtime.getIngredientManager();
        List<ItemStack> restore = new ArrayList<>();
        for (Item item : hidden) {
            ItemStack stack = new ItemStack(item);
            if (!ItemRemover.hidesFromRecipes(stack)) {
                restore.add(stack);
            }
        }
        if (!restore.isEmpty()) {
            manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, restore);
        }
        hidden.clear();
        List<ItemStack> hide = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            ItemStack stack = new ItemStack(item);
            if (ItemRemover.hidesFromRecipes(stack)) {
                hide.add(stack);
                hidden.add(item);
            }
        }
        if (!hide.isEmpty()) {
            manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hide);
        }
    }
}
