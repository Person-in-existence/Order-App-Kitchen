package networking;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

class NetworkScanner {
    public static final int scanWaitTime = 10000; // Time in ms
    public static final int expectedStringLengthPosition = 2+2+4+2;
    public static final int expectedSentPacketLength = 2+2+4;
    public static void scan(NewDevice newDevice, Timeout timeout)  {
        Device[] devices = new Device[256];

        String thisPart = Network.getIPAddress().split("\\.")[3];
        int finalPart = Integer.parseInt(thisPart);
        long endTime = System.currentTimeMillis() + scanWaitTime;
        ByteBuffer toSend = ByteBuffer.allocate(expectedSentPacketLength);

        // Version Number
        toSend.putShort(Network.NETWORK_VERSION_NUMBER);
        // Packet type (7)
        toSend.putShort((short) 7);
        // Idempotency (-1 so we don't mess up packets)
        toSend.putInt(-1);
        toSend.rewind();
        try{
            Selector selector = Selector.open();
            for (int index = 0; index < 256; index++) {
                try {
                    String ip = Network.subnet + index;
                    SocketChannel socketChannel = SocketChannel.open();
                    // Set it to non-blocking (so we dont need a thread per connection)
                    socketChannel.configureBlocking(false);
                    socketChannel.connect(new InetSocketAddress(ip, Network.PORT));

                    SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
                } catch (Exception ignored) {System.err.println(index);}
            }

            while (System.currentTimeMillis() < endTime) {
                int number = selector.select(20);
                if (number == 0) {
                    continue;
                }
                // Handle connections
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                for (SelectionKey key: selectedKeys) {
                    SocketChannel socket = (SocketChannel) key.channel();

                    // Connectable key - we finish connection and then send packet
                    if (key.isConnectable()) {
                        // Attach data
                        SocketData items = new SocketData(ByteBuffer.allocate(256), toSend);
                        key.attach(items);
                        try {
                            if (socket.finishConnect()) {
                                socket.write(items.toSend);
                            }

                        } catch (IOException e) {
                            socket.close();
                        }
                    } else if (key.isReadable()) {
                        SocketData items = (SocketData) key.attachment();
                        ByteBuffer buffer = items.buffer;

                        int read = socket.read(buffer);

                        if (items.isReady()) {
                            // Fire new device
                            newDevice.create(items.makeDevice(((InetSocketAddress) socket.getRemoteAddress()).getAddress()));
                            // Remove the socket
                            socket.close();
                        }

                        if (read == -1) {
                            // Connection closed
                            socket.close();
                        }
                    } else if (key.isWritable()) {
                        SocketData data = (SocketData) key.attachment();
                        if (data.toSend.hasRemaining()) {
                            socket.write(data.toSend);
                        }
                    }

                }
            }
            Log.d("networking.NetworkScanner", "Timeout reached, closing");
            // Close all
            for (SelectionKey channel : selector.keys()) {
                channel.channel().close();
            }
            timeout.timeout();



            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }
    public interface NewDevice {
        void create(Device device);
    }
    public interface Timeout {
        void timeout();
    }
    private static class SocketData {
        public ByteBuffer toSend;
        public ByteBuffer buffer;
        public int expectedLength = -1;
        public SocketData(ByteBuffer buffer, ByteBuffer toSend) {
            this.buffer = buffer;
            this.toSend = toSend.duplicate();
        }
        public boolean isReady() {
            Log.d("NetworkScanner", "Checking if socket is ready");
            // Check whether we can read the string length
            if (expectedLength == -1 && buffer.position() > expectedStringLengthPosition) {
                try {
                    // Mark and reset mean we don't advance the buffer
                    buffer.mark();
                    // +4 for the actual thing we just read.
                    expectedLength = buffer.getInt(expectedStringLengthPosition)*2 + expectedStringLengthPosition + 4;
                    buffer.reset();
                } catch (IndexOutOfBoundsException ignored) {
                    // Return false if there isnt space for the int
                    return false;
                }
            } else {
                return false;
            }
            return buffer.position() == expectedLength;
        }
        public Device makeDevice(InetAddress ip) {
            // Parse the packet
            buffer.rewind();
            short version = buffer.getShort();
            short type = buffer.getShort();
            int idempotency = buffer.getInt();

            short deviceType = buffer.getShort();

            int stringLength = buffer.getInt();
            StringBuilder stringBuilder = new StringBuilder(stringLength);
            for (int index = 0; index < stringLength; index++) {
                stringBuilder.append(buffer.getChar());
            }
            String name = stringBuilder.toString();

            return new Device(name, ip, deviceType, version);
        }
    }

}
