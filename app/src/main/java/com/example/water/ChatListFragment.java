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

public class ChatListFragment extends Fragment {

    private RecyclerView rvFriends;
    private FriendsAdapter adapter;
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_lists, container, false);
        // We'll change fragment_chat_list.xml to contain a RecyclerView
        rvFriends = view.findViewById(R.id.rvFriends);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FriendsAdapter(new ArrayList<>(), sessionManager.getUserId(), req -> {
            String otherId, otherEmail;
            if (req.getSender_id().equals(sessionManager.getUserId())) {
                otherId = req.getReceiver_id();
                otherEmail = req.getReceiver_email();
            } else {
                otherId = req.getSender_id();
                otherEmail = req.getSender_email();
            }
            // Load the chat detail fragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, ChatDetailFragment.newInstance(otherId, otherEmail))
                    .addToBackStack("chat_detail")
                    .commit();
        });

        rvFriends.setAdapter(adapter);

        loadFriends();

        return view;
    }

    private void loadFriends() {
        String token = sessionManager.getAccessToken();
        String userId = sessionManager.getUserId();
        if (token == null || userId == null) return;
        authHelper.fetchFriendsWithLastMessage(token, userId, new SupabaseAuthHelper.FriendRequestCallback() {
            @Override
            public void onSuccess(List<FriendRequest> requests) {
                adapter.updateList(requests);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
