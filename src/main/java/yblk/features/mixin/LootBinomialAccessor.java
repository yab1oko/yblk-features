package yblk.features.mixin;

import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BinomialDistributionGenerator.class)
public interface LootBinomialAccessor {
    @Accessor("n")
    NumberProvider features$n();

    @Accessor("p")
    NumberProvider features$p();
}
