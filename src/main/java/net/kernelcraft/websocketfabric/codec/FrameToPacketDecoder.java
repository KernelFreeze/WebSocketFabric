package net.kernelcraft.websocketfabric.codec;

import java.io.IOException;
import java.util.List;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import org.slf4j.Logger;

public class FrameToPacketDecoder extends MessageToMessageDecoder<WebSocketFrame> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final NetworkSide side;

    public FrameToPacketDecoder(NetworkSide side) {
        this.side = side;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame msg, List<Object> out) throws IOException {
        var packetByteBuf = new PacketByteBuf(msg.content().retain());
        var availableBytes = packetByteBuf.readableBytes();
        if (availableBytes == 0) {
            return;
        }

        var packetId = packetByteBuf.readVarInt();
        var packet = ctx.channel().attr(ClientConnection.PROTOCOL_ATTRIBUTE_KEY).get()
            .getPacketHandler(this.side, packetId, packetByteBuf);
        if (packet == null) {
            throw new IOException("Bad packet id " + packetId);
        }
        var protocolId = ctx.channel().attr(ClientConnection.PROTOCOL_ATTRIBUTE_KEY).get().getId();
        FlightProfiler.INSTANCE.onPacketReceived(protocolId, packetId, ctx.channel().remoteAddress(), availableBytes);

        if (packetByteBuf.readableBytes() > 0) {
            throw new IOException(
                "Packet %d/%d (%s) was larger than I expected, found %d bytes extra whilst reading packet %d".formatted(
                    ctx.channel().attr(ClientConnection.PROTOCOL_ATTRIBUTE_KEY).get()
                        .getId(), packetId, packet.getClass()
                        .getSimpleName(), packetByteBuf.readableBytes(), packetId));
        }
        out.add(packet);

        LOGGER.debug(ClientConnection.PACKET_RECEIVED_MARKER, " IN: [{}:{}] {}",
            ctx.channel().attr(ClientConnection.PROTOCOL_ATTRIBUTE_KEY).get(), packetId, packet.getClass().getName());
    }
}