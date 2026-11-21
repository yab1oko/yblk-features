package yblk.features.mobs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** One extra offer: who, two prices, and a result. */
public final class TradeCustoms {
    public static final String WANDERING = "wandering";

    public record Custom(String who, ItemStack costA, ItemStack costB, ItemStack result, String stored) {
        public MerchantOffer offer() {
            ItemStack second = costB == null || costB.isEmpty() ? ItemStack.EMPTY : costB.copy();
            return new MerchantOffer(costA.copy(), second, result.copy(), 0, 12, 1, 0.05f);
        }
    }

    private TradeCustoms() {
    }

    public static Custom build(String who, ItemStack costA, ItemStack costB, ItemStack result) {
        if (who == null || who.isBlank() || costA == null || costA.isEmpty() || result == null || result.isEmpty()) {
            return null;
        }
        ItemStack second = costB == null ? ItemStack.EMPTY : costB;
        String stored = who.trim()
                + " " + stack(costA)
                + " " + (second.isEmpty() ? "- 0" : stack(second))
                + " " + stack(result);
        return new Custom(who.trim(), costA.copy(), second.copy(), result.copy(), stored);
    }

    public static Custom parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.trim().split("\\s+");
        if (parts.length < 7) {
            return null;
        }
        ItemStack costA = stack(parts[1], parts[2]);
        ItemStack costB = "-".equals(parts[3]) ? ItemStack.EMPTY : stack(parts[3], parts[4]);
        ItemStack result = stack(parts[5], parts[6]);
        if (costA.isEmpty() || result.isEmpty()) {
            return null;
        }
        return new Custom(parts[0], costA, costB, result, raw.trim());
    }

    public static List<Custom> forWho(List<String> lines, String who) {
        List<Custom> out = new ArrayList<>();
        if (lines == null || who == null) {
            return out;
        }
        for (String line : lines) {
            Custom custom = parse(line);
            if (custom != null && who.equals(custom.who)) {
                out.add(custom);
            }
        }
        return out;
    }

    public static List<String> add(List<String> lines, Custom custom) {
        List<String> next = new ArrayList<>();
        if (lines != null) {
            next.addAll(lines);
        }
        next.add(custom.stored);
        return next;
    }

    public static List<String> remove(List<String> lines, String stored) {
        List<String> next = new ArrayList<>();
        if (lines == null) {
            return next;
        }
        for (String line : lines) {
            if (!line.equals(stored)) {
                next.add(line);
            }
        }
        return next;
    }

    private static String stack(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return (id == null ? "minecraft:air" : id) + " " + stack.getCount();
    }

    private static ItemStack stack(String id, String countRaw) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            return ItemStack.EMPTY;
        }
        var item = ForgeRegistries.ITEMS.getValue(parsed);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        int count = 1;
        try {
            count = Integer.parseInt(countRaw);
        } catch (NumberFormatException ignored) {
            count = 1;
        }
        return new ItemStack(item, MthCount(count, item.getMaxStackSize()));
    }

    private static int MthCount(int count, int max) {
        return Math.max(1, Math.min(max, count));
    }
}
