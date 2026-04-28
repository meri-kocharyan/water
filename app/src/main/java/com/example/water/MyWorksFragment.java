package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.water.supabase.SupabaseAuthHelper;

import java.util.ArrayList;
import java.util.List;

public class MyWorksFragment extends Fragment {

    private RecyclerView rvBooks;
    private Button btnNewWork;
    private BookAdapter adapter;
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_works, container, false);

        rvBooks = view.findViewById(R.id.rvMyBooks);
        btnNewWork = view.findViewById(R.id.btnNewWork);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        rvBooks.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new BookAdapter(new ArrayList<>(), book -> {
            BookManagementFragment managementFrag = BookManagementFragment.newInstance(book.getId());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, managementFrag)
                    .addToBackStack("book_management")
                    .commit();
        });
        rvBooks.setAdapter(adapter);

        btnNewWork.setOnClickListener(v -> {
            // Open the publish new work fragment (replace current)
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new PublishBookFragment())
                    .addToBackStack("publish")
                    .commit();
        });

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

    @Override
    public void onResume() {
        super.onResume();
        loadBooks(); // refresh when coming back from publish
    }
}
