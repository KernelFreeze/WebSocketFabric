package net.kernelcraft.websocketfabric.mixin;

import java.net.SocketAddress;

import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientConnection.class)
public interface ClientConnectionAccessor {
    @Accessor
    void setChannel(Channel channel);

    @Accessor
    void setAddress(SocketAddress address);

    @Accessor
    Channel getChannel();

    @Accessor
    SocketAddress getAddress();

    @Accessor
    void setDisconnectReason(@Nullable Text disconnectReason);
}
