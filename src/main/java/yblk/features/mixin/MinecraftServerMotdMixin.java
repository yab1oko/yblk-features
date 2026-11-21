package yblk.features.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yblk.features.server.LegacyText;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMotdMixin {
    @Redirect(
            method = "buildServerStatus",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;nullToEmpty(Ljava/lang/String;)Lnet/minecraft/network/chat/Component;"
            )
    )
    private Component features$coloredMotd(String motd) {
        return LegacyText.parse(motd == null ? "" : motd);
    }
}
