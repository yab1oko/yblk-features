package yblk.features.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yblk.features.client.BorderDangerClientEvents;

@Mixin(Level.class)
public abstract class LevelMixin {
    @Inject(method = "getRainLevel", at = @At("RETURN"), cancellable = true)
    private void features$borderRainLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        Level self = (Level) (Object) this;
        if (!self.isClientSide) {
            return;
        }
        float boost = BorderDangerClientEvents.rainWeatherBoost();
        if (boost > 0.001f) {
            cir.setReturnValue(Math.max(cir.getReturnValueF(), boost));
        }
    }
}
