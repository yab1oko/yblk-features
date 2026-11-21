package yblk.features.remover;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** Drops banned items out of every loot roll: mobs, chests, fishing, bartering. */
public class RemoveBannedItemsModifier extends LootModifier {
    public static final Codec<RemoveBannedItemsModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance).apply(instance, RemoveBannedItemsModifier::new));

    public RemoveBannedItemsModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generated, LootContext context) {
        if (!ItemRemover.isActive()) {
            return generated;
        }
        generated.removeIf(ItemRemover::removesFromLoot);
        return generated;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
