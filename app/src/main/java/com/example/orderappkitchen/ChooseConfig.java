package com.example.orderappkitchen;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.orderappkitchen.databinding.FragmentChooseConfigBinding;


public class ChooseConfig extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private FragmentChooseConfigBinding binding;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private MainActivity activity;

    public ChooseConfig() {
        // Required empty public constructor
    }

    public static ChooseConfig newInstance() {
        return new ChooseConfig();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();
        // Add listeners to the buttons
        binding.externalServerButton.setOnClickListener(view1->externalServer());
        binding.thisDeviceButton.setOnClickListener(view1->thisDevice());



    }

    private void externalServer() {
        activity.reset();
        // Set the connection type in the activity
        activity.setConnectionType(MainActivity.ConnectionType.EXTERNAL);
        // Navigate to external server connect
        NavHostFragment.findNavController(this).navigate(R.id.action_chooseConfig_to_externalServerFragment);

    }
    private void thisDevice() {
        activity.reset();
        // Set the connection type in the activity
        activity.setConnectionType(MainActivity.ConnectionType.DEVICE);
        // Navigate to next fragment
        NavHostFragment.findNavController(this).navigate(R.id.action_chooseConfig_to_SecondFragment);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentChooseConfigBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
}