package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.water.supabase.SupabaseAuthHelper;

import java.util.ArrayList;
import java.util.List;

public class FandomsFragment extends Fragment {

    private RecyclerView rvFandoms;
    private FandomAdapter adapter;
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fandoms_list, container, false);
        rvFandoms = view.findViewById(R.id.rvFandoms);
        rvFandoms.setLayoutManager(new LinearLayoutManager(getContext()));

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        // Create adapter with click listener
        adapter = new FandomAdapter(new ArrayList<>(), fandom -> {
            FandomBooksFragment booksFrag = FandomBooksFragment.newInstance(fandom);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, booksFrag)
                    .addToBackStack("fandom_books")
                    .commit();
        });
        rvFandoms.setAdapter(adapter);

        // Load fandoms from database
        authHelper.fetchFandoms(sessionManager.getAccessToken(), new SupabaseAuthHelper.TagsCallback() {
            @Override
            public void onSuccess(List<String> names) {
                adapter.setData(names);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}