package yblk.features.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import yblk.features.Features;

public final class ClothConfigScreens {
    private ClothConfigScreens() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("screen." + Features.MODID + ".config"));

        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory features = builder.getOrCreateCategory(Component.translatable("config." + Features.MODID + ".features"));
        // Feature toggles / detailed options live in the Features menu cards.
        // Keep a short pointer so Mod Menu / Config button is not empty.
        features.addEntry(entries.startTextDescription(
                Component.translatable("config." + Features.MODID + ".use_features_menu")
        ).build());

        return builder.build();
    }
}
