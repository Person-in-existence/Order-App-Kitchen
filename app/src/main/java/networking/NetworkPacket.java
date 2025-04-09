package networking;

import java.net.InetAddress;

import networking.packets.Packet;

public class NetworkPacket {
    public InetAddress sender;
    public InetAddress receiver;
    public Packet packet;
    public NetworkPacket(InetAddress sender, InetAddress receiver, Packet packet) {
        this.sender = sender;
        this.receiver = receiver;
        this.packet = packet;
    }
}
