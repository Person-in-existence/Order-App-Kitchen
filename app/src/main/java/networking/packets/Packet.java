package networking.packets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.DataOutputStream;
import java.io.IOException;

import networking.SuccessNotifier;

public abstract class Packet {
    private SuccessNotifier listener = null;
    public abstract Header getHeader();
    public void send(DataOutputStream out) throws IOException {
        // Send the header
        getHeader().send(out);
        // Pass body to child class
        sendBody(out);
    }
    protected abstract void sendBody(DataOutputStream out) throws IOException;
    public void sent(boolean success) {
        if (listener != null) {
            listener.success(success);
        }
    }
    public void setSendListener(@Nullable SuccessNotifier listener) {
        this.listener = listener;
    }


    @NonNull
    public String toString() {
        return getHeader().toString();
    }
}