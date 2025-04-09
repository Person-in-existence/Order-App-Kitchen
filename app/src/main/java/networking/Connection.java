package networking;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;

import networking.packets.Header;
import networking.packets.Packet;
import networking.packets.Type0;
import networking.packets.Type1;
import networking.packets.Type10;
import networking.packets.Type11;
import networking.packets.Type2;
import networking.packets.Type3;
import networking.packets.Type4;
import networking.packets.Type5;
import networking.packets.Type6;
import networking.packets.Type8;
import networking.packets.Type9;

class Connection extends Thread {
    public static final int packetWaitTimeMs = 500;
    private volatile ConcurrentLinkedQueue<Packet> queue = new ConcurrentLinkedQueue<>();
    private final ArrayList<Integer> receivedIdempotencies = new ArrayList<>();
    private final ArrayList<ResponseWait> waiting = new ArrayList<>();
    private Socket socket;
    private volatile boolean open = true;
    protected final InetAddress ip;
    private int currentIdempotency;
    private DataOutputStream out;
    private DataInputStream in;
    private CloseListener listener;

    protected Connection(Socket socket, @Nullable CloseListener closeListener) {
        this.socket = socket;
        this.ip = socket.getInetAddress();
        this.listener = closeListener;
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
                    socket.setSoTimeout(50);
                    Header header = new Header(in);
                    if (header.versionNumber != Network.NETWORK_VERSION_NUMBER) {
                        // Version is incorrect, throw error message
                        Log.e("networking.Connection", "Incorrect version number: Expected " + Network.NETWORK_VERSION_NUMBER + "but received " + header.versionNumber + ".");
                    }

                    // Handle the packet
                    handlePacket(header);

                    // Receive the idempotency (Do afterwards so we don't mess up packet handling)
                    receivedIdempotencies.add(header.idempotencyToken);

                    // Remove from waiting responses if it exists
                    removeWaiting(header);
                } catch (SocketTimeoutException ignored) {}

                // Check for unreceived expired packets
                for (ResponseWait packet: waiting) {
                    if (packet.isExpired()) {
                        // Resend packet
                        packet.packet.send(out);
                    }
                }

                // Send a waiting packet (only do 1 at a time to give chance to check response)
                // TODO: evaluate if that is necessary
                Packet toSend = queue.poll();
                // Check whether there is anything
                if (toSend != null) {
                    toSend.send(out);
                    // Only add waiting now - we don't want it to time out before being sent
                    waiting.add(new ResponseWait(toSend, packetWaitTimeMs));
                }

            } catch (IOException e) {
                Log.e("networking.Connection", "Exception in connection with ip " + ip + Arrays.toString(e.getStackTrace()));
            }
        }
    }
    protected void removeWaiting(Header header) {
        for (ResponseWait packet: waiting) {
            // Only care about idempotency - this header is the sent one, not expected
            if (packet.packet.getHeader().idempotencyToken == header.idempotencyToken) {
                waiting.remove(packet);
                // Exit here - there should only be one.
                return;
            }
        }
    }
    protected void sendPacket(Packet packet) {
        queue.add(packet);
        // Don't add to waiting now - it could expire before we actually send it
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
    protected void close(boolean sendType4) {
        if (sendType4) {
            try {
                Packet packet = new Type4(new Header(Network.NETWORK_VERSION_NUMBER, (short) 4, getIdempotency()));
                packet.send(out);
            } catch (Exception e) {
                Log.e("network.Connection", "Error while sending type 4 closing packet");
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

    private void handleType0(Header header) throws IOException {
        // Send a type 2 packet
        SessionData data = Network.getSessionData();
        Type2 packet = new Type2(new Header(Network.NETWORK_VERSION_NUMBER, (short) 2, header.idempotencyToken), data);

        sendPacket(packet);
    }
    private void handleType1(Header header) throws IOException {
        Type1 packet = new Type1(header, in);
        Order order = packet.getOrder();
        // Check whether we have had the idempotency before we do anything
        if (!receivedIdempotencies.contains(packet.getHeader().idempotencyToken)) {
            Network.addOrder(order);
        } else {
            Log.w("OrderApp Networking", "Duplicate idempotency token from IP: " + ip);
        }
        // Send a response if everything worked (type 3, true)
        Type3 confirmPacket = new Type3(new Header(Network.NETWORK_VERSION_NUMBER, (short) 3, header.idempotencyToken), true);
        confirmPacket.send(out);
    }
    private void handleType2(Header header) throws IOException {
        Type2 packet = new Type2(header, in);
        SessionData data = packet.getData();

        Network.setSessionData(data);
    }
    private void handleType4(Header header){
        close(false);
        // Trigger the close listener
        listener.listen(this);
    }
    private void handleType5(Header header) throws IOException {
        Type5 packet = new Type5(header, in);
        long orderID = packet.getOrderID();

        Network.removeOrderByID(orderID);
        // Send confirmation
        Type3 confirmPacket = new Type3(new Header(Network.NETWORK_VERSION_NUMBER, (short) 3, header.idempotencyToken), true);
        confirmPacket.send(out);
    }
    private void handleType6(Header header) throws IOException {
        Type6 packet = new Type6(header, in);

        boolean success;
        if (packet.getIsAdd()) {
            success = Network.addOrderChecksum(packet.getOrder(),packet.getChecksum());
        } else {
            success = Network.removeOrderChecksum(packet.getOrder().orderID, packet.getChecksum());
        }

        if (!success) {
            // If we failed, send a type 10 to request information
            Type10 request = new Type10(new Header(Network.NETWORK_VERSION_NUMBER, (short) 10, header.idempotencyToken));
            sendPacket(request);
        }
    }
    private void handleType7(Header header) throws IOException {
        Type8 response = new Type8(new Header(Network.NETWORK_VERSION_NUMBER, (short) 8,getIdempotency()), Network.getDeviceType(), Network.getDeviceName());
        response.send(out);
    }

    private void handleType9(Header header) throws IOException {
        Type9 packet = new Type9(header, in);

        boolean success = Network.addRemoveItemsByAmount(packet.getOrder(), packet.getChecksum());

        if (!success) {
            // Create a type 0 request for information
            Type0 informationRequest = new Type0(new Header(Network.NETWORK_VERSION_NUMBER, (short) 0, getIdempotency()));
            sendPacket(informationRequest);
        }
    }
    private void handleType10(Header header) throws IOException {
        OrderData data = Network.getOrderData();
        Type11 dataPacket = new Type11(new Header(Network.NETWORK_VERSION_NUMBER, (short) 11, header.idempotencyToken), data);
        dataPacket.send(out);
    }
    private void handleType11(Header header) throws IOException {
        Type11 packet = new Type11(header, in);
        Network.setOrderData(packet.getData());
    }
    private void handlePacket(Header header) throws IOException {
        switch (header.type) {
            case 0:
                handleType0(header);
                break;
            case 1:
                handleType1(header);
                break;
            case 2:
                handleType2(header);
                break;
            case 4:
                handleType4(header);
                break;
            case 5:
                handleType5(header);
                break;
            case 6:
                handleType6(header);
                break;
            case 7:
                handleType7(header);
                break;
            case 8:
                System.err.println("Was sent a type 8 packet by " + ip.toString() + " - this shouldn't happen in an established connection.");
                break;
            case 9:
                handleType9(header);
                break;
            case 10:
                handleType10(header);
                break;
            case 11:
                handleType11(header);
                break;
            default:
                System.err.println("Unrecognised packet type in handlePacket of connection to IP " + ip.toString() + " - " + header.type);
        }
    }
    protected interface CloseListener {
        void listen(Connection connection);
    }
}
