package yblk.features.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import yblk.features.Features;

public class FeaturesPauseButton extends AbstractWidget {
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    private static final ResourceLocation ICON =
            new ResourceLocation(Features.MODID, "textures/gui/features_button.png");
    private static final int ICON_TEX = 220;
    private static final int SIZE = 20;

    private final Screen pauseScreen;

    public FeaturesPauseButton(int x, int y, Screen pauseScreen) {
        super(x, y, SIZE, SIZE, Component.translatable("menu." + Features.MODID + ".features"));
        this.pauseScreen = pauseScreen;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        graphics.setColor(1f, 1f, 1f, 1f);
        // Same nine-slice as vanilla Button (widgets.png rows 46 / 66 / 86).
        graphics.blitNineSliced(WIDGETS, getX(), getY(), width, height, 20, 4, 200, 20, 0, textureY());

        int pad = 2;
        graphics.blit(ICON, getX() + pad, getY() + pad, width - pad * 2, height - pad * 2,
                0f, 0f, ICON_TEX, ICON_TEX, ICON_TEX, ICON_TEX);

        if (isHoveredOrFocused()) {
            graphics.renderTooltip(Minecraft.getInstance().font, getMessage(), mouseX, mouseY);
        }
    }

    private int textureY() {
        if (!active) {
            return 46;
        }
        if (isHoveredOrFocused()) {
            return 86;
        }
        return 66;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        Minecraft.getInstance().setScreen(new FeaturesScreen(pauseScreen));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
