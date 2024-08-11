package com.example.orderappkitchen;

import static android.app.PendingIntent.getActivity;

import android.util.Log;

import androidx.fragment.app.Fragment;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class Server extends Thread {
    public ServerSocket socket;
    public MainActivity parent;
    public ArrayList<Connection> connections = new ArrayList<>();
    public void run() {
        try {
            socket = new ServerSocket(65433);
            socket.setSoTimeout(30000);
        } catch (IOException e) {
            Log.e("OrderAppKitchen Server", "Error: " + Arrays.toString(e.getStackTrace()));
        }
        while (true) {
            try {
                System.out.println("LoopAgain");
                Socket current = socket.accept();
                System.out.println("Connection accepted");
                if (current != null) {
                    // Check if there is a connection for this address
                    int index = getConnection(current.getInetAddress());
                    if (index == -1) {
                        Connection connection = new Connection(current, this);
                        connections.add(connection);
                        // Run the monitor subprogram on a separate thread.
                        new Thread(connection::monitor).start();
                    } else {
                        connections.get(index).reconnect(current);
                    }
                }

            }catch (SocketTimeoutException s) {
                System.out.println("SOCKETTIME");
            }
            catch (Exception e) {
                Log.e("OrderAppKitchen Server", "Error: " + Arrays.toString(e.getStackTrace()));
            }
        }
    }
    public static void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
            Log.d("ExceptionServer", "Waiting.");
        }
    }
    public static Order newOrder(String name, ArrayList<Integer> ordered, MainActivity parent) {
        Order order = new Order();
        order.setOrders(ordered);
        order.setName(name);
        order.setItems(parent.getItems());
        return order;
    }
    public void disconnect(Connection toDisconnect) {
        connections.remove(toDisconnect);
    }
    public int getConnection(InetAddress address) {
        for (int index = 0; index < connections.size(); index++) {
            if (connections.get(index).address == address) {
                return index;
            }
        }
        return -1;
    }
}
