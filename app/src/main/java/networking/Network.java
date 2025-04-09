package networking;

import android.util.Log;

import androidx.annotation.Nullable;

import com.example.orderappkitchen.MainActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


public class Network {
    public static final String subnet = "192.168.1.";
    public static final short NETWORK_VERSION_NUMBER = 2;
    public static final int PORT = 65433;
    public static final boolean useIPv4 = false;

    protected static MainActivity activity;
    private volatile static Server server = new Server();
    private static final AtomicLong orderID = new AtomicLong(0);

    public static ArrayList<Device> scanDevices(@Nullable NetworkScanner.ProgressBarUpdate progressBar) {
        return NetworkScanner.scan(progressBar);
    }

    public static void startSession(MainActivity activity) {
        Network.activity = activity;
        server.setActivity(activity);
        server.setAccepting(true);
        server.start();
    }

    public static void endSession() {
        server.end();
    }

    public static void joinServer(String ip) {

    }
    public static long getNewOrderID() {
        return orderID.incrementAndGet();
    }

    public static void removeOrderByID(long orderID) {
        activity.removeOrderByID(orderID);
    }
    public static void setSessionData(SessionData data) {
        activity.setSessionData(data);
    }

    public static void setOrderData(OrderData data) {
        activity.setOrderData(data);
    }
    public static void addOrder(Order order) {
        activity.addOrder(order);
    }
    public static boolean addOrderChecksum(Order order, int receivedChecksum) {
        // Add the order
        addOrder(order);

        // Calculate checksum
        int actualChecksum = activity.makeChecksum();

        return receivedChecksum == actualChecksum;
    }
    public static boolean removeOrderChecksum(long orderID, int receivedChecksum) {
        // Remove the order
        removeOrderByID(orderID);

        // Check checksum
        int actualChecksum = activity.makeChecksum();

        return receivedChecksum == actualChecksum;
    }
    public static boolean addRemoveItemsByAmount(Order amounts, int checksum) {
        Log.w("networking.Network", "Received call to addRemoveItemsByAmount, not expected on a kitchen device. assert/return false");
        assert false;
        return false;
    }
    public static OrderData getOrderData() {
        return new OrderData(activity.orders);
    }


    protected static SessionData getSessionData() {
        return activity.getSessionData();
    }

    protected static short getDeviceType() {
        return MainActivity.DEVICE_TYPE;
    }
    protected static String getDeviceName() {
        return "Names haven't been implemented yet.";
    }



    public static void writeString(String string, DataOutputStream out) throws IOException {
        int stringLength = string.length();
        out.writeInt(stringLength);
        out.writeChars(string);
    }

    public static String readString(DataInputStream in) throws IOException {
        int stringLength = in.readInt();
        StringBuilder stringBuilder = new StringBuilder(stringLength);
        for (int index = 0; index < stringLength; index++) {
            stringBuilder.append(in.readChar());
        }
        return stringBuilder.toString();
    }

    public static String getIPAddress() {
        try {
            List<NetworkInterface> interfaces =
                    Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface interface_ : interfaces) {

                for (InetAddress inetAddress :
                        Collections.list(interface_.getInetAddresses())) {

                    /* a loopback address would be something like 127.0.0.1 (the device
                       itself). we want to return the first non-loopback address. */
                    if (!inetAddress.isLoopbackAddress()) {
                        String ipAddr = inetAddress.getHostAddress();
                        if (ipAddr != null) {
                            boolean isIPv4 = ipAddr.indexOf('.') < 3;

                            if (isIPv4 && !useIPv4) {
                                continue;
                            }
                            if (useIPv4 && !isIPv4) {
                                int delim = ipAddr.indexOf('%'); // drop ip6 zone suffix
                                ipAddr = delim < 0 ? ipAddr.toUpperCase() : ipAddr.substring(0, delim).toUpperCase();
                            }
                            return ipAddr;
                        }
                    }
                }

            }
        } catch (Exception ignored) { } // if we can't connect, just return empty string
        return "";
    }

}
