package com.example.orderappkitchen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.orderappkitchen.databinding.FragmentFirstBinding;

import java.util.ArrayList;
import java.util.Objects;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    public ArrayList<String> ordersDisplay;
    public ArrayList<Button> buttons;
    public MainActivity activity;
    public boolean deleteMode = false;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });
        Log.d("joinCode", MainActivity.getJoinCode());
        binding.joinCode.setText("Join Code:" + MainActivity.getJoinCode());
        buttons = new ArrayList<>();
        activity = (MainActivity)getActivity();
        assert activity != null;
        activity.setFragment(this);
        binding.deleteMode.setOnClickListener(View2 -> {
            deleteMode = !deleteMode;
        });

    }

    @Override
    public void onDestroyView() {
        activity.setFragment(null);
        super.onDestroyView();
        deleteMode = false;
        binding = null;
    }
    public void showOrder(ArrayList<Order> orders) {
        ArrayList<String> ordersToDisplay = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            ordersToDisplay.add(orders.get(i).getText());
        displayOrders(ordersToDisplay);
        }
    }
    public void reTag(int tag) {
        if (tag != 0) {
            for (int i = tag - 1; i < buttons.size(); i++) {
                buttons.get(i).setTag(i);
            }
        } else {for (int i = 0; i < buttons.size(); i++) {buttons.get(i).setTag(i);}}
    }
    public void displayOrders(ArrayList<String> texts) {
        activity.runOnUiThread(() -> {
            ArrayList<String> items = ((MainActivity)getActivity()).getItems();
            ArrayList<Integer> total = ((MainActivity)getActivity()).getTotal();
            LinearLayout scroll = binding.buttonHolder;
            int size = texts.size();
            int current = 0;
            // replace texts.size() with variable size
            if (buttons.size() == texts.size()) {
                System.out.println(buttons.size());
                System.out.println(texts.size());
                for (int i = 0; i < size; i++) {
                    if (size == i) {
                        break;
                    }
                    buttons.get(i).setText(texts.get(i));
                    buttons.get(i).setTag(i);
                }
            }
            else if (buttons.size() <= size) {
                for (int i = 0; i < buttons.size(); i++) {
                    buttons.get(i).setText(texts.get(i));
                    buttons.get(i).setTag(i);
                    current = i+1;
                }
                for (int i = current; i < size; i++) {
                    buttons.add(new Button(getContext()));
                    buttons.get(i).setText(texts.get(i));
                    buttons.get(i).setTag(i);
                    buttons.get(i).setOnClickListener(view -> {
                        if (deleteMode) {
                            Button thisButton = (Button)view;
                            if (thisButton != null) {
                                int tag = Integer.parseInt(String.valueOf(thisButton.getTag()));
                                buttons.remove(thisButton);
                                activity.removeOrder(tag);
                                LinearLayout parent = (LinearLayout) thisButton.getParent();
                                System.out.println(parent);
                                parent.removeView(view);
                                reTag(tag);
                                Log.d("Tag", String.valueOf(thisButton.getTag()));
                            }
                        }
                    });
                    scroll.addView(buttons.get(i));
                }
            }
            else {
                for (int i = 0; i < size; i++) {
                    buttons.get(i).setText(texts.get(i));
                    buttons.get(i).setTag(i);
                    current = i;
                }
                for (int i = current; i < buttons.size(); i++) {
                    scroll.removeView(buttons.get(current));
                    buttons.get(current).setVisibility(View.GONE);
                    buttons.remove(current);
                }
            }
            String total_one = "";
            for (int i = 0; i < 4; i++) {
                if (items.get(i) != null) {
                    if (!Objects.equals(items.get(i), "")) {
                        total_one = total_one + total.get(i) + " " + items.get(i);
                        if (total.get(i) != 1) {
                            total_one += "s";
                        }
                        total_one += ", ";
                    }
                }
            }
            binding.totalFirstFour.setText(total_one);
            String total_two = "";
            for (int i = 4; i < 8; i++) {
                if (!Objects.equals(items.get(i), "")) {
                    total_two = total_two + total.get(i) + " " + items.get(i);
                    if (total.get(i) != 1) {
                        total_two += "s";
                    }
                    total_two += ", ";
                }
            }
            binding.totalSecondFour.setText(total_two);
        });

    }
}