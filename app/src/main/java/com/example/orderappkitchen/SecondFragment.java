package com.example.orderappkitchen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.orderappkitchen.databinding.FragmentSecondBinding;

import java.util.ArrayList;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    public EditText[] itemNames;
    public EditText[] availableIns;
    public MainActivity activity;
    public String joinCode = MainActivity.getJoinCode();

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSecond.setOnClickListener(view1 -> {
            NavHostFragment.findNavController(SecondFragment.this).navigate(R.id.action_SecondFragment_to_FirstFragment);
            ArrayList<Integer> available = new ArrayList<>();
            ArrayList<String> items = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                if (String.valueOf(availableIns[i]).equals("")) {
                    available.add(0);
                } else {
                    try {
                        available.add(Integer.valueOf(String.valueOf(availableIns[i].getText())));
                    } catch (Exception e){
                        available.add(0);
                    }
                }
                items.add(String.valueOf(itemNames[i].getText()));
            }
            if (activity != null) {
                activity.startSession(available, items);
                activity.showSnackbar("Session Started");
            } else {
                Log.d("ERROR", "Activity was null");
            }
        });
        activity = (MainActivity)getActivity();
        itemNames = new EditText[] {binding.itemOne, binding.itemTwo, binding.itemThree, binding.itemFour, binding.itemFive, binding.itemSix, binding.itemSeven, binding.itemEight};
        availableIns = new EditText[] {binding.availableOne, binding.availableTwo, binding.availableThree, binding.availableFour, binding.availableFive, binding.availableSix, binding.availableSeven, binding.availableEight};
        binding.newSession.setOnClickListener(view2 -> {
            ArrayList<Integer> available = new ArrayList<>();
            ArrayList<String> items = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                if (String.valueOf(availableIns[i]).equals("")) {
                    available.add(0);
                } else {
                    try {
                        available.add(Integer.valueOf(String.valueOf(availableIns[i].getText())));
                    } catch (Exception e){
                        available.add(0);
                    }
                }
                items.add(String.valueOf(itemNames[i].getText()));
            }
            if (activity != null) {
                activity.startSession(available, items);
                activity.showSnackbar("Session Started");
            } else {
                Log.d("ERROR", "Activity was null");
            }
        });
        if (activity.hasItems()) {
            ArrayList<String> currentItems = activity.getItems();
            for (int i = 0; i < currentItems.size(); i++) {
                itemNames[i].setText(currentItems.get(i));
            }
        }
        if (activity.hasAvailables()) {
            ArrayList<Integer> currentAvailables = activity.getAvailable();
            for (int i = 0; i < currentAvailables.size(); i++) {
                availableIns[i].setText(String.valueOf(currentAvailables.get(i)));
            }
        }
        binding.joinCodeSecond.setText("Join Code: " + joinCode);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}