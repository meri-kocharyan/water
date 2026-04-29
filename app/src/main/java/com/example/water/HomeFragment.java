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

public class HomeFragment extends Fragment {

    private TextView tvWelcome;
    private RecyclerView rvNewest;
    private BookAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvWelcome = view.findViewById(R.id.tvWelcome);
        rvNewest = view.findViewById(R.id.rvNewest);

        // Set welcome message
        SessionManager sm = new SessionManager(requireContext());
        String email = sm.getUserEmail();
        if (tvWelcome != null) {
            if (email != null && !email.isEmpty()) {
                tvWelcome.setText("Welcome back, " + email + "!");
            } else {
                tvWelcome.setText("Welcome, guest!");
            }
        }

        // Setup RecyclerView if available
        if (rvNewest != null) {
            rvNewest.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new BookAdapter(new ArrayList<>(), book -> {
                BookDetailFragment detailFrag = BookDetailFragment.newInstance(book.getId());
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, detailFrag)
                        .addToBackStack("book_detail")
                        .commit();
            });
            rvNewest.setAdapter(adapter);

            // Load books from Supabase
            SupabaseAuthHelper helper = new SupabaseAuthHelper();
            helper.fetchBooks(sm.getAccessToken(), "", new SupabaseAuthHelper.BooksCallback() {
                @Override
                public void onSuccess(java.util.List<Book> books) {
                    if (adapter != null) {
                        adapter.updateList(books);
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Could not load books", Toast.LENGTH_SHORT).show();
                }
            });
        }

        rvNewest.setLayoutManager(new LinearLayoutManager(getContext()));
        // Add this line for the divider
        rvNewest.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(), LinearLayoutManager.VERTICAL));

        return view;
    }
}
