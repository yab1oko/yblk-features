package yblk.features.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yblk.features.world.FlightRules;

/** Stops the glide before the client sets the flag and tells the server it is flying. */
@Mixin(Player.class)
public class ElytraStartMixin {
    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void features$blockElytra(CallbackInfoReturnable<Boolean> cir) {
        if (FlightRules.blocksGlide((Player) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
