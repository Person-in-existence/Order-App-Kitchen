package networking;

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


public class Network {
    public static final String subnet = "192.168.1.";
    public static final short NETWORK_VERSION_NUMBER = 2;
    public static final int PORT = 65433;
    public static final boolean useIPv4 = false;

    protected static MainActivity activity;
    private volatile static Server server = new Server();

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

    protected static void writeString(String string, DataOutputStream out) throws IOException {
        int stringLength = string.length();
        out.writeInt(stringLength);
        out.writeChars(string);
    }

    protected static String readString(DataInputStream in) throws IOException {
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
