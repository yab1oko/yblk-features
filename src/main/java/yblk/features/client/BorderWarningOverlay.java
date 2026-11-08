package yblk.features.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.border.BorderDanger;
import yblk.features.border.BorderWarningStyle;
import yblk.features.border.BorderWarningTexts;
import yblk.features.config.FeaturesConfig;

@Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
public final class BorderWarningOverlay {
    private BorderWarningOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (FeaturesConfig.borderWarningStyle != BorderWarningStyle.CENTER || !FeaturesConfig.borderShowWarning) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }
        BorderDanger.WarningStage stage = BorderDanger.warningStage(player);
        if (stage == BorderDanger.WarningStage.NONE) {
            return;
        }

        int colorA;
        int colorB;
        if (stage == BorderDanger.WarningStage.DANGER) {
            colorA = FeaturesConfig.borderWarnColorDangerA;
            colorB = FeaturesConfig.borderWarnColorDangerB;
        } else {
            colorA = FeaturesConfig.borderWarnColorApproachA;
            colorB = FeaturesConfig.borderWarnColorApproachB;
        }

        float pulse = (Mth.sin(player.tickCount * 0.25f) + 1f) * 0.5f;
        int color = lerpRgb(colorA, colorB, pulse) | 0xFF000000;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        Component text = BorderWarningTexts.forStage(stage);
        float scale = 2.4f;
        int screenW = event.getWindow().getGuiScaledWidth();
        int y = Math.round(event.getWindow().getGuiScaledHeight() * 0.14f);

        graphics.pose().pushPose();
        graphics.pose().translate(screenW / 2.0, y, 0);
        graphics.pose().scale(scale, scale, 1f);
        int w = font.width(text);
        // Soft shadow for readability
        graphics.drawString(font, text, -w / 2 + 1, 1, 0x88000000, false);
        graphics.drawString(font, text, -w / 2, 0, color, false);
        graphics.pose().popPose();
    }

    private static int lerpRgb(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = (int) Mth.lerp(t, ar, br);
        int g = (int) Mth.lerp(t, ag, bg);
        int bl = (int) Mth.lerp(t, ab, bb);
        return (r << 16) | (g << 8) | bl;
    }
}
