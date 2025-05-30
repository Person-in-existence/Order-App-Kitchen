package networking;

import android.util.Log;

import java.util.ArrayList;

import networking.packets.Packet;

public class OrderData {
    public ArrayList<Order> orders;
    public OrderData(ArrayList<Order> orders) {
        // Copy everything so it doesn't accidentally break if this is changed.
        this.orders = new ArrayList<>();
        for (Order order: orders) {
            this.orders.add(new Order(order));
        }
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
}
