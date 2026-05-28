package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.water.supabase.SupabaseAuthHelper;

import java.util.ArrayList;
import java.util.List;

public class CreateChapterFragment extends Fragment {

    private EditText etChapterTitle, etChapterSummary, etNotesAbove, etNotesBelow, etChapterContent;
    private CheckBox cbAddSummary, cbNotesAbove, cbNotesBelow;
    private Button btnPublish;

    // Metadata received from previous fragment
    private String bookTitle, bookDescription, language, rating;
    private ArrayList<String> fandoms;

    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    private String existingBookId = null;  // field

    private int nextChapterNumber = 1;

    private boolean isAnonymous = false;
    private boolean commentsDisabled = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_chapter, container, false);

        // Bind views
        etChapterTitle = view.findViewById(R.id.etChapterTitle);
        etChapterSummary = view.findViewById(R.id.etChapterSummary);
        etNotesAbove = view.findViewById(R.id.etNotesAbove);
        etNotesBelow = view.findViewById(R.id.etNotesBelow);
        etChapterContent = view.findViewById(R.id.etChapterContent);
        cbAddSummary = view.findViewById(R.id.cbAddSummary);
        cbNotesAbove = view.findViewById(R.id.cbNotesAbove);
        cbNotesBelow = view.findViewById(R.id.cbNotesBelow);
        btnPublish = view.findViewById(R.id.btnPublish);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        // Retrieve metadata from arguments
        Bundle args = getArguments();
        if (args != null) {
            bookTitle = args.getString("title", "");
            bookDescription = args.getString("description", "");
            String authorNotes = args.getString("notes", "");      // ← ADD
            isAnonymous = args.getBoolean("anonymous", false);
            commentsDisabled = args.getBoolean("commentsDisabled", false);

            language = args.getString("language", "Other");
            rating = args.getString("rating", "Not Rated");
            fandoms = args.getStringArrayList("fandoms");
            if (fandoms == null) fandoms = new ArrayList<>();

            // Pre-fill the "notes above" field if the author wrote a preface
            if (!authorNotes.isEmpty()) {
                etNotesAbove.setText(authorNotes);
                etNotesAbove.setVisibility(View.VISIBLE);
                cbNotesAbove.setChecked(true);
            }

            // existingBookId handling (you have a second block below, keep that)
        }

        if (args != null) {
            existingBookId = args.getString("book_id", null);
            if (existingBookId != null) {
                nextChapterNumber = args.getInt("chapter_number", 1);
            }
        }

        // Checkbox toggles
        cbAddSummary.setOnCheckedChangeListener((buttonView, isChecked) ->
                etChapterSummary.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        cbNotesAbove.setOnCheckedChangeListener((buttonView, isChecked) ->
                etNotesAbove.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        cbNotesBelow.setOnCheckedChangeListener((buttonView, isChecked) ->
                etNotesBelow.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        // Pre-fill default chapter title
        etChapterTitle.setText("Chapter " + nextChapterNumber);

        btnPublish.setOnClickListener(v -> publishEverything());

        // Update header
        TextView tvHeader = view.findViewById(R.id.tvChapterHeader);
        if (tvHeader != null) {
            tvHeader.setText("Chapter " + nextChapterNumber);
        }

        return view;






    }

    private void publishEverything() {
        String chapterTitle = etChapterTitle.getText().toString().trim();
        String chapterContent = etChapterContent.getText().toString().trim();
        if (chapterContent.isEmpty()) {
            Toast.makeText(getContext(), "Chapter content cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String chapterSummary = cbAddSummary.isChecked() ? etChapterSummary.getText().toString().trim() : "";
        String notesAbove = cbNotesAbove.isChecked() ? etNotesAbove.getText().toString().trim() : "";
        String notesBelow = cbNotesBelow.isChecked() ? etNotesBelow.getText().toString().trim() : "";

        // Build tags list
        List<String> tags = new ArrayList<>();
        Bundle args = getArguments();
        if (args != null) {
            ArrayList<String> passedTags = args.getStringArrayList("tags");
            if (passedTags != null) {
                tags.addAll(passedTags);
            }
        }


        String token = sessionManager.getAccessToken();
        String authorId = sessionManager.getUserId();
        if (token == null || authorId == null) {
            Toast.makeText(getContext(), "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }


        if (existingBookId != null) {
            // Adding chapter to existing book
            authHelper.createChapter(token, existingBookId, nextChapterNumber, chapterTitle,
                    chapterContent, chapterSummary, notesAbove, notesBelow,
                    new SupabaseAuthHelper.ChapterCallback() {
                        @Override
                        public void onSuccess(Chapter chapter) {
                            Toast.makeText(getContext(), "Chapter added!", Toast.LENGTH_SHORT).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(getContext(), "Failed to add chapter: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            // Create book first
            authHelper.publishBook(token, authorId, bookTitle, bookDescription, tags,
                    isAnonymous, commentsDisabled, new SupabaseAuthHelper.BookCallback() {
                        @Override
                        public void onSuccess(Book book) {
                            // Now create chapter 1
                            authHelper.createChapter(token, book.getId(), 1, chapterTitle,
                                    chapterContent, chapterSummary, notesAbove, notesBelow,
                                    new SupabaseAuthHelper.ChapterCallback() {
                                        @Override
                                        public void onSuccess(Chapter chapter) {
                                            Toast.makeText(getContext(), "Work published!", Toast.LENGTH_SHORT).show();
                                            // Pop back to My Works
                                            requireActivity().getSupportFragmentManager().popBackStack("my_works", 0);
                                        }

                                        @Override
                                        public void onError(String error) {
                                            Toast.makeText(getContext(), "Book created but chapter failed: " + error, Toast.LENGTH_LONG).show();
                                            requireActivity().getSupportFragmentManager().popBackStack("my_works", 0);
                                        }
                                    });
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(getContext(), "Failed to publish: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

}
