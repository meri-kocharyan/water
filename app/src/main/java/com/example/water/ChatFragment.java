package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

public class ChatFragment extends Fragment {

    private TabLayout tabLayout;
    private FrameLayout container;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        this.container = view.findViewById(R.id.chatFragmentContainer);

        // Add tabs
        tabLayout.addTab(tabLayout.newTab().setText("Chats"));
        tabLayout.addTab(tabLayout.newTab().setText("Add Friend"));
        tabLayout.addTab(tabLayout.newTab().setText("Pending"));

        // Show Chats by default
        loadInnerFragment(new ChatListFragment());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: loadInnerFragment(new ChatListFragment()); break;
                    case 1: loadInnerFragment(new AddFriendFragment()); break;
                    case 2: loadInnerFragment(new PendingRequestsFragment()); break;
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    private void loadInnerFragment(Fragment fragment) {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.chatFragmentContainer, fragment)
                .commit();
    }
}