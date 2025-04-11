package com.example.orderappkitchen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.orderappkitchen.databinding.FragmentExternalServerBinding;

import java.util.Arrays;

import networking.Device;
import networking.Network;

public class ExternalServerFragment extends Fragment {
    private FragmentExternalServerBinding binding;

    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = com.example.orderappkitchen.databinding.FragmentExternalServerBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Scan devices when view made
        scanDevices();
        // TODO: NO MULTIPLE SCANS AT ONCE
        binding.serverScanButton.setOnClickListener(view1->{
            scanDevices();
        });

    }
    private void scanDevices() {
        binding.scanProgressBar.setVisibility(View.VISIBLE);
        ExternalServerFragment thisReference = this;
        new Thread() {
            public void run() {
                Network.scanDevices(thisReference::addDevice, thisReference::onTimeout);
            }
        }.start();
    }
    public void addDevice(Device device) {

    }
    public void updateProgressBar(int percent) {
        binding.scanProgressBar.setProgress(percent);
    }
    public void onTimeout() {
        try {
            requireActivity().runOnUiThread(() -> {
                binding.scanProgressBar.setVisibility(View.GONE);

            });
        } catch (IllegalStateException e) {
            Log.w("ExternalServerFragment", "Error in setdevices - " + Arrays.toString(e.getStackTrace()));
        }
    }
}
