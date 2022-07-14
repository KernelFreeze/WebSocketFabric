package net.kernelcraft.websocketfabric.client;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleUserEventChannelHandler;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.kernelcraft.websocketfabric.codec.FrameToPacketDecoder;
import net.kernelcraft.websocketfabric.codec.PacketToFrameEncoder;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;

public class ClientWebsocketHandler extends
    SimpleUserEventChannelHandler<WebSocketClientProtocolHandler.ClientHandshakeStateEvent> {
    private final ClientConnection clientConnection;

    public ClientWebsocketHandler(ClientConnection clientConnection) {
        this.clientConnection = clientConnection;
    }

    @Override
    protected void eventReceived(ChannelHandlerContext ctx,
        WebSocketClientProtocolHandler.ClientHandshakeStateEvent event) throws Exception {
        if (event == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            // Install the serializer and deserializer
            ctx.pipeline()
                .addLast("timeout", new ReadTimeoutHandler(30))
                .addLast("splitter", new ChannelDuplexHandler()) // no-op
                .addLast("decoder", new FrameToPacketDecoder(NetworkSide.CLIENTBOUND))
                .addLast("prepender", new ChannelDuplexHandler()) // no-op
                .addLast("encoder", new PacketToFrameEncoder(NetworkSide.SERVERBOUND))
                .addLast("packet_handler", clientConnection);

            clientConnection.channelActive(ctx);
        } else {
            ctx.fireUserEventTriggered(event);
        }
    }
}