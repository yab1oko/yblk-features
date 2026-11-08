package yblk.features.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;

@Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen pause)) {
            return;
        }
        // Screen.init already cleared widgets — no need to remove old buttons.
        FeaturesPauseButton button = new FeaturesPauseButton(0, 0, pause);
        snapToOptions(pause, button);
        event.addListener(button);
    }

    /**
     * Re-anchor every frame so a mid-session GUI-scale change can't leave the icon off-screen
     * until the pause menu is reopened.
     */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof PauseScreen pause)) {
            return;
        }
        for (GuiEventListener listener : pause.children()) {
            if (listener instanceof FeaturesPauseButton button) {
                snapToOptions(pause, button);
                return;
            }
        }
    }

    private static void snapToOptions(PauseScreen pause, FeaturesPauseButton button) {
        AbstractWidget options = findOptionsButton(pause);
        int x;
        int y;
        if (options != null) {
            x = options.getX() - button.getWidth() - 4;
            y = options.getY() + (options.getHeight() - button.getHeight()) / 2;
        } else {
            x = pause.width / 2 - 102 - button.getWidth() - 4;
            y = pause.height / 4 + 48 + 24;
        }
        x = Mth.clamp(x, 2, Math.max(2, pause.width - button.getWidth() - 2));
        y = Mth.clamp(y, 2, Math.max(2, pause.height - button.getHeight() - 2));
        button.setX(x);
        button.setY(y);
    }

    private static AbstractWidget findOptionsButton(PauseScreen pause) {
        for (GuiEventListener listener : pause.children()) {
            if (!(listener instanceof AbstractWidget widget) || widget instanceof FeaturesPauseButton) {
                continue;
            }
            Component message = widget.getMessage();
            if (message.getContents() instanceof TranslatableContents contents
                    && "menu.options".equals(contents.getKey())) {
                return widget;
            }
        }
        return null;
    }
}
