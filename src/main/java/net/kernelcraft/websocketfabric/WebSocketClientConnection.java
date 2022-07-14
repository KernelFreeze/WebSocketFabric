package net.kernelcraft.websocketfabric;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.kernelcraft.websocketfabric.initializer.listener.ConnectedListener;
import net.kernelcraft.websocketfabric.mixin.ClientConnectionAccessor;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class WebSocketClientConnection extends ClientConnection {
    private boolean connected = false;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<ConnectedListener> connectionListeners = Lists.newArrayList();

    public WebSocketClientConnection(NetworkSide side) {
        super(side);
    }

    public void addConnectedListener(ConnectedListener listener) {
        this.connectionListeners.add(listener);
    }

    public void onConnected() {
        this.connected = true;

        for (var listener : this.connectionListeners) {
            try {
                listener.onConnected();
            } catch (Exception e) {
                LOGGER.error("Error while calling onConnected()", e);
            }
        }
    }

    @Override
    public void disconnect(Text disconnectReason) {
        var accessor = (ClientConnectionAccessor) this;
        var channel = accessor.getChannel();

        if (channel.isOpen()) {
            channel
                .writeAndFlush(new CloseWebSocketFrame())
                .addListener(ChannelFutureListener.CLOSE);
            accessor.setDisconnectReason(disconnectReason);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.fireChannelActive();

        var accessor = (ClientConnectionAccessor) this;

        accessor.setChannel(ctx.channel());
        accessor.setAddress(ctx.channel().remoteAddress());
    }

    @Override
    public void send(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback) {
        if (this.isOpen() && this.connected) {
            this.sendQueuedPackets();
            this.sendImmediately(packet, callback);
        } else {
            this.packetQueue.add(new QueuedPacket(packet, callback));
        }
    }

    @Override
    protected void sendQueuedPackets() {
        if (!this.connected) {
            return;
        }
        super.sendQueuedPackets();
    }
}
