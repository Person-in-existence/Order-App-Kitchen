package networking;



import networking.packets.Packet;

public class ResponseWait {
    public long time; // Use a long so we don't have overflow.
    public final Packet packet;
    public ResponseWait(Packet packet, int waitTimeMs) {
        this.packet = packet;
        this.time = System.currentTimeMillis() + waitTimeMs;
    }
    public boolean isExpired() {
        return System.currentTimeMillis() > time;
    }
    public int timeToExpired() {
        // Int is safe here - the initial argument is an int wait time, and time doesn't go backwards (I hope)
        return (int) (time - System.currentTimeMillis());
    }
    public Packet getPacket() {
        return packet;
    }
}
