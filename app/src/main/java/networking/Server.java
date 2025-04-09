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

class Server extends Thread {
    private ServerSocket socket;
    private volatile boolean accepting = false;
    private volatile boolean running = true;
    private MainActivity activity;
    private ArrayList<Connection> connections;
    private Connection serverConnection;
    protected Server() {
        try {
            socket = new ServerSocket();

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
                    Socket connection = socket.accept();

                } else {
                    socket.accept().close(); // Reject the connection
                }
            } catch (SocketTimeoutException e) {
                Log.d("networking.Server", "Socket Timeout");
            } catch (Exception e) {
                Log.e("networking.Server", "IOException");
                if (e.getMessage() != null) {
                    Log.e("networking.Server", e.getMessage());
                }
                Log.e("networking.Server", Arrays.toString(e.getStackTrace()));
            }
        }
    }

    protected void joinServer(InetAddress ip) throws IOException {
        end();
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(ip, Network.PORT));
        serverConnection = new Connection(socket, this::removeConnection);

    }

    protected void leaveServer() {
        serverConnection.close(true);
    }
    protected void removeConnection(Connection connection) {
        connections.remove(connection);
    }

    protected void end() {
        setAccepting(false);
        for (Connection connection : connections) {
            connection.close(false);
        }
        running = false;
    }
}
