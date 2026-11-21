package yblk.features.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yblk.features.config.FeaturesConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
    private void features$hidePing(CallbackInfo ci) {
        if (FeaturesConfig.serverTweaksEnabled && FeaturesConfig.serverHidePing) {
            ci.cancel();
        }
    }

    @Inject(method = "getPlayerInfos", at = @At("RETURN"), cancellable = true)
    private void features$sortByRole(CallbackInfoReturnable<List<PlayerInfo>> cir) {
        if (!FeaturesConfig.serverTweaksEnabled || !FeaturesConfig.serverSortTab || cir.getReturnValue() == null) {
            return;
        }
        List<PlayerInfo> sorted = new ArrayList<>(cir.getReturnValue());
        sorted.sort(Comparator.comparingInt(PlayerTabOverlayMixin::rank)
                .thenComparing(info -> info.getProfile().getName(), String.CASE_INSENSITIVE_ORDER));
        cir.setReturnValue(sorted);
    }

    private static int rank(PlayerInfo info) {
        String prefix = info.getProfile().getId() + " ";
        for (String line : FeaturesConfig.serverRoles) {
            if (!line.startsWith(prefix)) {
                continue;
            }
            String[] parts = line.split(" ");
            if (parts.length < 2) {
                continue;
            }
            String role = parts[1];
            if ("operator".equals(role)) {
                return 0;
            }
            if ("moderator".equals(role)) {
                return 1;
            }
        }
        return 2;
    }
}
