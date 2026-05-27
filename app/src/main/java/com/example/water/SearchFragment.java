package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    private EditText etSearchText;
    private Button btnSearch;
    private RecyclerView rvResults;
    private BookAdapter adapter;
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        etSearchText = view.findViewById(R.id.etSearchText);
        btnSearch = view.findViewById(R.id.btnSearch);
        rvResults = view.findViewById(R.id.rvSearchResults);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookAdapter(new ArrayList<>(), book -> {
            // Open the book detail
            BookDetailFragment detailFrag = BookDetailFragment.newInstance(book.getId());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, detailFrag)
                    .addToBackStack("book_detail")
                    .commit();
        });
        rvResults.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> performSearch());

        // Also allow searching by pressing "Enter" on keyboard
        etSearchText.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });

        return view;
    }

    private void performSearch() {
        String query = etSearchText.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = sessionManager.getAccessToken();
        // Use the existing fetchBooks method that already searches title and description.
        // We'll also search by author_username because our view includes it.
        authHelper.fetchBooks(token, query, new SupabaseAuthHelper.BooksCallback() {
            @Override
            public void onSuccess(List<Book> books) {
                if (books.isEmpty()) {
                    Toast.makeText(getContext(), "No works found", Toast.LENGTH_SHORT).show();
                }
                adapter.updateList(books);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Search failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}