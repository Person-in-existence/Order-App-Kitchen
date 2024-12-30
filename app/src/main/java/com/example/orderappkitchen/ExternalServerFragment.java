package com.example.orderappkitchen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.orderappkitchen.databinding.FragmentExternalServerBinding;

import java.util.ArrayList;

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
        new Thread() {
            public void run() {
                setDevices(Network.scanDevices((int percent) -> updateProgressBar(percent)));
            }
        }.start();
    }
    public void updateProgressBar(int percent) {
        binding.scanProgressBar.setProgress(percent);
    }
    public void setDevices(ArrayList<Device> devices) {
        requireActivity().runOnUiThread(()->{
            binding.scanProgressBar.setVisibility(View.GONE);

        });
    }
}
