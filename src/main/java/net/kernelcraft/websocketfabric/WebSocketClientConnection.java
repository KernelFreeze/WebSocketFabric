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
import net.minecraft.network.NetworkState;
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
        var channel = getChannel();

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
        this.packetQueue.add(new QueuedPacket(packet, callback));
        this.sendQueuedPackets();
    }

    private NetworkState getState() {
        return getChannel().attr(PROTOCOL_ATTRIBUTE_KEY).get();
    }

    private Channel getChannel() {
        var accessor = (ClientConnectionAccessor) this;
        return accessor.getChannel();
    }

    @Override
    protected void sendImmediately(Packet<?> packet,
        @Nullable GenericFutureListener<? extends Future<? super Void>> callback) {
        var packetState = NetworkState.getPacketHandlerState(packet);
        var protocolState = this.getState();

        var newState = packetState != protocolState;

        if (getChannel().eventLoop().inEventLoop()) {
            if (newState) {
                this.setState(packetState);
            }
            doSendPacket(packet, callback);
        } else {
            // Note: In newer versions of Netty, we could use AbstractEventExecutor.LazyRunnable to avoid a wakeup.
            // This has the advantage of requiring slightly less code.
            // However, in practice, (almost) every write will use a WriteTask which doesn't wake up the event loop.
            // The only exceptions are transitioning states (very rare) and when a listener is provided (but this is
            // only upon disconnect of a client). So we can sit back and enjoy the GC savings.
            if (!newState && callback == null) {
                var voidPromise = getChannel().voidPromise();

                getChannel().writeAndFlush(packet, voidPromise);
            } else {
                // Fallback.
                if (newState) {
                    getChannel().config().setAutoRead(false);
                }

                getChannel().eventLoop().execute(() -> {
                    if (newState) {
                        this.setState(packetState);
                    }
                    doSendPacket(packet, callback);
                });
            }
        }
    }

    private void doSendPacket(Packet<?> packet,
        @Nullable GenericFutureListener<? extends Future<? super Void>> callback) {
        if (callback == null) {
            getChannel().write(packet, getChannel().voidPromise());
        } else {
            var channelFuture = getChannel().write(packet);
            channelFuture.addListener(callback);
            channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }

        getChannel().flush();
    }

    @Override
    protected void sendQueuedPackets() {
        if (!this.connected) {
            return;
        }
        super.sendQueuedPackets();
    }
}
