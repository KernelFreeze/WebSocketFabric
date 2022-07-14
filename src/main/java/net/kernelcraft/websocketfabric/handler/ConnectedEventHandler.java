package net.kernelcraft.websocketfabric.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleUserEventChannelHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import net.kernelcraft.websocketfabric.WebSocketClientConnection;

public class ConnectedEventHandler extends
    SimpleUserEventChannelHandler<WebSocketServerProtocolHandler.HandshakeComplete> {
    private final WebSocketClientConnection clientConnection;

    public ConnectedEventHandler(WebSocketClientConnection clientConnection) {
        this.clientConnection = clientConnection;
    }

    @Override
    protected void eventReceived(ChannelHandlerContext ctx,
        WebSocketServerProtocolHandler.HandshakeComplete event) {
        clientConnection.onConnected();
    }
}