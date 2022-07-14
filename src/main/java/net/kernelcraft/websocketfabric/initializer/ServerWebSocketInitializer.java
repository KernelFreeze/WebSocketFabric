package net.kernelcraft.websocketfabric.initializer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.kernelcraft.websocketfabric.WebSocketConstants;
import net.kernelcraft.websocketfabric.codec.FrameToPacketDecoder;
import net.kernelcraft.websocketfabric.codec.PacketToFrameEncoder;
import net.kernelcraft.websocketfabric.http.WebSocketPageHandler;
import net.kernelcraft.websocketfabric.mixin.ServerNetworkIoAccessor;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import org.jetbrains.annotations.NotNull;

public class ServerWebSocketInitializer extends ChannelInitializer<Channel> {
    private final MinecraftServer server;
    private final ServerNetworkIoAccessor networkIo;

    public ServerWebSocketInitializer(MinecraftServer server) {
        this.server = server;
        this.networkIo = (ServerNetworkIoAccessor) server.getNetworkIo();
    }

    @Override
    public void initChannel(@NotNull Channel channel) {
        setTCPNoDelay(channel);

        var rateLimit = server.getRateLimit();
        var clientConnection = new ClientConnection(NetworkSide.SERVERBOUND);

        networkIo.getConnections().add(clientConnection);
        clientConnection.setPacketListener(new ServerHandshakeNetworkHandler(server, clientConnection));

        channel.pipeline()
            .addLast(new HttpServerCodec())
            .addLast(new HttpObjectAggregator(WebSocketConstants.MAX_SIZE))
            .addLast(new WebSocketServerProtocolHandler(WebSocketConstants.WEBSOCKET_PATH, "minecraft", true))
            .addLast(new WebSocketPageHandler())
            .addLast("timeout", new ReadTimeoutHandler(30))
            .addLast("splitter", new ChannelDuplexHandler()) // no-op
            .addLast("decoder", new FrameToPacketDecoder(NetworkSide.SERVERBOUND))
            .addLast("prepender", new ChannelDuplexHandler())
            .addLast("encoder", new PacketToFrameEncoder(NetworkSide.CLIENTBOUND))
            .addLast("packet_handler", clientConnection);
    }

    private void setTCPNoDelay(@NotNull Channel ch) {
        try {
            ch.config().setOption(ChannelOption.TCP_NODELAY, true);
        } catch (ChannelException ignored) {
        }
    }
}