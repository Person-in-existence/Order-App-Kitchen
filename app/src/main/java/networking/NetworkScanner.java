package networking;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

class NetworkScanner {
    public static final int scanWaitTime = 1000; // Time in ms
    public static ArrayList<Device> scan(@Nullable ProgressBarUpdate progressBar)  {
        Device[] devices = new Device[256];

        String thisPart = Network.getIPAddress().split("\\.")[3];
        int finalPart = Integer.parseInt(thisPart);
        for (int index = 0; index < 256; index++) {
            int finalIndex = index;
            new Thread() {
                public void run() {

                    Log.d("NetworkScanner",String.valueOf(finalIndex));
                    // Dont detect this device
                    if(finalIndex ==finalPart)  {
                        return;
                    }

                    String ip = Network.subnet + finalIndex;
                    try {
                        Socket socket = new Socket();
                        socket.setSoTimeout(scanWaitTime);
                        socket.connect(new InetSocketAddress(ip, Network.PORT), 100);

                        // Send a type 8 packet
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                        // Version number
                        short versionNumber = Network.NETWORK_VERSION_NUMBER;
                        out.writeShort(versionNumber);

                        // Packet type: 8
                        short packetType = 8;
                        out.writeShort(packetType);

                        // Idempotency token - 0 will do
                        int idempotency = 0;
                        out.writeInt(idempotency);

                        // End packet

                        // Set a read timeout of 1s
                        DataInputStream in = new DataInputStream(socket.getInputStream());

                        // Version number - dont check for old version numbers because we want them to show up but be outdated
                        short inVersionNumber = in.readShort();

                        short inPacketType = in.readShort();
                        // If the packet type is not scan return then quit
                        if (inPacketType != 9) {
                            out.close();
                            socket.close();
                            return;
                        }
                        // Idempotency (ignored)
                        in.readInt();
                        // Device type
                        short deviceType = in.readShort();
                        String deviceName = Network.readString(in);

                        // Add the device
                        devices[finalIndex] = new Device(deviceName, socket.getInetAddress(), deviceType, inVersionNumber);

                        // Close in
                        in.close();
                        // Close out
                        out.close();

                        socket.close();
                    } catch(IOException e){
                        Log.d("NetworkScanner", Network.subnet + finalIndex + " timeout");
                        Log.d("NetworkScanner", e.getMessage());
                    }
                }

            }.start();
        }
        // Check whether the progressbar needs to be updated
        if (progressBar != null) {
            long currentMillis = System.currentTimeMillis();
            long target = currentMillis + scanWaitTime;
            while (currentMillis <= target) {
                long difference = target-currentMillis;
                int progress = (int) (100-difference/scanWaitTime*100);
                System.out.println(progress);
                progressBar.update(progress);
                currentMillis = System.currentTimeMillis();
                try {
                    Thread.sleep(scanWaitTime/100);
                } catch (InterruptedException ignored) {}

            }
        } else {
            // If not, just sleep for 1000
            try {
                Thread.sleep(scanWaitTime);
            } catch (InterruptedException ignored) {}
        }

        System.out.println(Arrays.toString(devices));
        // Put into a smaller array
        ArrayList<Device> arrayListDevices = new ArrayList<>();
        for (Device device : devices) {
            if (device != null) {
                arrayListDevices.add(device);
            }
        }
        System.out.println(arrayListDevices.size());
        return arrayListDevices;
    }
    public interface ProgressBarUpdate {
        void update(int progress);
    }

}
