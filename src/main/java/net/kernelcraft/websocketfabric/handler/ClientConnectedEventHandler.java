package net.kernelcraft.websocketfabric.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleUserEventChannelHandler;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import net.kernelcraft.websocketfabric.WebSocketClientConnection;

public class ClientConnectedEventHandler extends
    SimpleUserEventChannelHandler<WebSocketClientProtocolHandler.ClientHandshakeStateEvent> {
    private final WebSocketClientConnection clientConnection;

    public ClientConnectedEventHandler(WebSocketClientConnection clientConnection) {
        this.clientConnection = clientConnection;
    }

    @Override
    protected void eventReceived(ChannelHandlerContext ctx,
        WebSocketClientProtocolHandler.ClientHandshakeStateEvent event) {
        if (event == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            clientConnection.onConnected();
        } else {
            ctx.fireUserEventTriggered(event);
        }
    }
}