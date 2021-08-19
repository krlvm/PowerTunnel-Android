/*
 * This file is part of PowerTunnel-Android.
 *
 * PowerTunnel-Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PowerTunnel-Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerTunnel-Android.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.krlvm.powertunnel.utilities;

import java.util.Arrays;
import java.util.LinkedList;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import ru.krlvm.powertunnel.PowerTunnel;

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
