package net.kernelcraft.websocketfabric.codec;

import java.io.IOException;
import java.util.List;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.kernelcraft.websocketfabric.WebSocketConstants;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketEncoderException;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import org.slf4j.Logger;

public class PacketToFrameEncoder extends MessageToMessageEncoder<Packet<?>> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final NetworkSide side;

    public PacketToFrameEncoder(NetworkSide side) {
        this.side = side;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet<?> packet, List<Object> out) throws Exception {
        var networkState = ctx.channel().attr(ClientConnection.PROTOCOL_ATTRIBUTE_KEY).get();
        if (networkState == null) {
            throw new RuntimeException("ConnectionProtocol unknown: " + packet);
        }

        var packetId = networkState.getPacketId(this.side, packet);
        if (packetId == null) {
            throw new IOException("Can't serialize unregistered packet");
        }

        var packetByteBuf = PacketByteBufs.create();
        packetByteBuf.writeVarInt(packetId);

        try {
            var writerIndex = packetByteBuf.writerIndex();
            packet.write(packetByteBuf);

            var packetSize = packetByteBuf.writerIndex() - writerIndex;
            if (packetSize > WebSocketConstants.MAX_SIZE) {
                throw new IllegalArgumentException(
                    "Packet too big (is %d, should be less than %d): %s".formatted(packetSize, WebSocketConstants.MAX_SIZE, packet));
            }

            var protocolId = ctx.channel().attr(ClientConnection.PROTOCOL_ATTRIBUTE_KEY).get().getId();
            FlightProfiler.INSTANCE.onPacketSent(protocolId, packetId, ctx.channel().remoteAddress(), packetSize);
        } catch (Throwable throwable) {
            LOGGER.error("Error encoding packet {}", packetId, throwable);
            if (packet.isWritingErrorSkippable()) {
                throw new PacketEncoderException(throwable);
            }
            throw throwable;
        }

        out.add(new BinaryWebSocketFrame(packetByteBuf));
    }
}
