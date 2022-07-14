package net.kernelcraft.websocketfabric.mixin;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.kernelcraft.websocketfabric.initializer.ServerWebSocketInitializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerNetworkIo;
import net.minecraft.util.Lazy;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings("deprecation")
@Mixin(ServerNetworkIo.class)
public abstract class MixinServerNetworkIo {
    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    @Final
    private List<ChannelFuture> channels;

    @Shadow
    @Final
    public static Lazy<EpollEventLoopGroup> EPOLL_CHANNEL;

    @Shadow
    @Final
    public static Lazy<NioEventLoopGroup> DEFAULT_CHANNEL;

    @Shadow
    @Final
    private static Logger LOGGER;

    /**
     * @author KernelFreeze
     * @reason Overwritten to use WebSocketInitializer
     */
    @SuppressWarnings("SynchronizeOnNonFinalField")
    @Overwrite
    public void bind(@Nullable InetAddress address, int port) throws IOException {
        synchronized (this.channels) {
            Lazy<? extends EventLoopGroup> lazy;
            Class<? extends ServerSocketChannel> socketChannel;

            if (Epoll.isAvailable() && this.server.isUsingNativeTransport()) {
                socketChannel = EpollServerSocketChannel.class;
                lazy = EPOLL_CHANNEL;
                LOGGER.info("Using epoll channel type");
            } else {
                socketChannel = NioServerSocketChannel.class;
                lazy = DEFAULT_CHANNEL;
                LOGGER.info("Using default channel type");
            }

            this.channels.add(new ServerBootstrap()
                .channel(socketChannel)
                .childHandler(new ServerWebSocketInitializer(server))
                .group(lazy.get())
                .localAddress(address, port)
                .bind()
                .syncUninterruptibly());
        }
    }
}
