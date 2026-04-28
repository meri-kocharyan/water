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

public class ChaptersListChildFragment extends Fragment {

    private static final String ARG_BOOK_ID = "book_id";
    private String bookId;

    private RecyclerView rvChapters;
    private Button btnAddChapter;
    private ChapterAdapter adapter;
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    public static ChaptersListChildFragment newInstance(String bookId) {
        ChaptersListChildFragment frag = new ChaptersListChildFragment();
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
        View view = inflater.inflate(R.layout.fragment_chapters_list_child, container, false);

        rvChapters = view.findViewById(R.id.rvChapters);
        btnAddChapter = view.findViewById(R.id.btnAddChapter);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        rvChapters.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ChapterAdapter(new ArrayList<>(), chapter -> {
            ChapterViewFragment viewer = ChapterViewFragment.newInstance(chapter.getId());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, viewer)
                    .addToBackStack("chapter_view")
                    .commit();
        });
        rvChapters.setAdapter(adapter);

        btnAddChapter.setOnClickListener(v -> {


            int nextNumber = adapter.getItemCount() + 1;
            Bundle args = new Bundle();
            args.putString("book_id", bookId);
            args.putInt("chapter_number", nextNumber);
            CreateChapterFragment createFrag = new CreateChapterFragment();
            createFrag.setArguments(args);

            args.putString("book_id", bookId); // new parameter
            createFrag.setArguments(args);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, createFrag)
                    .addToBackStack("create_chapter")
                    .commit();
        });

        loadChapters();

        return view;
    }

    private void loadChapters() {
        String token = sessionManager.getAccessToken();
        authHelper.fetchChaptersByBookId(token, bookId, new SupabaseAuthHelper.ChaptersCallback() {
            @Override
            public void onSuccess(List<Chapter> chapters) {
                adapter.updateList(chapters);
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
        loadChapters(); // refresh when coming back
    }
}
