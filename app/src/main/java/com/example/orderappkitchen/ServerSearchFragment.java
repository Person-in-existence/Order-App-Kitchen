package com.example.orderappkitchen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.orderappkitchen.databinding.FragmentServerSearchBinding;

import java.util.Arrays;

import networking.Device;
import networking.Network;

public class ServerSearchFragment extends Fragment {
    private FragmentServerSearchBinding binding;

    private MainActivity activity;
    private boolean scanRunning = false;
    private boolean joinRunning = false;

    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = com.example.orderappkitchen.databinding.FragmentServerSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activity = (MainActivity) requireActivity();
        // Scan devices when view made
        scanDevices();
        // TODO: NO MULTIPLE SCANS AT ONCE
        binding.serverScanButton.setOnClickListener(view1->{
            scanDevices();
        });

    }
    private void scanDevices() {
        // Don't do multiple scans at once
        if (scanRunning) {
            return;
        }
        scanRunning = true;

        // Clear existing devices
        binding.deviceTable.removeAllViews();

        binding.scanProgressBar.setVisibility(View.VISIBLE);
        ServerSearchFragment thisReference = this;
        new Thread() {
            public void run() {
                Network.scanDevices(thisReference::addDevice, thisReference::onTimeout);
            }
        }.start();
    }
    public void addDevice(Device device) {
        // Only accept server devices
        if (device.deviceType != 1) {
            Log.d("ServerSearchFragment.addDevice()", "Found device " + device + " but device type not server");
            return;
        }
        try {
            Log.d("ServerSearchFragment", "New device! " + device);
            activity.runOnUiThread(()->{
                try {
                    TableRow deviceRow = new TableRow(activity);

                    // Device name
                    TextView nameView = new TextView(activity);
                    nameView.setPadding(5,5,16,5);
                    nameView.setText(device.name);
                    deviceRow.addView(nameView);

                    // Button
                    Button button = new Button(activity);
                    button.setText("Connect!");
                    button.setOnClickListener(view->joinServer(device));
                    deviceRow.addView(button);


                    binding.deviceTable.addView(deviceRow);

                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }
    public void updateProgressBar(int percent) {
        binding.scanProgressBar.setProgress(percent);
    }
    private void joinServer(Device device) {
        // Don't do multiple at once
        if (joinRunning) {
            return;
        }
        joinRunning = true;
        Network.joinServer(device.getJoinCode(), this::onSuccess);

    }
    private void onSuccess(boolean success) {
        activity.runOnUiThread(()->{
            joinRunning = false;
            if (success) {
                NavHostFragment.findNavController(this).navigate(R.id.action_externalServerFragment_to_SecondFragment);
                activity.showSnackbar("Connection success!");
            } else {
                activity.showSnackbar("Connection failed!");
            }
        });
    }
    public void onTimeout() {
        try {
            scanRunning = false;
            requireActivity().runOnUiThread(() -> {
                binding.scanProgressBar.setVisibility(View.GONE);

            });
        } catch (IllegalStateException e) {
            Log.w("ExternalServerFragment", "Error in setdevices - " + Arrays.toString(e.getStackTrace()));
        }
    }
}
