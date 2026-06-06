package com.example.water;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

public class ChapterViewFragment extends Fragment {

    private static final String ARG_CHAPTER_ID = "chapter_id";
    private String chapterId;

    private TextView tvTitle, tvNotesAbove, tvContent, tvNotesBelow;
    private EditText etTitle, etNotesAbove, etContent, etNotesBelow;
    private Button btnEdit, btnSave, btnPrev, btnNext, btnBackToBook;
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;
    private Chapter currentChapter;
    private boolean isAuthor = false;
    private boolean commentsDisabled = false;   // <-- NEW

    private RecyclerView rvComments;
    private EditText etCommentInput;
    private ImageButton btnSendComment;
    private LinearLayout postCommentArea;
    private CommentAdapter commentAdapter;
    private TextView tvCommentsDisabled;   // <-- NEW

    private LinearLayout notesAboveContainer, notesBelowContainer;

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
        btnPrev = view.findViewById(R.id.btnPrev);
        btnNext = view.findViewById(R.id.btnNext);
        btnBackToBook = view.findViewById(R.id.btnBackToBook);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        // Initially hide edit controls – will be shown later if author
        btnEdit.setVisibility(View.INVISIBLE);
        btnSave.setVisibility(View.INVISIBLE);

        btnEdit.setOnClickListener(v -> enableEditing(true));
        btnSave.setOnClickListener(v -> saveChanges());
        btnPrev.setOnClickListener(v -> navigateChapter(false));
        btnNext.setOnClickListener(v -> navigateChapter(true));
        btnBackToBook.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack("book_detail", 0);
        });

        // Comments section
        rvComments = view.findViewById(R.id.rvComments);
        etCommentInput = view.findViewById(R.id.etCommentInput);
        btnSendComment = view.findViewById(R.id.btnSendComment);
        postCommentArea = view.findViewById(R.id.postCommentArea);
        tvCommentsDisabled = view.findViewById(R.id.tvCommentsDisabled);   // <-- NEW

        notesAboveContainer = view.findViewById(R.id.notesAboveContainer);
        notesBelowContainer = view.findViewById(R.id.notesBelowContainer);

        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        commentAdapter = new CommentAdapter(new ArrayList<>());
        rvComments.setAdapter(commentAdapter);

        // Initially hide the post area; we'll show it after we know if comments are allowed
        postCommentArea.setVisibility(View.GONE);

        btnSendComment.setOnClickListener(v -> postComment());

        // Load comments immediately
        loadComments();

        // Fetch chapter data and check ownership/comments flag
        fetchChapterAndCheckOwnership();

        return view;
    }

    private void fetchChapterAndCheckOwnership() {
        String token = sessionManager.getAccessToken();
        authHelper.fetchChapterById(token, chapterId, new SupabaseAuthHelper.ChapterCallback() {
            @Override
            public void onSuccess(Chapter chapter) {
                currentChapter = chapter;
                populateDisplay(chapter);

                // Check book ownership and comment settings
                authHelper.fetchBookById(token, chapter.getBook_id(), new SupabaseAuthHelper.BookCallback() {
                    @Override
                    public void onSuccess(Book book) {
                        String myUserId = sessionManager.getUserId();
                        isAuthor = (myUserId != null && myUserId.equals(book.getAuthor_id()));

                        // Show edit buttons if author
                        btnEdit.setVisibility(isAuthor ? View.VISIBLE : View.GONE);

                        // Handle comments disabled flag
                        commentsDisabled = book.isComments_disabled();
                        if (commentsDisabled) {
                            tvCommentsDisabled.setVisibility(View.VISIBLE);
                            postCommentArea.setVisibility(View.GONE);
                        } else {
                            tvCommentsDisabled.setVisibility(View.GONE);
                            postCommentArea.setVisibility(
                                    sessionManager.isLoggedIn() ? View.VISIBLE : View.GONE);
                        }

                        updateNavigationButtons(book.getChapter_count());
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), "Failed to verify ownership", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateDisplay(Chapter chapter) {
        tvTitle.setText(chapter.getTitle());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvContent.setText(Html.fromHtml(chapter.getContent(), Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvContent.setText(Html.fromHtml(chapter.getContent()));
        }

        // Notes above
        String above = chapter.getNotes_above();
        if (above != null && !above.isEmpty()) {
            tvNotesAbove.setText(above);
            notesAboveContainer.setVisibility(View.VISIBLE);
        } else {
            notesAboveContainer.setVisibility(View.GONE);
        }

        // Notes below
        String below = chapter.getNotes_below();
        if (below != null && !below.isEmpty()) {
            tvNotesBelow.setText(below);
            notesBelowContainer.setVisibility(View.VISIBLE);
        } else {
            notesBelowContainer.setVisibility(View.GONE);
        }
    }

    private void enableEditing(boolean enable) {
        if (!isAuthor) return;

        if (enable) {
            // Hide read-mode views
            tvTitle.setVisibility(View.GONE);
            tvContent.setVisibility(View.GONE);

            // Hide notes containers (read mode)
            notesAboveContainer.setVisibility(View.GONE);
            notesBelowContainer.setVisibility(View.GONE);

            // Show edit-mode views
            etTitle.setVisibility(View.VISIBLE);
            etContent.setVisibility(View.VISIBLE);

            // Show notes edit fields
            etNotesAbove.setVisibility(View.VISIBLE);
            etNotesBelow.setVisibility(View.VISIBLE);

            // Pre‑fill edit fields
            etTitle.setText(currentChapter.getTitle());
            etContent.setText(currentChapter.getContent());
            etNotesAbove.setText(currentChapter.getNotes_above());
            etNotesBelow.setText(currentChapter.getNotes_below());

            btnEdit.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
        } else {
            // Show read-mode views
            tvTitle.setVisibility(View.VISIBLE);
            tvContent.setVisibility(View.VISIBLE);

            // Show notes containers (read mode) only if they have content
            String above = currentChapter.getNotes_above();
            notesAboveContainer.setVisibility(
                    (above != null && !above.isEmpty()) ? View.VISIBLE : View.GONE);
            String below = currentChapter.getNotes_below();
            notesBelowContainer.setVisibility(
                    (below != null && !below.isEmpty()) ? View.VISIBLE : View.GONE);

            // Hide edit-mode views
            etTitle.setVisibility(View.GONE);
            etContent.setVisibility(View.GONE);
            etNotesAbove.setVisibility(View.GONE);
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

    private void navigateChapter(boolean next) {
        if (currentChapter == null) return;
        final int currentNumber = currentChapter.getChapter_number();
        final String bookId = currentChapter.getBook_id();
        final int direction = next ? 1 : -1;

        String token = sessionManager.getAccessToken();
        authHelper.fetchChaptersByBookId(token, bookId, new SupabaseAuthHelper.ChaptersCallback() {
            @Override
            public void onSuccess(java.util.List<Chapter> chapters) {
                Chapter target = null;
                for (Chapter ch : chapters) {
                    if (ch.getChapter_number() == currentNumber + direction) {
                        target = ch;
                        break;
                    }
                }
                if (target != null) {
                    ChapterViewFragment newFrag = ChapterViewFragment.newInstance(target.getId());
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, newFrag)
                            .addToBackStack(null)
                            .commit();
                } else {
                    Toast.makeText(getContext(), next ? "No next chapter" : "No previous chapter", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNavigationButtons(int totalChapters) {
        if (currentChapter == null) return;
        int num = currentChapter.getChapter_number();
        btnPrev.setVisibility(num > 1 ? View.VISIBLE : View.INVISIBLE);
        btnNext.setVisibility(num < totalChapters ? View.VISIBLE : View.INVISIBLE);
    }

    private void loadComments() {
        String token = sessionManager.getAccessToken();
        authHelper.fetchComments(token, chapterId, new SupabaseAuthHelper.CommentsCallback() {
            @Override
            public void onSuccess(List<Comment> comments) {
                commentAdapter.updateList(comments);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load comments", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postComment() {
        if (commentsDisabled) return;   // extra safety

        String text = etCommentInput.getText().toString().trim();
        if (text.isEmpty()) return;
        String token = sessionManager.getAccessToken();
        String userId = sessionManager.getUserId();
        if (token == null || userId == null) return;

        authHelper.postComment(token, chapterId, userId, text, new SupabaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String a, String b, String c, String d) {
                etCommentInput.setText("");
                loadComments();
                notifyAuthorIfNeeded(chapterId);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void notifyAuthorIfNeeded(String chapterId) {
        String token = sessionManager.getAccessToken();
        String myUserId = sessionManager.getUserId();
        if (token == null || myUserId == null) return;

        authHelper.fetchChapterById(token, chapterId, new SupabaseAuthHelper.ChapterCallback() {
            @Override
            public void onSuccess(Chapter chapter) {
                authHelper.fetchBookById(token, chapter.getBook_id(), new SupabaseAuthHelper.BookCallback() {
                    @Override
                    public void onSuccess(Book book) {
                        String authorId = book.getAuthor_id();
                        if (authorId != null && !authorId.equals(myUserId)) {
                            String message = "New comment on \"" + book.getTitle() + "\"";
                            authHelper.createNotification(token, authorId, "comment",
                                    message, chapterId, book.getId(),
                                    new SupabaseAuthHelper.AuthCallback() {
                                        @Override
                                        public void onSuccess(String a, String b, String c, String d) {}
                                        @Override
                                        public void onError(String error) {}
                                    });
                        }
                    }
                    @Override
                    public void onError(String error) {}
                });
            }
            @Override
            public void onError(String error) {}
        });
    }
}