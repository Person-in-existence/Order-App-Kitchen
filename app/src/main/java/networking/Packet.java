package networking;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Packet {
    public final short versionNumber;
    public final short type;
    public final int idempotencyToken;
    public final Object[] body;
    public Packet(short versionNumber, short type, int idempotencyToken, Object[] body) {
        this.versionNumber = versionNumber;
        this.type = type;
        this.idempotencyToken = idempotencyToken;
        this.body = body;
        if (!validBody()) {
            System.err.println("Invalid body:");
            System.err.println(this);
            throw new IllegalArgumentException("Body was invalid");
        }
    }
    private boolean type12ValidBody() throws ClassCastException, IndexOutOfBoundsException {
        // Number of orders: int
        if (!(body[0] instanceof Integer)) {
            return false;
        }
        int numOrders = (int) body[0];
        int countedOrders = 0;
        int currentIndex = 1;
        while (countedOrders < numOrders) {
            // long order ID, short numItems
            if (!(body[currentIndex] instanceof Long & body[currentIndex + 1] instanceof Short)) {
                return false;
            }
            currentIndex++;
            short numItems = (short) body[currentIndex];
            currentIndex++;
            // NumItems times:
            int tempIndex;
            for (tempIndex = currentIndex; tempIndex < currentIndex+(numItems*2); tempIndex+=2) {
                // Item ID: short, Item Quantity: int
                if (!(body[tempIndex] instanceof Short & body[tempIndex+1] instanceof Integer)) {
                    return false;
                }
            }
            // Add two here so that we point to the next item
            currentIndex = tempIndex + 2;

            countedOrders++;

        }
        return true;
    }
    private boolean validBody() {
        try {
            boolean correct;
            switch (type) {
                case 0:
                    return body.length == 0;
                case 1:
                    correct = (body[0] instanceof String) & (body[1] instanceof Short) & ((body.length-2)/2==(short)body[1]);
                    if (!correct) {
                        return false;
                    }
                    // Check that the remaining items are alternating short and integer
                    for (int index = 2; index < body.length; index++) {
                        if (index % 2 == 0) {
                            if (!(body[index] instanceof Short)) {
                                return false;
                            }
                        } else {
                            if (!(body[index] instanceof Integer)) {
                                return false;
                            }
                        }
                    }
                    return true;
                case 2:
                    correct = body[0] instanceof Short & (((short) body[0]) == (body.length-1)/2);
                    if (!correct) {
                        return false;
                    }
                    for (int index = 1; index < body.length; index++) {
                        if (index % 2 == 1) {
                            if (!(body[index] instanceof String)) {
                                return false;
                            }
                        } else {
                            if (!(body[index] instanceof Integer)) {
                                return false;
                            }
                        }
                    }
                    return true;
                case 3:
                case 4:
                    return (body[0] instanceof Boolean) & (body.length == 1);
                case 5:
                    return body.length == 0;
                case 6:
                    return (body[0] instanceof Long) & body.length == 1;
                case 7:
                    correct = (body[0] instanceof Long) & (body[1] instanceof Boolean) & (body[2] instanceof Integer);
                    if (!correct) {
                        return false;
                    }
                    // If isAdd
                    if ((boolean) body[1]) {
                        short numItems = (short) body[3];
                        if (body.length != (numItems*2) + 4) {
                            return false;
                        }
                        for (int index = 4; index < body.length; index += 2) {
                            if (!(body[index] instanceof Short & body[index+1] instanceof Integer)) {
                                return false;
                            }
                        }
                        return true;
                    } else {
                        return body.length == 3;
                    }

                case 8:
                    return body.length == 0;
                case 9:
                    return (body[0] instanceof Short) & (body[1] instanceof String) & (body.length == 2);
                case 10:
                    return (body[0] instanceof Short) & (body[1] instanceof Integer) & (body[2] instanceof Integer) & (body.length == 3);
                case 11:
                    return body.length == 0;
                case 12:
                    return type12ValidBody();
                default:
                    return false;
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e("network.Packet", e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }
    private void sendType1(DataOutputStream out) throws IOException {
        // Customer name: String
        Network.writeString((String) body[0], out);
        // Number of non-0 items: short
        out.writeShort((short) body[1]);
        for (int index = 2; index < body.length; index += 2) {
            // Item ID: short
            out.writeShort((short) body[index]);
            // Item quantity: int
            out.writeInt((int) body[index + 1]);
        }
    }

    private void sendType2(DataOutputStream out) throws IOException {
        // Number of Items: short
        out.writeShort((short) body[0]);
        for (int index = 1; index < body.length; index += 2) {
            // Item name: String
            Network.writeString((String) body[index], out);
            // Item Quantity: int
            out.writeInt((int) body[index + 1]);
        }
    }
    private void sendType3or4(DataOutputStream out) throws IOException {
        // Successful packet reception: boolean
        out.writeBoolean((boolean) body[0]);
    }
    private void sendType6(DataOutputStream out) throws IOException {
        // Order ID: long
        out.writeLong((long) body[0]);
    }
    private void sendType7(DataOutputStream out) throws IOException {
        // Order ID: long
        out.writeLong((long) body[0]);
        // isAdd: boolean
        boolean isAdd = (boolean) body[1];
        out.writeBoolean(isAdd);
        // Checksum: int
        out.writeInt((int) body[2]);
        // if isAdd:
        if (isAdd) {
            // Number of items: short
            out.writeShort((short) body[3]);
            for (int index = 0; index < body.length; index += 2) {
                // Item ID: short
                out.writeShort((short) body[index]);
                // Item Quantity: int
                out.writeInt((int) body[index+1]);
            }
        }
    }
    private void sendType9(DataOutputStream out) throws IOException {
        // Device type: short
        out.writeShort((short) body[0]);
        // Name: String
        Network.writeString((String) body[1], out);
    }
    private void sendType10(DataOutputStream out) throws IOException {
        // Item ID: short
        out.writeShort((short) body[0]);
        // Amount: int
        out.writeInt((int) body[1]);
        // Checksum: int
        out.writeInt((int) body[2]);
    }
    private void sendType12(DataOutputStream out) throws IOException {
        // Number of orders: int
        int numOrders = (int) body[0];
        out.writeInt(numOrders);

        int bodyIndex = 1;
        for (int orderIndex = 0; orderIndex < numOrders; orderIndex++) {
            // Order ID: long
            out.writeLong((long) body[bodyIndex]);
            bodyIndex++;

            // Number of items: short
            short numItems = (short) body[bodyIndex];
            out.writeShort(numItems);
            bodyIndex++;

            // numItems times:
            for (int itemIndex = 0; itemIndex < numItems; itemIndex++) {
                // Item ID: short
                out.writeShort((short) body[bodyIndex]);
                bodyIndex++;

                // Item Quantity: int
                out.writeInt((int) body[bodyIndex]);
                bodyIndex++;
            }
        }

    }

    public void send(DataOutputStream out) throws IOException {
        out.writeShort(versionNumber);
        out.writeShort(type);
        out.writeInt(idempotencyToken);
        switch (type) {
            case 1:
                sendType1(out);
                return;
            case 2:
                sendType2(out);
                return;
            case 3:
            case 4:
                sendType3or4(out);
                return;
            case 6:
                sendType6(out);
                return;
            case 7:
                sendType7(out);
                return;
            case 9:
                sendType9(out);
                return;
            case 10:
                sendType10(out);
                return;
            case 12:
                sendType12(out);
                return;
        }
    }

    private static Object[] receiveType1(DataInputStream in) throws IOException {
        String customerName = Network.readString(in);
        short numItems = in.readShort();
        // Make the array now we know how large it is (add two because customerName and numItems need to be in it)
        Object[] body = new Object[(numItems*2) + 2];
        body[0] = customerName;
        body[1] = numItems;
        // Go up by 2 indexes at a time
        for (int index = 2; index < body.length; index += 2) {
            // Item ID: short
            body[index] = in.readShort();
            // Item quantity: integer
            body[index+1] = in.readInt();
        }
        return body;
    }

    private static Object[] receiveType2(DataInputStream in) throws IOException {
        short numItems = in.readShort();
        // Make the array now we know the size (+1 for numItems
        Object[] body = new Object[(numItems * 2) + 1];
        for (int index = 1; index < body.length; index+= 2) {
            // Item Name: String
            body[index] = Network.readString(in);
            // Item Quantity: integer
            body[index + 1] = in.readInt();
        }
        return body;
    }

    private static Object[] receiveType3or4(DataInputStream in) throws IOException {
        // Successful packet reception: boolean
        return new Object[] {in.readBoolean()};
    }

    private static Object[] receiveType6(DataInputStream in) throws IOException {
        // Order ID: long
        return new Object[] {in.readLong()};
    }

    private static Object[] receiveType7(DataInputStream in) throws IOException {
        Object[] body;
        // Order ID: long
        long orderID = in.readLong();
        // IsAdd: boolean
        boolean isAdd = in.readBoolean();
        // Checksum: int
        int checksum = in.readInt();
        if (isAdd) {
            // Number of items: short
            short numItems = in.readShort();
            body = new Object[4+(numItems*2)];
            body[0] = orderID;
            body[1] = isAdd;
            body[2] = checksum;
            body[3] = numItems;
            for (int index = 4; index < body.length; index += 2) {
                // Item ID: short
                body[index] = in.readShort();
                // Item Quantity: int
                body[index + 1] = in.readInt();
            }
        } else {
            body = new Object[] {orderID, isAdd, checksum};
        }
        return body;
    }

    private static Object[] receiveType9(DataInputStream in) throws IOException {
        short deviceType = in.readShort();
        String name = Network.readString(in);
        return new Object[] {deviceType, name};
    }

    private static Object[] receiveType10(DataInputStream in) throws IOException {
        short itemID = in.readShort();
        int amount = in.readInt();
        int checksum = in.readInt();
        return new Object[] {itemID, amount, checksum};
    }

    private static Object[] receiveType12(DataInputStream in) throws IOException {
        ArrayList<Object> bodyList = new ArrayList<>();
        // Number of orders: int
        int numOrders = in.readInt();
        bodyList.add(numOrders);

        // numOrders times:
        for (int orderIndex = 0; orderIndex < numOrders; orderIndex++) {
            // Order ID: long
            bodyList.add(in.readLong());

            // Number of items: short
            short numItems = in.readShort();
            bodyList.add(numItems);

            // numItems times:
            for (int itemIndex = 0; itemIndex < numItems; itemIndex++) {
                // Item ID: short
                bodyList.add(in.readShort());
                // Item Quantity: int
                bodyList.add(in.readInt());
            }
        }

        return bodyList.toArray();
    }

    public static Object[] receiveBody(short packetType, DataInputStream in) throws IOException {
        switch (packetType) {
            // Empty bodies
            case 0:
            case 5:
            case 8:
            case 11:
                return new Object[0];
            case 1:
                return receiveType1(in);
            case 2:
                return receiveType2(in);
            case 3:
            case 4:
                return receiveType3or4(in);
            case 6:
                return receiveType6(in);
            case 7:
                return receiveType7(in);
            case 9:
                return receiveType9(in);
            case 10:
                return receiveType10(in);
            case 12:
                return receiveType12(in);
            default:
                throw new IllegalArgumentException("Invalid packet type: " + packetType);
        }
    }

    @NonNull
    public String toString() {
        String string = "versionNumber: " + String.valueOf(versionNumber) + "\ntype: " + String.valueOf(type) + "\nidempotencyToken" + String.valueOf(idempotencyToken);
        for (Object object : body) {

            string += "\n" + object.getClass() + " " + object;
        }

        return string;
    }
}
