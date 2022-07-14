package net.kernelcraft.websocketfabric.mixin;

import java.util.List;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.ServerNetworkIo;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerNetworkIo.class)
public interface ServerNetworkIoAccessor {
    @Accessor
    @NonNull
    List<ClientConnection> getConnections();
}
