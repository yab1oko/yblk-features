package yblk.features.mixin;

import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.restrict.CommandBlocker;

/** The packet root is a copy. Prune it after the merge and before it is sent. */
@Mixin(Commands.class)
public class CommandsMixin {
    @Inject(method = "sendCommands", at = @At("HEAD"))
    private void features$rememberPlayer(ServerPlayer player, CallbackInfo ci) {
        CommandBlocker.SENDING.set(player);
    }

    @ModifyArg(
            method = "sendCommands",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ClientboundCommandsPacket;<init>(Lcom/mojang/brigadier/tree/RootCommandNode;)V"
            )
    )
    private RootCommandNode<SharedSuggestionProvider> features$pruneCommands(RootCommandNode<SharedSuggestionProvider> root) {
        ServerPlayer player = CommandBlocker.SENDING.get();
        if (player != null) {
            CommandBlocker.prune(root, player);
        }
        return root;
    }

    @Inject(method = "sendCommands", at = @At("TAIL"))
    private void features$forgetPlayer(ServerPlayer player, CallbackInfo ci) {
        CommandBlocker.SENDING.remove();
    }
}
