package yblk.features.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import yblk.features.remover.ItemRemover;

import java.util.HashSet;
import java.util.Set;

/** Hides recipe-banned items from EMI. Loaded only when EMI is installed. */
@EmiEntrypoint
public class FeaturesEmiPlugin implements EmiPlugin {
    private static EmiRegistry registry;
    private static final Set<Item> hidden = new HashSet<>();

    @Override
    public void register(EmiRegistry emiRegistry) {
        registry = emiRegistry;
        refresh();
    }

    public static void refresh() {
        if (registry == null) {
            return;
        }
        for (Item item : Set.copyOf(hidden)) {
            registry.addEmiStack(EmiStack.of(item));
        }
        hidden.clear();
        registry.removeEmiStacks(stack -> {
            ItemStack itemStack = stack.getItemStack();
            if (itemStack.isEmpty() || !ItemRemover.hidesFromRecipes(itemStack)) {
                return false;
            }
            hidden.add(itemStack.getItem());
            return true;
        });
    }
}
