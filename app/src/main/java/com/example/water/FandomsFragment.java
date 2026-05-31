package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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

    private CheckBox cbShowAll;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fandoms_list, container, false);
        rvFandoms = view.findViewById(R.id.rvFandoms);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        cbShowAll = view.findViewById(R.id.cbShowAll);

        cbShowAll.setOnCheckedChangeListener((buttonView, isChecked) -> loadFandoms(isChecked));
        rvFandoms.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FandomAdapter(new ArrayList<>(), fandom -> {
            // Open fandom books page
            FandomBooksFragment booksFrag = FandomBooksFragment.newInstance(fandom.getName());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, booksFrag)
                    .addToBackStack("fandom_books")
                    .commit();
        });
        rvFandoms.setAdapter(adapter);

        loadFandoms(false);
        return view;
    }

    private void loadFandoms(boolean showAll) {
        String token = sessionManager.getAccessToken();
        authHelper.fetchFandomStats(token, showAll, new SupabaseAuthHelper.FandomStatsCallback() {
            @Override public void onSuccess(List<FandomStat> stats) {
                adapter.updateList(stats);
            }
            @Override public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load fandoms", Toast.LENGTH_SHORT).show();
            }
        });
    }
}