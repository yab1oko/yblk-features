package yblk.features.mixin;

import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SetItemCountFunction.class)
public interface LootCountAccessor {
    @Accessor("value")
    NumberProvider features$value();

    @Accessor("add")
    boolean features$add();
}
