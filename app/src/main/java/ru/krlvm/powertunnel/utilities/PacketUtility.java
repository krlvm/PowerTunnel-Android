package ru.krlvm.powertunnel.utilities;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import ru.krlvm.powertunnel.PowerTunnel;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * Utility with working with
 * HTTP/HTTPS packets
 *
 * @author krlvm
 */
public class PacketUtility {

    /**
     * Retrieves list of packet's ByteBuf chunks
     *
     * @param buf - ByteBuf of packet
     * @return - ByteBuf chunks
     */
    public static LinkedList<ByteBuf> bufferChunk(ByteBuf buf) {
        return bufferChunk(buf, PowerTunnel.DEFAULT_CHUNK_SIZE);
    }

    /**
     * Retrieves list of packet's ByteBuf chunks
     *
     * @param buf - ByteBuf of packet
     * @param chunkSize - size of chunk
     * @return - ByteBuf chunks
     */
    public static LinkedList<ByteBuf> bufferChunk(ByteBuf buf, int chunkSize) {
        LinkedList<byte[]> chunks = chunk(buf, chunkSize);
        LinkedList<ByteBuf> buffers = new LinkedList<>();
        for (byte[] chunk : chunks) {
            buffers.add(Unpooled.wrappedBuffer(chunk));
        }
        return buffers;
    }

    /**
     * Retrieves list (byte[]) of packet's ByteBuf chunks
     *
     * @param buf - ByteBuf of packet
     * @return - ByteBuf chunks (byte[])
     */
    public static LinkedList<byte[]> chunk(ByteBuf buf) {
        return chunk(buf, PowerTunnel.DEFAULT_CHUNK_SIZE);
    }

    /**
     * Retrieves list (byte[]) of packet's ByteBuf chunks
     *
     * @param buf - ByteBuf of packet
     * @param chunkSize - size of chunk
     * @return - ByteBuf chunks (byte[])
     */
    public static LinkedList<byte[]> chunk(ByteBuf buf, int chunkSize) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        int len = bytes.length;
        LinkedList<byte[]> byteChunks = new LinkedList<>();
        if(PowerTunnel.FULL_CHUNKING) {
            int i = 0;
            while (i < len) {
                byteChunks.add(Arrays.copyOfRange(bytes, i, i += chunkSize));
            }
        } else {
            byteChunks.add(Arrays.copyOfRange(bytes, 0, chunkSize));
            byteChunks.add(Arrays.copyOfRange(bytes, chunkSize, len));
        }
        return byteChunks;
    }
}
