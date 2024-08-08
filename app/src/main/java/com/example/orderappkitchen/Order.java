package com.example.orderappkitchen;

import java.util.ArrayList;

public class Order {
    public ArrayList<Integer> orders;
    public ArrayList<String> items;
    public String name;
    public boolean setItems(ArrayList<String> itemsIn) {
        if (itemsIn.size() == 8) {
            items = itemsIn;
            return true;
        } else {
            return false;
        }
    }
    public boolean setOrders(ArrayList<Integer> ordersIn) {
        if (ordersIn.size() == 8) {
            orders = ordersIn;
            return true;
        } else {
            return false;
        }
    }
    public boolean setName(String nameIn) {
        if (nameIn != null)  {
            if (nameIn != "") {
                name = nameIn;
                return true;
            }
        }
        return false;
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
