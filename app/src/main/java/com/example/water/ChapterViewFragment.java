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

import com.example.water.supabase.SupabaseAuthHelper;

public class ChapterViewFragment extends Fragment {

    private static final String ARG_CHAPTER_ID = "chapter_id";

    private String chapterId;

    private TextView tvTitle, tvNotesAbove, tvContent, tvNotesBelow;
    private EditText etTitle, etNotesAbove, etContent, etNotesBelow;
    private Button btnEdit, btnSave;

    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;
    private Chapter currentChapter;

    public static ChapterViewFragment newInstance(String chapterId) {
        ChapterViewFragment frag = new ChapterViewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAPTER_ID, chapterId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chapterId = getArguments().getString(ARG_CHAPTER_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chapter_view, container, false);

        tvTitle = view.findViewById(R.id.tvChapterTitle);
        tvNotesAbove = view.findViewById(R.id.tvNotesAbove);
        tvContent = view.findViewById(R.id.tvContent);
        tvNotesBelow = view.findViewById(R.id.tvNotesBelow);

        etTitle = view.findViewById(R.id.etChapterTitle);
        etNotesAbove = view.findViewById(R.id.etNotesAbove);
        etContent = view.findViewById(R.id.etContent);
        etNotesBelow = view.findViewById(R.id.etNotesBelow);

        btnEdit = view.findViewById(R.id.btnEdit);
        btnSave = view.findViewById(R.id.btnSave);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        btnEdit.setOnClickListener(v -> enableEditing(true));
        btnSave.setOnClickListener(v -> saveChanges());

        fetchChapter();

        return view;
    }

    private void fetchChapter() {
        String token = sessionManager.getAccessToken();
        // We need a method to fetch a single chapter by ID. We'll add it to helper.
        authHelper.fetchChapterById(token, chapterId, new SupabaseAuthHelper.ChapterCallback() {
            @Override
            public void onSuccess(Chapter chapter) {
                currentChapter = chapter;
                populateDisplay(chapter);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateDisplay(Chapter chapter) {
        tvTitle.setText(chapter.getTitle());
        tvNotesAbove.setText(chapter.getNotes_above());
        tvContent.setText(chapter.getContent());
        tvNotesBelow.setText(chapter.getNotes_below());
    }

    private void enableEditing(boolean enable) {
        if (enable) {
            // Show edit fields, hide display fields
            tvTitle.setVisibility(View.GONE);
            tvNotesAbove.setVisibility(View.GONE);
            tvContent.setVisibility(View.GONE);
            tvNotesBelow.setVisibility(View.GONE);

            etTitle.setVisibility(View.VISIBLE);
            etNotesAbove.setVisibility(View.VISIBLE);
            etContent.setVisibility(View.VISIBLE);
            etNotesBelow.setVisibility(View.VISIBLE);

            etTitle.setText(currentChapter.getTitle());
            etNotesAbove.setText(currentChapter.getNotes_above());
            etContent.setText(currentChapter.getContent());
            etNotesBelow.setText(currentChapter.getNotes_below());

            btnEdit.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setVisibility(View.VISIBLE);
            tvNotesAbove.setVisibility(View.VISIBLE);
            tvContent.setVisibility(View.VISIBLE);
            tvNotesBelow.setVisibility(View.VISIBLE);

            etTitle.setVisibility(View.GONE);
            etNotesAbove.setVisibility(View.GONE);
            etContent.setVisibility(View.GONE);
            etNotesBelow.setVisibility(View.GONE);

            btnEdit.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.GONE);
        }
    }

    private void saveChanges() {
        String newTitle = etTitle.getText().toString().trim();
        String newContent = etContent.getText().toString().trim();
        String newNotesAbove = etNotesAbove.getText().toString().trim();
        String newNotesBelow = etNotesBelow.getText().toString().trim();

        if (newContent.isEmpty()) {
            Toast.makeText(getContext(), "Content cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = sessionManager.getAccessToken();
        authHelper.updateChapter(token, chapterId, newTitle, newContent, null,
                newNotesAbove, newNotesBelow, new SupabaseAuthHelper.AuthCallback() {
                    @Override
                    public void onSuccess(String a, String b, String c, String d) {
                        // Update local chapter
                        currentChapter = new Chapter(); // update fields
                        currentChapter.setTitle(newTitle);
                        currentChapter.setContent(newContent);
                        currentChapter.setNotes_above(newNotesAbove);
                        currentChapter.setNotes_below(newNotesBelow);
                        populateDisplay(currentChapter);
                        enableEditing(false);
                        Toast.makeText(getContext(), "Saved", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}