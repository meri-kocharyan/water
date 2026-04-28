package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.water.Book;
import com.example.water.R;
import com.example.water.SessionManager;
import com.example.water.supabase.SupabaseAuthHelper;

import java.util.Arrays;
import java.util.List;

public class BookDetailsChildFragment extends Fragment {

    private static final String ARG_BOOK_ID = "book_id";
    private String bookId;

    private TextView tvTitle, tvDescription, tvTags, tvLanguage, tvRating;
    private EditText etEditTitle, etEditDescription, etEditTags, etEditLanguage, etEditRating;
    private Button btnEdit, btnSave;
    private View displayMode, editMode;

    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;
    private Book currentBook;

    @NonNull
    public static BookDetailsChildFragment newInstance(String bookId) {
        BookDetailsChildFragment frag = new BookDetailsChildFragment();
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
        View view = inflater.inflate(R.layout.fragment_book_details_child, container, false);

        // Display views
        tvTitle = view.findViewById(R.id.tvBookTitle);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvTags = view.findViewById(R.id.tvTags);
        tvLanguage = view.findViewById(R.id.tvLanguage);
        tvRating = view.findViewById(R.id.tvRating);

        // Edit views
        etEditTitle = view.findViewById(R.id.etEditTitle);
        etEditDescription = view.findViewById(R.id.etEditDescription);
        etEditTags = view.findViewById(R.id.etEditTags);
        etEditLanguage = view.findViewById(R.id.etEditLanguage);
        etEditRating = view.findViewById(R.id.etEditRating);

        btnEdit = view.findViewById(R.id.btnEdit);
        btnSave = view.findViewById(R.id.btnSave);

        displayMode = view.findViewById(R.id.displayMode);
        editMode = view.findViewById(R.id.editMode);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        btnEdit.setOnClickListener(v -> switchToEditMode());
        btnSave.setOnClickListener(v -> saveChanges());

        fetchBookDetails();

        return view;
    }

    private void fetchBookDetails() {
        String token = sessionManager.getAccessToken();
        authHelper.fetchBookById(token, bookId, new SupabaseAuthHelper.BookCallback() {
            @Override
            public void onSuccess(Book book) {
                currentBook = book;
                populateDisplay(book);
                populateEditFields(book);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load book", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateDisplay(Book book) {
        tvTitle.setText(book.getTitle());
        tvDescription.setText(book.getDescription());
        // Combine tags as string
        String tagsStr = book.getTags() != null ? String.join(", ", book.getTags()) : "";
        tvTags.setText(tagsStr);
        // Language and Rating are embedded in tags (prefixed), we can parse them out later.
        // For now, show empty or extract.
        tvLanguage.setText("");
        tvRating.setText("");
        if (book.getTags() != null) {
            for (String tag : book.getTags()) {
                if (tag.startsWith("Language:")) tvLanguage.setText(tag.replace("Language:", ""));
                else if (tag.startsWith("Rating:")) tvRating.setText(tag.replace("Rating:", ""));
            }
        }
    }

    private void populateEditFields(Book book) {
        etEditTitle.setText(book.getTitle());
        etEditDescription.setText(book.getDescription());
        String tagsStr = book.getTags() != null ? String.join(", ", book.getTags()) : "";
        etEditTags.setText(tagsStr);
        etEditLanguage.setText(tvLanguage.getText());
        etEditRating.setText(tvRating.getText());
    }

    private void switchToEditMode() {
        displayMode.setVisibility(View.GONE);
        editMode.setVisibility(View.VISIBLE);
        btnEdit.setVisibility(View.GONE);
    }

    private void saveChanges() {
        String newTitle = etEditTitle.getText().toString().trim();
        String newDesc = etEditDescription.getText().toString().trim();
        String tagsRaw = etEditTags.getText().toString().trim();
        String lang = etEditLanguage.getText().toString().trim();
        String rating = etEditRating.getText().toString().trim();

        if (newTitle.isEmpty()) {
            Toast.makeText(getContext(), "Title required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reconstruct tags list with proper prefixes
        List<String> newTags = new java.util.ArrayList<>();
        if (!lang.isEmpty()) newTags.add("Language:" + lang);
        if (!rating.isEmpty()) newTags.add("Rating:" + rating);
        if (!tagsRaw.isEmpty()) {
            // Preserve existing non‑Language/Rating tags
            if (currentBook != null && currentBook.getTags() != null) {
                for (String tag : currentBook.getTags()) {
                    if (!tag.startsWith("Language:") && !tag.startsWith("Rating:")) {
                        newTags.add(tag);
                    }
                }
            }
        }

        String token = sessionManager.getAccessToken();
        authHelper.updateBook(token, bookId, newTitle, newDesc, newTags, new SupabaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String a, String b, String c, String d) {
                Toast.makeText(getContext(), "Saved!", Toast.LENGTH_SHORT).show();
                // Update local book object and switch back
                currentBook.setTitle(newTitle);
                currentBook.setDescription(newDesc);
                currentBook.setTags(newTags);
                populateDisplay(currentBook);
                editMode.setVisibility(View.GONE);
                displayMode.setVisibility(View.VISIBLE);
                btnEdit.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }
}