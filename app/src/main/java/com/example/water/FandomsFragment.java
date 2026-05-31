package com.example.water;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.water.supabase.SupabaseAuthHelper;

import java.util.List;

public class FandomsFragment extends Fragment {
    private RecyclerView rvFandoms;
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

        authHelper.fetchFandoms(sessionManager.getAccessToken(), new SupabaseAuthHelper.TagsCallback() {
            @Override public void onSuccess(List<String> names) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_list_item_1, names);
                rvFandoms.setAdapter(adapter);
            }
            @Override public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // Click on a fandom → open FandomBooksFragment
        rvFandoms.addOnItemTouchListener(new RecyclerItemClickListener(getContext(), (view1, position) -> {
            String fandom = names.get(position);
            FandomBooksFragment booksFrag = FandomBooksFragment.newInstance(fandom);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, booksFrag)
                    .addToBackStack("fandom_books")
                    .commit();
        }));

        return view;
    }
}
