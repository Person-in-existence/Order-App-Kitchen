package com.example.orderappkitchen;

import android.app.Activity;
import android.util.Log;

import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class IpCode implements Runnable {
    public String ip2 = "";
    public void run() {
        try {
            //Inet4Address address = new Inet4Address();
            ip2 = getIPAddress();
            Log.d("JoinCode", ip2);
            Thread.sleep(50);
        } catch (Exception e) {
            Log.d("IpCode error", String.valueOf(e));
        }
    }
    public String ip() {
        Log.d("ReturnIP", ip2);
        return ip2;
    }
    public static String getIPAddress() {
        boolean useIPv4 = false;
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
                        boolean isIPv4 = ipAddr.indexOf('.') < 3;

                        if (isIPv4 && !useIPv4) {
                            continue;
                        }
                        if (useIPv4 && !isIPv4) {
                            int delim = ipAddr.indexOf('%'); // drop ip6 zone suffix
                            ipAddr = delim < 0 ? ipAddr.toUpperCase() :
                                    ipAddr.substring(0, delim).toUpperCase();
                        }
                        return ipAddr;
                    }
                }

            }
        } catch (Exception ignored) { } // if we can't connect, just return empty string
        return "";
    }

}
