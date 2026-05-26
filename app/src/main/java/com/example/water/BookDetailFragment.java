package com.example.water;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookDetailFragment extends Fragment {

    private static final String ARG_BOOK_ID = "book_id";
    private String bookId;

    private TextView tvFandom, tvTitle, tvDate, tvAuthor;
    private TextView tvWarnings, tvRelationships, tvCharacters, tvFreeforms;
    private TextView tvSummary, tvStats, tvUpdated;
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

        tvFandom        = view.findViewById(R.id.tvDetailFandom);
        tvTitle         = view.findViewById(R.id.tvDetailTitle);
        tvDate          = view.findViewById(R.id.tvDetailDate);
        tvAuthor        = view.findViewById(R.id.tvDetailAuthor);
        tvWarnings      = view.findViewById(R.id.tvDetailWarnings);
        tvRelationships = view.findViewById(R.id.tvDetailRelationships);
        tvCharacters    = view.findViewById(R.id.tvDetailCharacters);
        tvFreeforms     = view.findViewById(R.id.tvDetailFreeforms);
        tvSummary       = view.findViewById(R.id.tvDetailSummary);
        tvStats         = view.findViewById(R.id.tvDetailStats);
        tvUpdated       = view.findViewById(R.id.tvDetailUpdated);
        btnStartReading = view.findViewById(R.id.btnStartReading);
        rvChapters      = view.findViewById(R.id.rvChapters);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        rvChapters.setLayoutManager(new LinearLayoutManager(getContext()));
        chapterAdapter = new ChapterAdapter(new ArrayList<>(), chapter -> {
            ChapterViewFragment viewer = ChapterViewFragment.newInstance(chapter.getId());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, viewer)
                    .addToBackStack("chapter_view")
                    .commit();
        });
        rvChapters.setAdapter(chapterAdapter);

        btnStartReading.setOnClickListener(v -> {
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
                displayBook(book);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load book details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayBook(Book book) {
        // Parse tags (same logic as BookAdapter)
        String fandom = "";
        StringBuilder warnings = new StringBuilder();
        StringBuilder relationships = new StringBuilder();
        StringBuilder characters = new StringBuilder();
        StringBuilder freeforms = new StringBuilder();
        String language = "";
        String rating = "";

        if (book.getTags() != null) {
            for (String tag : book.getTags()) {
                if (tag.startsWith("Fandom:")) {
                    fandom = tag.substring(7).trim();
                } else if (tag.startsWith("Warning:")) {
                    if (warnings.length() > 0) warnings.append(", ");
                    warnings.append(tag.substring(8).trim());
                } else if (tag.startsWith("Relationship:")) {
                    if (relationships.length() > 0) relationships.append(", ");
                    relationships.append(tag.substring(13).trim());
                } else if (tag.startsWith("Character:")) {
                    if (characters.length() > 0) characters.append(", ");
                    characters.append(tag.substring(10).trim());
                } else if (tag.startsWith("Language:")) {
                    language = tag.substring(9).trim();
                } else if (tag.startsWith("Rating:")) {
                    rating = tag.substring(7).trim();
                } else {
                    if (freeforms.length() > 0) freeforms.append(", ");
                    freeforms.append(tag);
                }
            }
        }

        tvFandom.setText(fandom);

        // Title + Author in one line with colors (matching adapter)
        String author = book.getAuthor_username() != null ? book.getAuthor_username() : "Unknown";
        String titlePart = book.getTitle();
        String authorPart = " by " + author;
        SpannableString span = new SpannableString(titlePart + authorPart);
        span.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.ao3_title)),
                0, titlePart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(0xFF000000),
                titlePart.length(), span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvTitle.setText(span);

        // Date
        String datePosted = formatDate(book.getDatePosted());
        if (!datePosted.isEmpty()) {
            tvDate.setText(datePosted);
            tvDate.setVisibility(View.VISIBLE);
        } else {
            tvDate.setVisibility(View.GONE);
        }

        tvAuthor.setText("by " + author);

        // Tags visibility
        String warnText = warnings.toString();
        if (!warnText.isEmpty()) {
            tvWarnings.setText(warnText);
            tvWarnings.setVisibility(View.VISIBLE);
        } else {
            tvWarnings.setVisibility(View.GONE);
        }

        String relText = relationships.toString();
        if (!relText.isEmpty()) {
            tvRelationships.setText(relText);
            tvRelationships.setVisibility(View.VISIBLE);
        } else {
            tvRelationships.setVisibility(View.GONE);
        }

        String charText = characters.toString();
        if (!charText.isEmpty()) {
            tvCharacters.setText(charText);
            tvCharacters.setVisibility(View.VISIBLE);
        } else {
            tvCharacters.setVisibility(View.GONE);
        }

        String freeText = freeforms.toString();
        if (!freeText.isEmpty()) {
            tvFreeforms.setText(freeText);
            tvFreeforms.setVisibility(View.VISIBLE);
        } else {
            tvFreeforms.setVisibility(View.GONE);
        }

        // Summary
        String summary = book.getDescription();
        if (summary != null && !summary.isEmpty()) {
            tvSummary.setText(summary);
            tvSummary.setVisibility(View.VISIBLE);
        } else {
            tvSummary.setVisibility(View.GONE);
        }

        // Stats
        StringBuilder stats = new StringBuilder();
        if (!language.isEmpty()) stats.append("Language: ").append(language).append("  ");
        if (!rating.isEmpty())   stats.append("Rating: ").append(rating).append("  ");
        stats.append("Words: ").append(book.getWord_count());
        stats.append("  Chapters: ").append(book.getChapter_count());
        tvStats.setText(stats.toString().trim());

        // Last updated
        String updated = formatDate(book.getLastUpdated());
        if (!updated.isEmpty()) {
            tvUpdated.setText("Updated: " + updated);
            tvUpdated.setVisibility(View.VISIBLE);
        } else {
            tvUpdated.setVisibility(View.GONE);
        }
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

    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat input = new SimpleDateFormat(pattern, Locale.getDefault());
                Date date = input.parse(raw);
                if (date != null) {
                    return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date);
                }
            } catch (ParseException ignored) {}
        }
        return raw;
    }
}