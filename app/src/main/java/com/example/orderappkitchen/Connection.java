package com.example.orderappkitchen;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Connection {
    public Server parent;
    public static final short TIMEOUT = 1000; // one second (ms)
    public static final short NETWORK_VERSION_NUMBER = 1;
    public Socket socket;
    public int clientIdempotencyToken = -1;
    public int idempotencyToken;
    public InetAddress address;

    public Connection(Socket socket, Server parent) {
        this.socket = socket;
        this.parent = parent;
        this.address = socket.getInetAddress();
    }
    public void monitor() {
        System.out.println("Monitor Started");
        try {
            InputStream inputStream = socket.getInputStream();
            DataInputStream in = new DataInputStream(inputStream);
            OutputStream outputStream = socket.getOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            while (true) {
                try {
                    // Check (and wait) for incoming packets
                    if (waitAvailable(in)) { // waitAvailable returns true if something arrives before the timeout
                        // Read packet header
                        // Version Number
                        /*short versionNumber = */readShort(in); // Version number is ignored - the client will check the version number on the return packet and end connection
                        // Packet type
                        short packetType = readShort(in);
                        // Idempotency Token
                        int incomingIdempotencyToken = readInt(in);
                        // Packet Body
                        switch (packetType) {
                            case 0: // New connection/client requesting information. (client->server)
                                handleType0(out, in, incomingIdempotencyToken);
                                break;
                            case 1: // Order being sent (client->server)
                                handleType1(out, in, incomingIdempotencyToken);
                                break;
                            case 4: // Client confirming reception of packet (client->server)
                                Log.e("OrderAppKitchen Networking", "Unexpected type 4 incoming packet");
                                break;
                            case 5: // Client disconnecting from server (client->server)
                                parent.disconnect(this);
                                socket.close();
                                return;
                        }

                    }

                } catch (Exception e) {
                    Log.e("OrderAppKitchen Networking", "Handling packet failed. " + Arrays.toString(e.getStackTrace()) + " " + e.getClass());
                }
            }
        } catch (IOException e) {
            Log.e("OrderAppKitchen", "Connection failed " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void handleType0(DataOutputStream out, DataInputStream in, int incomingIdempotencyToken) throws IOException {
        Log.d("OrderAppKitchen", "Type 0 packet");
        // When a type 0 has been received, a type 2 needs to be returned
        // No packet body to read
        clearIn(in);
        // Respond with a type 2
        // Get the things to send
        ArrayList<Integer> quantities = parent.parent.available;
        ArrayList<String> names = parent.parent.items;

        // Send packet header
        // Version Number
        Log.d("OrderAppKitchen", "Type 0 response start");
        writeShort(out, NETWORK_VERSION_NUMBER);
        // Packet type: 2
        writeShort(out, (short) 2);
        // Idempotency token (same as incoming)
        writeInt(out, incomingIdempotencyToken);

        // Packet Body
        // Number of items
        writeShort(out, (short) quantities.size());
        // For every item
        for (int index = 0; index < quantities.size(); index++) {
            // Item Name
            writeString(out, names.get(index));
            // Item Quantity
            writeInt(out, quantities.get(index));
        }
        Log.d("OrderAppKitchen", "Type 0 response end");

    }
    private void handleType1(DataOutputStream out, DataInputStream in, int incomingIdempotencyToken) throws IOException {
        // Read packet body
        // Customer Name
        String customerName = readString(in);
        // Number of items
        short numItems = readShort(in);
        // Items
        ArrayList<Short> itemPositions = new ArrayList<>();
        ArrayList<Integer> itemQuantities = new ArrayList<>();
        for (int i = 0; i < numItems; i++) {
            // Item ID
            itemPositions.add(readShort(in));
            // Item Quantity
            itemQuantities.add(readInt(in));
        }
        // Protect against duplicate orders
        if (incomingIdempotencyToken == clientIdempotencyToken) {
            Log.d("OrderAppKitchen", "Disregarding incoming order with duplicate Idempotency token");
        } else {
            // Process items into a list
            ArrayList<Integer> orderItems = new ArrayList<>();
            for (int i = 0; i < parent.parent.items.size(); i++) {
                orderItems.add(0);
            }
            for (int index = 0; index < itemPositions.size(); index++) {
                if (0 <= itemPositions.get(index) & itemPositions.get(index) < orderItems.size()) {
                    orderItems.set(itemPositions.get(index), itemQuantities.get(index));
                } else {
                    // Data was corrupted, send a false ack to client
                    sendType3(out, false, incomingIdempotencyToken);
                    // Clear input so it can be cleanly read
                    clearIn(in);
                    return;
                }
            }
            parent.parent.addOrder(Server.newOrder(customerName, orderItems, parent.parent));
        }
        // Send acknowledgement
        sendType3(out, true, incomingIdempotencyToken);
        clientIdempotencyToken = incomingIdempotencyToken;
    }

    private void sendType3(DataOutputStream out, boolean status, int idempotencyToken) throws IOException {
        // Header
        // Version number
        writeShort(out, NETWORK_VERSION_NUMBER);
        // Packet Type
        writeShort(out, (short) 3);
        // Idempotency Token
        writeInt(out, idempotencyToken);
        // Packet Body
        out.writeBoolean(status);
    }

    public boolean waitAvailable(DataInputStream in) {
        try {
            if (in.available() != 0) {
                return true;
            }
            short counter = 0;
            while ((in.available() == 0) & (counter < TIMEOUT)) {
                safeWait(1);
                counter++;

            }
            return counter != TIMEOUT;
        } catch (IOException e) {
            Log.e("OrderAppKitchen Networking", "Wait I/O Exception");
            return false;
        }
    }
    public short readShort(DataInputStream in) throws IOException {
        waitAvailable(in);
        short tries = 0;
        while (tries < 10) {
            try {
                return in.readShort();
            } catch (IOException e) {
                Log.e("OrderAppKitchen Networking", "reading short failed " + Arrays.toString(e.getStackTrace()));
                tries++;
            }
        }
        throw new IOException();
    }
    public int readInt(DataInputStream in) throws IOException {
        waitAvailable(in);
        short tries = 0;
        while (tries < 10) {
            try {
                return in.readInt();
            } catch (IOException e) {
                Log.e("OrderAppKitchen Networking", "reading int failed " + Arrays.toString(e.getStackTrace()));
                tries++;
            }
        }
        throw new IOException();
    }
    public char readChar(DataInputStream in) throws IOException {
        waitAvailable(in);
        short tries = 0;
        while (tries < 10) {
            try {
                return in.readChar();
            } catch (IOException e) {
                Log.e("OrderAppKitchen Networking", "reading char failed " + Arrays.toString(e.getStackTrace()));
                tries++;
            }
        }
        throw new IOException();
    }
    public String readString(DataInputStream in) throws IOException {
        try {
            int stringLength = readInt(in);
            StringBuilder stringBuilder = new StringBuilder(stringLength);
            for (int i = 0; i < stringLength; i++) {
                stringBuilder.append(readChar(in));
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            Log.e("OrderAppKitchen Networking", "Failed to read string " + Arrays.toString(e.getStackTrace()));
            throw new IOException();
        }
    }
    public boolean readBoolean(DataInputStream in) throws IOException {
        waitAvailable(in);
        short tries = 0;
        while (tries < 10) {
            try {
                return in.readBoolean();
            } catch (IOException e) {
                Log.e("OrderAppKitchen Networking", "reading boolean failed " + Arrays.toString(e.getStackTrace()));
                tries++;
            }
        }
        throw new IOException();
    }
    public void writeShort(DataOutputStream out, short outShort) throws IOException {
        out.writeShort(outShort);
    }
    public void writeInt(DataOutputStream out, int outInt) throws IOException {
        out.writeInt(outInt);
    }
    public void writeChars(DataOutputStream out, String chars) throws IOException {
        out.writeChars(chars);
    }
    public void writeString(DataOutputStream out, String string) throws IOException {
        int length = string.length();
        out.writeInt(length);
        writeChars(out, string);
    }
    public void safeWait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Log.e("OrderAppKitchen Networking", "Wait fail " + Arrays.toString(e.getStackTrace()));
        }
    }
    public void clearIn(DataInputStream in) {
        try {
            in.skip(in.available());
        } catch (IOException e) {
            Log.e("OrderAppKitchen Networking", "Clearing failed: "+ Arrays.toString(e.getStackTrace()));
        }
    }
    public void reconnect(Socket socket) {
        this.socket = socket;
    }

}
