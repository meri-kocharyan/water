package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class BookDetailFragment extends Fragment {

    private static final String ARG_BOOK_ID = "book_id";
    private String bookId;

    private TextView tvTitle, tvAuthor, tvSummary, tvFandom, tvWarning, tvLanguage, tvRating, tvStats;
    private Button btnStartReading;
    private RecyclerView rvChapters;
    private ChapterAdapter chapterAdapter;

    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    public static BookDetailFragment newInstance(String bookId) {
        BookDetailFragment frag = new BookDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BOOK_ID, bookId);
        frag.setArguments(args);
        return frag;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_detail, container, false);

        tvTitle = view.findViewById(R.id.tvDetailTitle);
        tvAuthor = view.findViewById(R.id.tvDetailAuthor);
        tvSummary = view.findViewById(R.id.tvDetailSummary);
        tvFandom = view.findViewById(R.id.tvDetailFandom);
        tvWarning = view.findViewById(R.id.tvDetailWarning);
        tvLanguage = view.findViewById(R.id.tvDetailLanguage);
        tvRating = view.findViewById(R.id.tvDetailRating);
        tvStats = view.findViewById(R.id.tvDetailStats);
        btnStartReading = view.findViewById(R.id.btnStartReading);
        rvChapters = view.findViewById(R.id.rvChapters);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        rvChapters.setLayoutManager(new LinearLayoutManager(getContext()));
        chapterAdapter = new ChapterAdapter(new ArrayList<>(), chapter -> {
            // Open chapter reading view
            ChapterViewFragment viewer = ChapterViewFragment.newInstance(chapter.getId());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, viewer)
                    .addToBackStack("chapter_view")
                    .commit();
        });
        rvChapters.setAdapter(chapterAdapter);

        btnStartReading.setOnClickListener(v -> {
            // Fetch chapters and open the first one if available
            String token = sessionManager.getAccessToken();
            authHelper.fetchChaptersByBookId(token, bookId, new SupabaseAuthHelper.ChaptersCallback() {
                @Override
                public void onSuccess(List<Chapter> chapters) {
                    if (!chapters.isEmpty()) {
                        Chapter first = chapters.get(0);
                        ChapterViewFragment viewer = ChapterViewFragment.newInstance(first.getId());
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, viewer)
                                .addToBackStack("chapter_view")
                                .commit();
                    } else {
                        Toast.makeText(getContext(), "No chapters yet", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        loadBookDetails();
        loadChapters();

        return view;
    }

    private void loadBookDetails() {
        String token = sessionManager.getAccessToken();
        authHelper.fetchBookById(token, bookId, new SupabaseAuthHelper.BookCallback() {
            @Override
            public void onSuccess(Book book) {
                tvTitle.setText(book.getTitle());

                // Author – we just show "Author" for now (can fetch username later)
                tvAuthor.setText("by " + (book.getAuthor_username() != null ? book.getAuthor_username() : "Unknown"));

                tvSummary.setText(book.getDescription());

                // Parse tags
                String fandom = "", warning = "", language = "", rating = "";
                if (book.getTags() != null) {
                    for (String tag : book.getTags()) {
                        if (tag.startsWith("Fandom:")) fandom = tag.substring(7);
                        else if (tag.startsWith("Warning:")) warning = tag.substring(8);
                        else if (tag.startsWith("Language:")) language = tag.substring(9);
                        else if (tag.startsWith("Rating:")) rating = tag.substring(7);
                    }
                }
                tvFandom.setText("Fandom: " + fandom);
                tvWarning.setText("Warning: " + (warning.isEmpty() ? "None" : warning));
                tvLanguage.setText("Language: " + (language.isEmpty() ? "Not specified" : language));
                tvRating.setText("Rating: " + (rating.isEmpty() ? "Not Rated" : rating));

                tvStats.setText(book.getWord_count() + " words | " + book.getChapter_count() + " chapters");
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load book details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadChapters() {
        String token = sessionManager.getAccessToken();
        authHelper.fetchChaptersByBookId(token, bookId, new SupabaseAuthHelper.ChaptersCallback() {
            @Override
            public void onSuccess(List<Chapter> chapters) {
                chapterAdapter.updateList(chapters);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load chapters", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
