package com.example.water;

import android.content.DialogInterface;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.water.supabase.SupabaseAuthHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyBookDetailFragment extends Fragment {

    private static final String ARG_BOOK_ID = "book_id";
    private String bookId;

    private TextView tvTitle, tvDate, tvFandom;
    private TextView tvWarnings, tvRating, tvCategories, tvRelationships, tvCharacters, tvFreeforms;
    private TextView tvStats;
    private Button btnEdit, btnDelete;

    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    public static MyBookDetailFragment newInstance(String bookId) {
        MyBookDetailFragment frag = new MyBookDetailFragment();
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
        View view = inflater.inflate(R.layout.fragment_my_book_detail, container, false);

        tvTitle = view.findViewById(R.id.tvDetailTitle);
        tvDate = view.findViewById(R.id.tvDetailDate);
        tvFandom = view.findViewById(R.id.tvDetailFandom);
        tvWarnings = view.findViewById(R.id.tvDetailWarnings);
        tvRating = view.findViewById(R.id.tvDetailRating);
        tvCategories = view.findViewById(R.id.tvDetailCategories);
        tvRelationships = view.findViewById(R.id.tvDetailRelationships);
        tvCharacters = view.findViewById(R.id.tvDetailCharacters);
        tvFreeforms = view.findViewById(R.id.tvDetailFreeforms);
        tvStats = view.findViewById(R.id.tvDetailStats);
        btnEdit = view.findViewById(R.id.btnEdit);
        btnDelete = view.findViewById(R.id.btnDelete);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        loadBookDetails();

        btnEdit.setOnClickListener(v -> {
            // Open edit screen
            EditBookFragment editFrag = EditBookFragment.newInstance(bookId);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, editFrag)
                    .addToBackStack("edit_book")
                    .commit();
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete work")
                    .setMessage("Are you sure you want to permanently delete this work?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        String token = sessionManager.getAccessToken();
                        authHelper.deleteBook(token, bookId, new SupabaseAuthHelper.AuthCallback() {
                            @Override
                            public void onSuccess(String a, String b, String c, String d) {
                                Toast.makeText(getContext(), "Work deleted", Toast.LENGTH_SHORT).show();
                                // Go back to My Works
                                requireActivity().getSupportFragmentManager().popBackStack();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(getContext(), "Failed to delete: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

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
        // Same parsing logic as BookDetailFragment
        String fandom = "";
        StringBuilder warnings = new StringBuilder();
        StringBuilder categories = new StringBuilder();
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
                } else if (tag.startsWith("Category:")) {
                    if (categories.length() > 0) categories.append(", ");
                    categories.append(tag.substring(9).trim());
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

        // Title + author
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
        tvDate.setText(datePosted);
        tvDate.setVisibility(datePosted.isEmpty() ? View.GONE : View.VISIBLE);

        // Fandom
        tvFandom.setText(fandom.isEmpty() ? "" : fandom);
        tvFandom.setVisibility(fandom.isEmpty() ? View.GONE : View.VISIBLE);

        // Warnings (bold)
        String warnText = warnings.toString();
        tvWarnings.setText(warnText.isEmpty() ? "" : "Archive Warning: " + warnText);
        tvWarnings.setVisibility(warnText.isEmpty() ? View.GONE : View.VISIBLE);

        // Rating (italic)
        tvRating.setText(rating.isEmpty() ? "" : "Rating: " + rating);
        tvRating.setVisibility(rating.isEmpty() ? View.GONE : View.VISIBLE);

        // Categories
        String catText = categories.toString();
        tvCategories.setText(catText.isEmpty() ? "" : "Category: " + catText);
        tvCategories.setVisibility(catText.isEmpty() ? View.GONE : View.VISIBLE);

        // Relationships
        String relText = relationships.toString();
        tvRelationships.setText(relText.isEmpty() ? "" : "Relationship: " + relText);
        tvRelationships.setVisibility(relText.isEmpty() ? View.GONE : View.VISIBLE);

        // Characters
        String charText = characters.toString();
        tvCharacters.setText(charText.isEmpty() ? "" : "Character: " + charText);
        tvCharacters.setVisibility(charText.isEmpty() ? View.GONE : View.VISIBLE);

        // Freeforms
        String freeText = freeforms.toString();
        tvFreeforms.setText(freeText.isEmpty() ? "" : "Additional Tags: " + freeText);
        tvFreeforms.setVisibility(freeText.isEmpty() ? View.GONE : View.VISIBLE);

        // Stats
        StringBuilder stats = new StringBuilder();
        if (!language.isEmpty()) stats.append("Language: ").append(language).append("  ");
        if (!rating.isEmpty()) stats.append("Rating: ").append(rating).append("  ");
        stats.append("Words: ").append(book.getWord_count());
        stats.append("  Chapters: ").append(book.getChapter_count());
        tvStats.setText(stats.toString().trim());
    }

    private String formatDate(String raw) {
        // same as before
        if (raw == null || raw.isEmpty()) return "";
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"};
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