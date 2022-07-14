package net.kernelcraft.websocketfabric.initializer;

import java.net.URI;

import io.netty.channel.Channel;
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
import net.kernelcraft.websocketfabric.client.ClientWebsocketHandler;
import net.kernelcraft.websocketfabric.WebSocketConstants;
import net.minecraft.network.ClientConnection;
import org.jetbrains.annotations.NotNull;

public class ClientWebSocketInitializer extends ChannelInitializer<Channel> {
    private final ClientConnection clientConnection;
    private final URI uri;

    public ClientWebSocketInitializer(ClientConnection clientConnection, URI uri) {
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
            .addLast(new ClientWebsocketHandler(clientConnection));
    }

    private static void setTCPNoDelay(Channel channel) {
        try {
            channel.config().setOption(ChannelOption.TCP_NODELAY, true);
        } catch (ChannelException channelException) {
            // empty catch block
        }
    }
}