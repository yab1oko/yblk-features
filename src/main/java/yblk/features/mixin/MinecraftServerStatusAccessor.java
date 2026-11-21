package yblk.features.mixin;

import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

@Mixin(MinecraftServer.class)
public interface MinecraftServerStatusAccessor {
    @Accessor("statusIcon")
    void features$setStatusIcon(ServerStatus.Favicon icon);

    @Invoker("loadStatusIcon")
    Optional<ServerStatus.Favicon> features$loadStatusIcon();
}
