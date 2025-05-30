package networking;

import androidx.annotation.NonNull;

import java.net.InetAddress;

public class Device {
    public final String name;
    public final InetAddress ip;
    public final short deviceType;
    public final short version;
    public Device(String name, InetAddress ip, short deviceType, short version) {
        this.name = name;
        this.ip = ip;
        this.deviceType = deviceType;
        this.version = version;
    }
    @NonNull
    public String toString() {
        return "Device with Name: " + name + " IP: " + ip + " Device Type: " + deviceType + " Version: " + version;
    }

    public static String typeToString(short deviceType) {
        switch (deviceType) {
            case 0:
                return "Waiter";
            case 1:
                return "Server";
            case 2:
                return "Kitchen";
            default:
                return "Unrecognised device type";
        }
    }
    public String typeName() {
        return typeToString(deviceType);
    }
    public String getJoinCode() {
        return String.valueOf(ip).split("\\.")[3];
    }
}
