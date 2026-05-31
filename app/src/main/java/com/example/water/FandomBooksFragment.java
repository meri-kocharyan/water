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

public class FandomBooksFragment extends Fragment {

    private static final String ARG_FANDOM = "fandom";
    private String fandomName;
    private RecyclerView rvBooks;
    private BookAdapter adapter;
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    private TextView tvFandomTitle, tvBookCount;


    public static FandomBooksFragment newInstance(String fandom) {
        FandomBooksFragment frag = new FandomBooksFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FANDOM, fandom);
        frag.setArguments(args);
        return frag;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) fandomName = getArguments().getString(ARG_FANDOM);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fandom_books, container, false);
        rvBooks = view.findViewById(R.id.rvFandomBooks);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        tvFandomTitle = view.findViewById(R.id.tvFandomTitle);
        tvBookCount = view.findViewById(R.id.tvFandomBookCount);

        tvFandomTitle.setText(fandomName);

        tvBookCount = view.findViewById(R.id.tvFandomBookCount);

        rvBooks.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookAdapter(new ArrayList<>(), book -> {
            BookDetailFragment detailFrag = BookDetailFragment.newInstance(book.getId());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, detailFrag)
                    .addToBackStack("book_detail")
                    .commit();
        });
        rvBooks.setAdapter(adapter);

        loadBooks();
        return view;
    }

    private void loadBooks() {
        String token = sessionManager.getAccessToken();
        authHelper.advancedSearch(token,
                null,                     // title
                null,                     // author
                fandomName,               // fandom
                null,                     // warnings
                null,                     // rating
                null,                     // categories
                null,                     // language
                null,                     // characters
                null,                     // relationships
                null,                     // freeforms
                0, 0,                     // min/max words
                new SupabaseAuthHelper.BooksCallback() {
                    @Override
                    public void onSuccess(List<Book> books) {
                        adapter.updateList(books);
                        if (tvBookCount != null) {
                            tvBookCount.setText(books.size() + " works");
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}