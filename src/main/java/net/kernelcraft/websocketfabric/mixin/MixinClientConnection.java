package net.kernelcraft.websocketfabric.mixin;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.kernelcraft.websocketfabric.WebSocketClientConnection;
import net.kernelcraft.websocketfabric.initializer.ClientWebSocketInitializer;
import net.kernelcraft.websocketfabric.WebSocketConstants;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.util.Lazy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings("deprecation")
@Mixin(ClientConnection.class)
public class MixinClientConnection {
    @Shadow
    @Final
    public static Lazy<EpollEventLoopGroup> EPOLL_CLIENT_IO_GROUP;

    @Shadow
    @Final
    public static Lazy<NioEventLoopGroup> CLIENT_IO_GROUP;

    /**
     * @author KernelFreeze
     * @reason Overwritten to use WebSocketInitializer
     */
    @Overwrite
    public static ClientConnection connect(InetSocketAddress address, boolean useEpoll) throws URISyntaxException {
        Lazy<? extends EventLoopGroup> lazy;
        Class<? extends SocketChannel> socketChannel;

        var clientConnection = new WebSocketClientConnection(NetworkSide.CLIENTBOUND);
        if (Epoll.isAvailable() && useEpoll) {
            socketChannel = EpollSocketChannel.class;
            lazy = EPOLL_CLIENT_IO_GROUP;
        } else {
            socketChannel = NioSocketChannel.class;
            lazy = CLIENT_IO_GROUP;
        }

        var uri = new URI("ws://" + address.getHostString() + ":" + address.getPort() + WebSocketConstants.WEBSOCKET_PATH);
        new Bootstrap()
            .group(lazy.get())
            .handler(new ClientWebSocketInitializer(clientConnection, uri))
            .channel(socketChannel)
            .connect(address.getAddress(), address.getPort())
            .syncUninterruptibly();
        return clientConnection;
    }
}
