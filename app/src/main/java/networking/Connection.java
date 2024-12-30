package networking;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Queue;

class Connection extends Thread {
    private volatile Queue<Packet> queue;
    private Socket socket;
    private volatile boolean open = true;
    protected final InetAddress ip;
    private int currentIdempotency;
    private DataOutputStream out;
    private DataInputStream in;

    protected Connection(Socket socket) {
        this.socket = socket;
        this.ip = socket.getInetAddress();
    }
    public void run() {
        try {
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            Log.e("networking.Connection", "Making socket parts failed for connection IP: "+ socket.getInetAddress().toString());
            if (e.getMessage() != null) {
                Log.e("networking.Connection", e.getMessage());
            }
            Log.e("networking.Connection", Arrays.toString(e.getStackTrace()));
        }
        while (open) {
            try {
                try {
                    socket.setSoTimeout(10);
                    short versionNumber = in.readShort();
                    // If nothing can be read, it throws a socket timeout exception taking us to writing
                    // If we get here, we must have read a version number, so increase the socket timeout so we dont get stuck in the middle of a packet
                    socket.setSoTimeout(1000);
                    if (versionNumber != Network.NETWORK_VERSION_NUMBER) {
                        // Version is incorrect, throw error message
                        Log.e("networking.Connection", "Incorrect version number: Expected " + Network.NETWORK_VERSION_NUMBER + "but received " + versionNumber + ".");
                    }
                    // Get the packet type
                    short packetType = in.readShort();
                    int idempotencyToken = in.readInt();

                    Object[] packetBody = Packet.receiveBody(packetType, in);

                    Packet recievedPacket = new Packet(versionNumber, packetType, idempotencyToken, packetBody);
                    // For some types, a response is required:
                    

                } catch (SocketTimeoutException e) {
                    socket.setSoTimeout(1000);
                }
            } catch (Exception e) {

            }
        }
    }
    protected void sendPacket(Packet packet) {
        queue.add(packet);
    }

    protected void reconnect() throws IOException {
        try {
            socket.close();
        } catch (Exception e) {
            Log.e("networking.Connection", "Socket close failed");
            if (e.getMessage() != null) {
                Log.e("networking.Connection", e.getMessage());
            }
            Log.e("networking.Connection", Arrays.toString(e.getStackTrace()));
        }
        // Open a new socket
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, Network.PORT));


    }

    protected int getIdempotency() {
        currentIdempotency++;
        return currentIdempotency;
    }
    protected void close(boolean sendType5) {
        if (sendType5) {
            try {
                Packet packet = new Packet(Network.NETWORK_VERSION_NUMBER, (short) 5, getIdempotency(), new Object[0]);
                packet.send(out);
            } catch (Exception e) {
                Log.e("network.Connection", "Error while sending type 5 closing packet");
                if (e.getMessage() != null) {
                    Log.e("network.Connection", e.getMessage());
                }
                Log.e("network.Connection", Arrays.toString(e.getStackTrace()));
            }
        }
        open = false;
        try {
            socket.close();
        } catch (Exception e) {
            Log.e("network.Connection", "Closing socket failed");
        }
    }
}
