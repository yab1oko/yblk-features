package yblk.features.mixin;

import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.world.FlightRules;

/** Clients ask to start elytra and to fly. The server drops those packets when the dimension forbids them. */
@Mixin(ServerGamePacketListenerImpl.class)
public class FlightPacketMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerCommand", at = @At("HEAD"), cancellable = true)
    private void features$blockFallFlying(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        if (packet.getAction() != ServerboundPlayerCommandPacket.Action.START_FALL_FLYING) {
            return;
        }
        if (FlightRules.deniesElytraStart(player)) {
            FlightRules.stopGlide(player);
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerAbilities", at = @At("HEAD"), cancellable = true)
    private void features$blockModFlight(ServerboundPlayerAbilitiesPacket packet, CallbackInfo ci) {
        if (!packet.isFlying() || !FlightRules.deniesCreativeFlight(player)) {
            return;
        }
        FlightRules.groundFlight(player);
        FlightRules.tell(player, FlightRules.Deny.FLIGHT, null);
        ci.cancel();
    }
}
