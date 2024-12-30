package com.example.orderappkitchen;

import java.util.ArrayList;

@Deprecated(since = "To be rewritten (uses an ArrayList as a fixed size array?) + needs better order tracking.")
public class Order {
    public ArrayList<Integer> orders;
    public ArrayList<String> items;
    public String name;
    public Order(ArrayList<Integer> orders, ArrayList<String> items, String name) {
        this.orders = orders;
        this.items = items;
        this.name = name;
    }
    public boolean setItems(ArrayList<String> itemsIn) {
        if (itemsIn.size() == 8) {
            items = itemsIn;
            return true;
        } else {
            return false;
        }
    }
    public String getText() {
        String toSend = name + ": ";
        for (int i = 0; i < 8; i++) {
            if (orders.get(i) > 0) {
                toSend = toSend + (orders.get(i) + " " + items.get(i));
                if (orders.get(i) > 1) {
                    toSend = toSend + "s";
                }
                toSend = toSend + ", ";
            }
        }
        return toSend;
    }
    public ArrayList<Integer> getAmounts() {
        return orders;
    }
}
