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
import java.util.Locale;
import java.util.Objects;

public class Server extends Thread {
    ServerSocket socket;
    MainActivity parent;
    public void run() {
        try {
            socket = new ServerSocket(65433);
            socket.setSoTimeout(30000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                System.out.println("LoopAgain");
                Socket current = socket.accept();
                System.out.println("Connection accepted");
                if (current != null) {
                    System.out.println(current);
                    OutputStream to_server = current.getOutputStream();
                    DataOutputStream out = new DataOutputStream(to_server);
                    // Creates the dataInputStream from the server
                    InputStream from_server = current.getInputStream();
                    DataInputStream in = new DataInputStream(from_server);
                    char data;
                    while (true) {
                        try {
                            data = in.readChar();
                            break;
                        } catch (EOFException e) {
                            System.out.println("Oof");
                            wait(250);
                        }
                    }
                    System.out.println("New incoming connection. Type is: " + data);
                    if (data == 'N') {
                        Log.d("Socket", "Connection EstablishedP1");
                        System.out.println("Incoming connection of type: N");
                        ArrayList<Integer> availables = parent.available;
                        ArrayList<String> items = parent.items;
                        for (int available : availables) {
                            out.writeInt(available);
                        }
                        for (String item: items) {
                            out.writeInt(item.length());
                            out.writeChars(item);
                        }
                        System.out.println("NCOMPLETE");
                    } else if (data == 'O') {
                        Log.d("Order", "New order connection");
                        System.out.println("Incoming connection of type: O");
                        String name = readString(in);
                        ArrayList<Integer> ordered = new ArrayList<>();
                        for (int i = 0; i < 8; i++) {
                            ordered.add(readInt(in));
                        }
                        Log.d("OrdersServer", String.valueOf(ordered));
                        parent.addOrder(newOrder(name, ordered, parent));

                    } else if (data == 'M') {
                        Log.d("OrderMerger", "New OrderMerger Connection");
                        String name = readString(in);
                        ArrayList<Integer> ordered = new ArrayList<>();
                        for (int i = 0; i < 8; i++) {
                            ordered.add(readInt(in));
                        }
                        mergeOrder(name, ordered);
                    }
                    // UNTESTED
                    for (int i = 0; i < 30; i++) {
                        writeString("S", out);
                        System.out.println("LOOP");
                        wait(10);
                        String result = readStringLimited(in);
                        System.out.println("res"+result);
                        if (Objects.equals(result, "Y")) {
                            break;
                        }
                    }

                    current.close();
                    System.out.println("SOCKETCLOSE");
                }

            }catch (SocketTimeoutException s) {
                System.out.println("SOCKETTIME");
            }
            catch (Exception e) {
                Log.d("Server", "Error");
                e.printStackTrace();
            }
        }
    }
    public static Integer readInt(DataInputStream in) {
        while (true) {
            try {
                return in.readInt();
            } catch (EOFException e) {
                Log.d("No message so", "looping");
            } catch (IOException e) {
                Log.d("Server", "Stream Closed");
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
    public static String readString(DataInputStream in) {
        int length;
        ArrayList<Character> message = new ArrayList<>();
        while (true) {
            try {
                length = in.readInt();
                break;
            } catch (EOFException e) {
                Log.d("No message so", "looping");
                wait(5);
            } catch (IOException e) {
                Log.d("Server", "Stream Closed");
                return null;
            }
        }
        for (int i = 0; i < length; i++) {
            while (true) {
                try {
                    message.add(in.readChar());
                    break;
                } catch (EOFException e) {
                    Log.d("No message so", "looping");
                    wait(5);
                } catch (IOException e) {
                    Log.d("Server", "Stream Closed");
                    return null;
                }
            }
        }
        return decode(length, message);
    }
    public static String readStringLimited(DataInputStream in) {
        int length = 0;
        ArrayList<Character> message = new ArrayList<>();
        int checks = 0;
        while (checks < 11) {
            try {
                if (in.available() > 0) {
                    length = in.readInt();
                } else {
                    System.out.println("NONEAVAILABLE");
                }
                break;
            } catch (EOFException e) {
                Log.d("No message so", "looping 166");
                wait(5);
                checks++;
            } catch (IOException e) {
                Log.d("Server", "Stream Closed");
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (checks == 10) {return null;}
        for (int i = 0; i < length; i++) {
            while (true) {
                try {
                    message.add(in.readChar());
                    break;
                } catch (EOFException e) {
                    Log.d("No message so", "looping");
                    wait(5);
                } catch (IOException e) {
                    Log.d("Server", "Stream Closed");
                    return null;
                }
            }
        }
        return decode(length, message);
    }
    public static String decode(Integer length, ArrayList<Character> chars) {
        String to_return = "";
        for (int i = 0; i < length; i++) {
            to_return += chars.get(i);
        }
        return to_return;
    }
    public static Order newOrder(String name, ArrayList<Integer> ordered, MainActivity parent) {
        Order order = new Order();
        order.setOrders(ordered);
        order.setName(name);
        order.setItems(parent.getItems());
        return order;
    }
    public void mergeOrder(String name, ArrayList<Integer> ordered) {
        Boolean success = false;
        for (int i = parent.orders.size() - 1; i == 0; i--) {
            if (parent.orders.get(i).name.toUpperCase(Locale.ROOT).equals(name.toUpperCase(Locale.ROOT))) {
                for (int x = 0; x < 8; x++) {
                    parent.orders.get(i).orders.set(x, parent.orders.get(i).orders.get(i) + ordered.get(i));
                }
                parent.updateOrders();
                success = true;
                break;
            }
        }
        if (!success) {
            parent.addOrder(newOrder(name, ordered, parent));
        }
    }
    public static void writeString(String message, DataOutputStream out) {
        int length = message.length();
        try {
            out.writeInt(length);
        } catch (IOException e) {
            Log.d("Sender", "Error, noOutputStream.");

        }
        try {
            out.writeChars(message);
        } catch (IOException e) {
            Log.d("Sender", "Error, noOutputStream.");
        }
    }
}
