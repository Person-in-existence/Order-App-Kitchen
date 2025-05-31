package networking;


import android.util.Log;

import com.example.orderappkitchen.MainActivity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;

import networking.packets.Header;
import networking.packets.Packet;
import networking.packets.Type0;
import networking.packets.Type10;
import networking.packets.Type2;
import networking.packets.Type5;
import networking.packets.Type9;

class Server extends Thread {
    private ServerSocket socket;
    private volatile boolean accepting = false;
    private volatile boolean running = false;
    private MainActivity activity;
    private final ArrayList<Connection> connections = new ArrayList<>();
    private Connection serverConnection;
    private boolean serverJoinReturned = false;
    protected Server() {
        try {
            socket = new ServerSocket(Network.PORT);

        } catch (IOException e) {
            Log.e("networking.Server", "IOException");
            if (e.getMessage() != null) {
                Log.e("networking.Server", e.getMessage());
            }
            Log.e("networking.Server", Arrays.toString(e.getStackTrace()));
        }
    }

    protected void setAccepting(boolean accepting) {
        this.accepting = accepting;
    }
    protected void setActivity(MainActivity activity) {
        this.activity = activity;
    }


    public void run() {
        running = true;
        while (running) {
            try {
                if (accepting) {
                    Socket s = socket.accept();
                    Log.v("networking.Server", "New connection accept " + s.getInetAddress());

                    // Try and find an existing connection with the socket.
                    boolean foundExisting = false;
                    // Synchronize as another thread could break otherwise
                    synchronized (connections) {
                        for (Connection connection: connections) {
                            if (connection.ip.toString().equals(s.getInetAddress().toString())) {
                                Log.v("networking.Server", "Pre-existing connection found for reconnect, transferring to that");


                                Connection newConnection = new Connection(s, false, this::removeConnection, connection.getIdempotency(), connection.getReceivedIdempotencies());
                                connections.add(newConnection);

                                // Close the old connection (which removes it because of the closelistener)


                                foundExisting = true;
                                // Close the old connection
                                connection.close(false);
                                break;
                            }
                        }
                    }

                    if (!foundExisting) {
                        // Don't try reconnect - that is the connecting device's job.
                        Connection connection = new Connection(s, false, this::removeConnection);
                        connections.add(connection);
                    }

                } else {
                    socket.accept().close(); // Reject the connection
                }
            } catch (SocketTimeoutException e) {
                Log.d("networking.Server", "Socket Timeout");
            } catch (Exception e) {
                Log.e("networking.Server", "Exception " + e.getClass());
                if (e.getMessage() != null) {
                    Log.e("networking.Server", e.getMessage());
                }
                Log.e("networking.Server", Arrays.toString(e.getStackTrace()));
            }
        }
    }

    protected void joinServer(String ip, SuccessNotifier successNotifier){
        new Thread() {
            public void run() {
                // Close existing stuff
                end();
                if (serverConnection != null) {
                    if (serverConnection.isOpen()) {
                        serverConnection.close(true);
                    }
                }

                serverJoinReturned = false;

                // Create connection
                Socket socket = new Socket();
                try {
                    socket.connect(new InetSocketAddress(ip, Network.PORT));
                } catch (IOException e) {
                    successNotifier.success(false);
                }
                serverConnection = new Connection(socket, true, (ignored)-> activity.onExternalDisconnect());
                // Send packet
                Type0 packet = new Type0(new Header(Network.NETWORK_VERSION_NUMBER, (short) 0, serverConnection.getIdempotency()));
                packet.setSendListener(success -> {
                    serverJoinReturned = true;
                    successNotifier.success(success);
                    Log.d("networking.Network", "Returned, success: " + success);
                });
                serverConnection.sendPacket(packet);
                // Fetch orders
                Type10 orderPacket = new Type10(new Header(Network.NETWORK_VERSION_NUMBER, (short)10, serverConnection.getIdempotency()));
                serverConnection.sendPacket(orderPacket);
                try {
                    Thread.sleep(2000); // Wait 2 seconds
                } catch (InterruptedException ignored) {}
                // If false, trigger activity.joinedServer(false)
                if (!serverJoinReturned) {
                    Log.w("networking.Network", "Server join failed to return");
                    // Shut the connection - it hasn't worked.
                    // Remove the listener first so we don't move around
                    serverConnection.setListener(null);
                    serverConnection.close(false);
                    successNotifier.success(false);
                }
            }
        }.start();

    }

    protected void setExternalData() {
        if (serverConnection.isOpen()) {
            Packet packet = new Type2(new Header(Network.NETWORK_VERSION_NUMBER, (short) 2, serverConnection.getIdempotency()), Network.getSessionData());
            serverConnection.sendPacket(packet);
        }
    }
    protected void deleteExternalOrder(long orderID) {
        if (serverConnection.isOpen()) {
            Packet packet = new Type5(new Header(Network.NETWORK_VERSION_NUMBER, (short) 5, serverConnection.getIdempotency()), orderID);
            serverConnection.sendPacket(packet);
        }
    }

    /**
     * Sends a new type 2 to all current connections. This is used for when data is changed by the server.
     */
    public void resendInfo(SessionData data) {
        synchronized (connections) {
            for (Connection connection: connections) {
                Type2 packet = new Type2(new Header(Network.NETWORK_VERSION_NUMBER, (short) 2, connection.getIdempotency()), data);
                connection.sendPacket(packet); // This will want a confirmation and handle resending.
            }
        }
    }

    public void sendWaiterUpdates(Order order, int checksum, Connection noSend) {
        synchronized (connections) {
            for (Connection connection: connections) {
                // Don't send to the waiter that did the order - it has already updated
                if (connection == noSend) {
                    continue;
                }
                Type9 packet = new Type9(new Header(Network.NETWORK_VERSION_NUMBER, (short) 9, connection.getIdempotency()), order, checksum);
                connection.sendPacket(packet);
            }
        }
    }

    protected void leaveServer() {
        serverConnection.close(true);
    }

    protected void removeConnection(Connection connection) {
        synchronized (connections) {
            connections.remove(connection);
        }
        Log.d("networking.Server.removeConnection", "Connections: " + connections);
    }

    protected void end() {
        setAccepting(false);
        running = false;

        if (serverConnection != null) {
            if (serverConnection.isOpen()) {
                serverConnection.close(true);
            }
        }

        // Close the server socket (So we don't accept anything etc)
        try {
            socket.close();
        } catch (Exception ignored) {}
        synchronized (connections) {
            for (Connection connection : connections) {
                connection.close(true);
            }
        }

    }
    public boolean isRunning() {
        return running;
    }
}
