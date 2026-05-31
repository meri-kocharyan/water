package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.water.supabase.SupabaseAuthHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;


import com.example.water.R;

public class LibraryFragment extends Fragment {

    private RecyclerView rvBooks;
    private BookAdapter adapter;
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    private List<Book> bookList = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);
        rvBooks = view.findViewById(R.id.rvLibraryBooks);
        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        rvBooks.setLayoutManager(new LinearLayoutManager(getContext()));

        // We'll create a simple adapter that shows book title + delete button, not the full BookAdapter.
        // For now, we'll reuse BookAdapter but with a long-click to delete. Better: custom adapter with delete.
        // Let's use BookAdapter for display, and add a delete option via a button in a custom layout.
        // For simplicity, we'll use a SwipeRefreshLayout or a delete icon on each item.
        // I'll provide a custom LibraryAdapter below.



        DividerItemDecoration divider = new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.divider_thick));
        rvBooks.addItemDecoration(divider);


        // Using a dedicated adapter
        adapter = new BookAdapter(new ArrayList<>(), book -> {
            // Open book detail
            BookDetailFragment detailFrag = BookDetailFragment.newInstance(book.getId());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, detailFrag)
                    .addToBackStack("book_detail")
                    .commit();
        });
        rvBooks.setAdapter(adapter);

        loadLibrary();
        return view;
    }

    private void loadLibrary() {
        String token = sessionManager.getAccessToken();
        String userId = sessionManager.getUserId();
        if (token == null || userId == null) return;

        authHelper.fetchBookmarkedBooks(token, userId, new SupabaseAuthHelper.BooksCallback() {
            @Override public void onSuccess(List<Book> books) {
                adapter.updateList(books);
            }
            @Override public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}