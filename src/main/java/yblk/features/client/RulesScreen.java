package yblk.features.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.client.ui.DarkButton;
import yblk.features.config.FeaturesConfig;
import yblk.features.network.ModNetwork;
import yblk.features.network.ServerActionPacket;

/** First-join rules. Accept is the only way out. */
@Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
public class RulesScreen extends Screen {
    public RulesScreen() {
        super(Component.translatable("screen." + Features.MODID + ".server_tweaks.rules_title"));
    }

    public static void openIfNeeded() {
        Minecraft minecraft = Minecraft.getInstance();
        if (FeaturesConfig.serverTweaksPromptRules && minecraft.screen == null && minecraft.player != null) {
            FeaturesConfig.serverTweaksPromptRules = false;
            minecraft.setScreen(new RulesScreen());
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            openIfNeeded();
        }
    }

    @Override
    protected void init() {
        addRenderableWidget(new DarkButton(font, width / 2 - 50, height - 32, 100, 18,
                Component.translatable("screen." + Features.MODID + ".server_tweaks.accept"), this::accept,
                0xFF8E6BBF, 0xFF2A2438));
    }

    private void accept() {
        if (minecraft != null && minecraft.getConnection() != null) {
            ModNetwork.sendToServer(new ServerActionPacket("accept"));
        }
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 24, 0xF0F0F0);
        int y = 48;
        for (String line : FeaturesConfig.serverRules) {
            graphics.drawCenteredString(font, font.plainSubstrByWidth(line, width - 40), width / 2, y, 0xD0D0D8);
            y += 12;
            if (y > height - 48) {
                break;
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
