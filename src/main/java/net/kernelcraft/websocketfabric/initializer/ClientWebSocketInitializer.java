package net.kernelcraft.websocketfabric.initializer;

import java.net.URI;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.kernelcraft.websocketfabric.WebSocketClientConnection;
import net.kernelcraft.websocketfabric.WebSocketConstants;
import net.kernelcraft.websocketfabric.handler.ClientConnectedEventHandler;
import net.kernelcraft.websocketfabric.codec.FrameToPacketDecoder;
import net.kernelcraft.websocketfabric.codec.PacketToFrameEncoder;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import org.jetbrains.annotations.NotNull;

public class ClientWebSocketInitializer extends ChannelInitializer<Channel> {
    private final WebSocketClientConnection clientConnection;
    private final URI uri;

    public ClientWebSocketInitializer(WebSocketClientConnection clientConnection, URI uri) {
        this.clientConnection = clientConnection;
        this.uri = uri;
    }

    @Override
    protected void initChannel(@NotNull Channel channel) {
        setTCPNoDelay(channel);

        var handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, "minecraft", false,
            EmptyHttpHeaders.INSTANCE, 1280000);

        channel.pipeline()
            .addLast(new HttpClientCodec())
            .addLast(new HttpObjectAggregator(WebSocketConstants.MAX_SIZE))
            .addLast(new WebSocketClientProtocolHandler(handshaker))
            .addLast(WebSocketClientCompressionHandler.INSTANCE)
            .addLast(new ClientConnectedEventHandler(clientConnection))
            .addLast("timeout", new ReadTimeoutHandler(30))
            .addLast("splitter", new ChannelDuplexHandler()) // no-op
            .addLast("decoder", new FrameToPacketDecoder(NetworkSide.CLIENTBOUND))
            .addLast("prepender", new ChannelDuplexHandler()) // no-op
            .addLast("encoder", new PacketToFrameEncoder(NetworkSide.SERVERBOUND))
            .addLast("packet_handler", clientConnection);

        clientConnection.addConnectedListener(() -> clientConnection.setState(NetworkState.HANDSHAKING));
    }

    private static void setTCPNoDelay(Channel channel) {
        try {
            channel.config().setOption(ChannelOption.TCP_NODELAY, true);
        } catch (ChannelException channelException) {
            // empty catch block
        }
    }
}