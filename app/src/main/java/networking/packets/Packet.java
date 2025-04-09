package networking.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Packet {
    public abstract Header getHeader();
    public void send(DataOutputStream out) throws IOException {
        // Send the header
        getHeader().send(out);
        // Pass body to child class
        sendBody(out);
    }
    protected abstract void sendBody(DataOutputStream out) throws IOException;
}