package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.water.supabase.SupabaseAuthHelper;

import java.util.ArrayList;
import java.util.List;

public class PublishedWorksFragment extends Fragment {

    private RecyclerView rvBooks;
    private BookAdapter adapter;
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_published_works, container, false);

        rvBooks = view.findViewById(R.id.rvPublishedBooks);
        rvBooks.setLayoutManager(new LinearLayoutManager(getContext()));

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        adapter = new BookAdapter(new ArrayList<>(), book -> {
            // Later: open reading view
            Toast.makeText(getContext(), "Open book: " + book.getTitle(), Toast.LENGTH_SHORT).show();
        });
        rvBooks.setAdapter(adapter);

        loadBooks();

        return view;
    }

    private void loadBooks() {
        String token = sessionManager.getAccessToken();
        String userId = sessionManager.getUserId();
        if (token == null || userId == null) return;

        authHelper.fetchMyBooks(token, userId, new SupabaseAuthHelper.BooksCallback() {
            @Override
            public void onSuccess(List<Book> books) {
                adapter.updateList(books);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
