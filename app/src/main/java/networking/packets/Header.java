package networking.packets;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Header {
    public final short versionNumber;
    public final short type;
    public final int idempotencyToken;
    public Header(DataInputStream in) throws IOException {
        this.versionNumber = in.readShort();
        this.type = in.readShort();
        this.idempotencyToken = in.readInt();
    }
    public Header(short versionNumber, short type, int idempotencyToken) {
        this.versionNumber = versionNumber;
        this.type = type;
        this.idempotencyToken = idempotencyToken;
    }
    public void send(DataOutputStream out) throws IOException {
        out.writeShort(versionNumber);
        out.writeShort(type);
        out.writeInt(idempotencyToken);
    }
    public void checkType(short expectedType) {
        if (this.type != expectedType) {
            Log.w("orderAppKitchen.networking.packets.Header", "Wrong header type: expected " + expectedType + " but got " + this.type);
        }
    }
}
