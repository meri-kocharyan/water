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

public class BookManagementFragment extends Fragment {

    private static final String ARG_BOOK_ID = "book_id";

    private String bookId;
    private TabLayout tabLayout;
    private FrameLayout container;

    public static BookManagementFragment newInstance(String bookId) {
        BookManagementFragment fragment = new BookManagementFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BOOK_ID, bookId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bookId = getArguments().getString(ARG_BOOK_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_management, parent, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        container = view.findViewById(R.id.managementContainer);

        tabLayout.addTab(tabLayout.newTab().setText("Details"));
        tabLayout.addTab(tabLayout.newTab().setText("Chapters"));

        // Load default tab
        loadChildFragment(BookDetailsChildFragment.newInstance(bookId));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    loadChildFragment(BookDetailsChildFragment.newInstance(bookId));
                } else {
                    loadChildFragment(ChaptersListChildFragment.newInstance(bookId));
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    private void loadChildFragment(Fragment fragment) {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.managementContainer, fragment)
                .commit();
    }
}
