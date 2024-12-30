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
import java.util.Arrays;

import networking.Network;
import networking.SessionData;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    public volatile ArrayList<Order> orders = new ArrayList<>();
    public volatile ArrayList<String> items = new ArrayList<>();
    public volatile ArrayList<Integer> available = new ArrayList<>();
    public FirstFragment fragment;
    public FragmentManager manager;

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

        Server networkHandler = new Server();
        Thread networkThread = new Thread(networkHandler);
        networkHandler.parent = this;
        networkThread.start();
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
    public SessionData getOrderData() {
        int itemSize = items.size();
        String[] itemArray = new String[itemSize];
        for (int index = 0; index < itemSize; index++) {
            itemArray[index] = items.get(index);
        }

        int availableSize = available.size();
        int[] availableArray = new int[availableSize];
        for (int index = 0; index < availableSize; index++) {
            availableArray[index] = available.get(index);
        }
        return new SessionData(itemArray, availableArray);
    }

    public void setOrderData(SessionData sessionData) {
        items = new ArrayList<>(Arrays.asList(sessionData.names));
        ArrayList<Integer> newAvailable = new ArrayList<>(available);
        for (int item : sessionData.available) {
            newAvailable.add(item);
        }
        available = newAvailable;
    }

    public void removeOrder(int orderNumber) {
        orders.remove(orderNumber);
        if (fragment != null) {
            Server.wait(500);
            fragment.showOrder(orders);
        }
    }

    protected void addOrder(Order order) {
        orders.add(order);
        if (fragment != null) {
            Server.wait(2);
            fragment.showOrder(orders);
            fragment.showOrder(orders);
        }
        for (int i = 0; i < 8; i++) {
            available.set(i, available.get(i) - order.getAmounts().get(i));
        }
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
            ArrayList<Integer> items = order.getAmounts();
            for (int i = 0; i < 8; i++) {
                total.set(i, total.get(i) + items.get(i));
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
                fragment.showOrder(orders);
            }
        }
    }
    protected void startSession(ArrayList<Integer> newAvailable, ArrayList<String> newItems) {
        available = newAvailable;
        items = newItems;
        if (!orders.isEmpty()) {
            for (int i = 0; i < orders.size(); i++) {
                orders.get(i).setItems(items);
            }
        }
    }

    protected boolean hasItems() {return !items.isEmpty();}
    protected boolean hasAvailables() {return !available.isEmpty();}
    protected void showSnackbar(String message) {
        Snackbar.make(binding.toolbar, message, Snackbar.LENGTH_LONG)
                .setAction(message, null).show();
    }
}