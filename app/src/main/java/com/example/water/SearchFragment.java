package com.example.water;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.water.supabase.SupabaseAuthHelper;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText etSearch;
    private RecyclerView rvResults;
    private BookAdapter adapter;
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;
    private Handler handler = new Handler();
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        etSearch = view.findViewById(R.id.etSearch);
        rvResults = view.findViewById(R.id.rvSearchResults);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookAdapter(new ArrayList<>(), book -> {
            // Open book detail
            BookDetailFragment detailFrag = BookDetailFragment.newInstance(book.getId());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, detailFrag)
                    .addToBackStack("book_detail")
                    .commit();
        });
        rvResults.setAdapter(adapter);

        // Search with debounce (300ms after typing stops)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
                searchRunnable = () -> performSearch(s.toString().trim());
                handler.postDelayed(searchRunnable, 300);
            }
        });

        return view;
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            adapter.updateList(new ArrayList<>());
            return;
        }
        String token = sessionManager.getAccessToken();
        authHelper.searchBooks(token, query, new SupabaseAuthHelper.BooksCallback() {
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