package com.example.orderappkitchen;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;

import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.orderappkitchen.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

import networking.Network;
import networking.OrderData;
import networking.SessionData;
import networking.Order;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    public volatile ArrayList<Order> orders = new ArrayList<>();
    public volatile ArrayList<String> items = new ArrayList<>();
    public volatile ArrayList<Integer> available = new ArrayList<>();
    public FirstFragment fragment;
    public FragmentManager manager;
    public static final short DEVICE_TYPE = 2; // 2 for kitchen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        /*
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
         */

        FragmentManager manager = getSupportFragmentManager();



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
    public SessionData getSessionData() {
        assert items.size() == available.size();
        SessionData.SessionItem[] data = new SessionData.SessionItem[items.size()];
        for (int index = 0; index < items.size(); index++) {
            data[index] = new SessionData.SessionItem(items.get(index), available.get(index));

        }
        return new SessionData(data);
    }

    public void setSessionData(SessionData sessionData) {
        ArrayList<String> newItems = new ArrayList<>();
        ArrayList<Integer> newAvailable = new ArrayList<>();
        for (SessionData.SessionItem item : sessionData.items) {
            newItems.add(item.name);
            newAvailable.add(item.quantity);
        }
        this.items = newItems;
        this.available = newAvailable;
        // Update orders (so they reset if names have changed etc)
        runOnUiThread(() -> {
            if (fragment != null) {
                fragment.showOrders(orders);
            }
        });
    }

    public OrderData getOrderData() {
        return new OrderData(orders);
    }

    public void setOrderData(OrderData data) {
        this.orders = data.orders;

        runOnUiThread(()->{
            if (fragment != null) {
                fragment.showOrders(orders);
            }
        });
    }

    public void removeOrder(int orderIndex) {
        orders.remove(orderIndex);
        runOnUiThread(()->{
            if (fragment != null) {
                fragment.showOrders(orders);
            }
        });
    }
    public void removeOrderByID(long orderID) {
        for (int index = 0; index < orders.size(); index++) {
            if (orders.get(index).orderID == orderID) {
                orders.remove(index);
                // Break so we dont go over the length of the list once we have found it
                break;
            }
        }
        runOnUiThread(()->{
            if (fragment != null) {
                fragment.showOrders(orders);
            }
        });
    }
    public int makeChecksum() {
        int total = 0;
        for (int index = 0; index < available.size(); index++) {
            total += (int) (Math.pow(7, index) * available.get(index));
        }
        return total;
    }

    public void addOrder(Order order) {
        orders.add(order);
        for (Order.OrderItem item: order.items) {
            available.set(item.itemID, available.get(item.itemID)-item.quantity);
        }
        runOnUiThread(()->{
            if (fragment != null) {
                fragment.showOrders(orders);
            }
        });
    }

    public boolean isOrderWithID(long orderID) {
        for (Order order: orders) {
            if (order.orderID == orderID) {
                return true;
            }
        }
        return false;
    }

    protected static String getJoinCode() {
        try {
            final String[] ip = new String[1];
            Thread getIPThread = new Thread() {
                public void run() {
                    ip[0] = Network.getIPAddress();
                }
            };
            getIPThread.start();
            getIPThread.join(10000);
            Log.d("MainActivity", ip[0]);
            if (ip[0] != null) {
                String[] bits = ip[0].split("\\.");
                Log.d("JoinCode", bits[3]);
                return bits[3];
            } else {
                Log.d("JoinCode", "IP was null");
                return "";
            }
        } catch (Exception e) {
            Log.d("JoinCode", String.valueOf(e));
            return "";
        }
    }
    protected ArrayList<Integer> getTotal() {
        ArrayList<Integer> total = new ArrayList<>();
        for (int z = 0; z<8; z++) {
            total.add(0);
        }
        for (Order order : orders) {
            for (Order.OrderItem item: order.items) {
                total.set(item.itemID, total.get(item.itemID) + item.quantity);
            }
        }
        return total;
    }
    protected ArrayList<String> getItems() {
        return items;
    }
    protected ArrayList<Integer> getAvailable() {
        return available;
    }

    protected void setFragment(FirstFragment newFragment) {fragment = newFragment; updateOrders();}
    protected void updateOrders() {
        if (fragment != null) {
            if (!orders.isEmpty()) {
                fragment.showOrders(orders);
            }
        }
    }
    protected void startSession(ArrayList<Integer> newAvailable, ArrayList<String> newItems) {
        available = newAvailable;
        items = newItems;

        // Start the network session
        Network.startSession(this);
    }

    protected boolean hasItems() {return !items.isEmpty();}
    protected boolean hasAvailables() {return !available.isEmpty();}
    protected void showSnackbar(String message) {
        Snackbar.make(binding.toolbar, message, Snackbar.LENGTH_LONG)
                .setAction(message, null).show();
    }
}