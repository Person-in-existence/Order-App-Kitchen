package networking;

import android.util.Log;

import java.util.ArrayList;

import networking.packets.Packet;

public class OrderData {
    public ArrayList<Order> orders;
    public OrderData(ArrayList<Order> orders) {
        this.orders = orders;
    }
    public OrderData(OrderData data) {
        this.orders = new ArrayList<>();
        // Make a copy of every order, so it can't be affected by the original
        for (Order order: data.orders) {
            orders.add(new Order(order));
        }
    }

    protected Object[] makePacketType12() {
        int numOrders = orders.size();
        ArrayList<Object> bodyArrayList = new ArrayList<>();
        bodyArrayList.add(numOrders);

        for (Order order: this.orders) {
            long orderID = order.orderID;
            bodyArrayList.add(orderID);
            String customerName = order.customerName;
            bodyArrayList.add(customerName);
            short numItems = (short) order.items.size();
            bodyArrayList.add(numItems);
            for (Order.OrderItem item: order.items) {
                // ItemID: short
                bodyArrayList.add(item.itemID);
                // Item Quantity: int
                bodyArrayList.add(item.quantity);
            }
        }

        return bodyArrayList.toArray();
    }
    protected static OrderData makeFromPacketType12(Packet packet) {
        if (packet.type != 12) {
            Log.e("networking.OrderData", "Packet type invalid in makeFromPacketType12: expected 12 but got " + packet.type + ".");
        }
        int numOrders = (int) packet.body[0];
        ArrayList<Order> orders = new ArrayList<>();
        int bodyIndex = 1;
        for (int index = 0; index < numOrders; index++) {
            // OrderID: long
            long orderID = (long) packet.body[bodyIndex];
            bodyIndex++;

            // customerName: string
            String customerName = (String) packet.body[bodyIndex];
            bodyIndex++;

            // numItems: short
            short numItems = (short) packet.body[bodyIndex];
            bodyIndex++;

            ArrayList<Order.OrderItem> items = new ArrayList<>();
            // numitems times:
            for (int itemIndex = 0; itemIndex < numItems; itemIndex++) {
                // Item ID: short
                short itemID = (short) packet.body[bodyIndex];
                bodyIndex++;
                // Item Quantity: int
                int itemQuantity = (int) packet.body[bodyIndex];
                bodyIndex++;
                items.add(new Order.OrderItem(itemID, itemQuantity));
            }
            // Make the order and add it to orders
            orders.add(new Order(items, customerName, orderID));
        }
        return new OrderData(orders);
    }

}
