package net.kernelcraft.websocketfabric.initializer;

import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.kernelcraft.websocketfabric.WebSocketClientConnection;
import net.kernelcraft.websocketfabric.WebSocketConstants;
import net.kernelcraft.websocketfabric.codec.FrameToPacketDecoder;
import net.kernelcraft.websocketfabric.codec.PacketToFrameEncoder;
import net.kernelcraft.websocketfabric.handler.ClientConnectedEventHandler;
import net.kernelcraft.websocketfabric.handler.ConnectedEventHandler;
import net.kernelcraft.websocketfabric.http.WebSocketPageHandler;
import net.kernelcraft.websocketfabric.mixin.ServerNetworkIoAccessor;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class ServerWebSocketInitializer extends ChannelInitializer<Channel> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final MinecraftServer server;
    private final ServerNetworkIoAccessor networkIo;

    public ServerWebSocketInitializer(MinecraftServer server) {
        this.server = server;
        this.networkIo = (ServerNetworkIoAccessor) server.getNetworkIo();
    }

    @Override
    public void initChannel(@NotNull Channel channel) {
        setTCPNoDelay(channel);
        var clientConnection = new WebSocketClientConnection(NetworkSide.SERVERBOUND);

        networkIo.getConnections().add(clientConnection);
        clientConnection.setPacketListener(new ServerHandshakeNetworkHandler(server, clientConnection));

        channel.pipeline()
            .addLast(new HttpServerCodec())
            .addLast(new HttpObjectAggregator(WebSocketConstants.MAX_SIZE))
            .addLast(new WebSocketServerProtocolHandler(WebSocketConstants.WEBSOCKET_PATH, "minecraft", true))
            .addLast(new WebSocketPageHandler())
            .addLast(new ConnectedEventHandler(clientConnection))
            .addLast("timeout", new ReadTimeoutHandler(30))
            .addLast("splitter", new ChannelDuplexHandler()) // no-op
            .addLast("decoder", new FrameToPacketDecoder(NetworkSide.SERVERBOUND))
            .addLast("prepender", new ChannelDuplexHandler())
            .addLast("encoder", new PacketToFrameEncoder(NetworkSide.CLIENTBOUND))
            .addLast("packet_handler", clientConnection);

        clientConnection.addConnectedListener(() -> {
            LOGGER.info("Client '{}' connected", clientConnection.getAddress());
            clientConnection.setState(NetworkState.HANDSHAKING);
        });
    }

    private void setTCPNoDelay(@NotNull Channel ch) {
        try {
            ch.config().setOption(ChannelOption.TCP_NODELAY, true);
        } catch (ChannelException ignored) {
        }
    }
}