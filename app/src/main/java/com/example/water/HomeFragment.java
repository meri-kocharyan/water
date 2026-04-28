package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.water.supabase.SupabaseAuthHelper;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvWelcome;
    private RecyclerView rvNewest;
    private BookAdapter adapter;
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        // Remove all code except this minimal safe block
        return view;
    }

    /*
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        //tvWelcome = view.findViewById(R.id.tvWelcome);
        //rvNewest = view.findViewById(R.id.rvNewest);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        // Set welcome message
        String username = sessionManager.getUserEmail(); // or you could fetch actual username from profile
        if (username != null && !username.isEmpty()) {
            tvWelcome.setText("Welcome back, " + username + "!");
        } else {
            tvWelcome.setText("Welcome!");
        }

        // Setup RecyclerView
        rvNewest.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookAdapter(new ArrayList<>(), book -> {
            // Later: open reading view. For now, just toast.
            Toast.makeText(getContext(), "Open: " + book.getTitle(), Toast.LENGTH_SHORT).show();
        });
        rvNewest.setAdapter(adapter);

        loadNewestBooks();

        return view;
    }
    */

    private void loadNewestBooks() {
        // Use empty query to get newest, use anon key for public read
        String token = sessionManager.getAccessToken();
        authHelper.fetchBooks(token, "", new SupabaseAuthHelper.BooksCallback() {
            @Override
            public void onSuccess(List<Book> books) {
                adapter.updateList(books);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load books: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
