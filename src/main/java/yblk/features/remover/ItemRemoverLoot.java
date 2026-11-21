package yblk.features.remover;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import yblk.features.Features;

public final class ItemRemoverLoot {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Features.MODID);

    public static final RegistryObject<Codec<RemoveBannedItemsModifier>> REMOVE_BANNED =
            MODIFIERS.register("remove_banned", () -> RemoveBannedItemsModifier.CODEC);

    private ItemRemoverLoot() {
    }
}
