package com.example.water;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.water.supabase.SupabaseAuthHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddFriendFragment extends Fragment {

    private EditText etSearch;
    private RecyclerView rvUsers;
    private UserAdapter adapter;
    private List<UserProfile> allUsers = new ArrayList<>();
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_friend, container, false);

        etSearch = view.findViewById(R.id.etSearchUser);
        rvUsers = view.findViewById(R.id.rvUsers);
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        adapter = new UserAdapter(new ArrayList<>(), user -> {
            // Send friend request
            String myId = sessionManager.getUserId();
            String myEmail = sessionManager.getUserEmail();
            if (myId == null) return;
            if (myId.equals(user.getId())) {
                Toast.makeText(getContext(), "You can't add yourself", Toast.LENGTH_SHORT).show();
                return;
            }
            authHelper.sendFriendRequest(sessionManager.getAccessToken(), myId, myEmail,
                    user.getId(), user.getEmail(), new SupabaseAuthHelper.AuthCallback() {
                        @Override
                        public void onSuccess(String a, String b, String c, String d) {
                            Toast.makeText(getContext(), "Friend request sent to " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                        }
                    });
        });
        rvUsers.setAdapter(adapter);

        // Load all users initially
        fetchUsers("");

        // Search with a short delay after typing stops
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> fetchUsers(s.toString().trim());
                searchHandler.postDelayed(searchRunnable, 300); // 300ms delay
            }
        });

        return view;
    }

    private Set<String> connectedUserIds = new HashSet<>();

    private void fetchConnectedUsers() {
        String token = sessionManager.getAccessToken();
        String myId = sessionManager.getUserId();
        if (token == null || myId == null) return;
        authHelper.fetchConnectedUserIds(token, myId, new SupabaseAuthHelper.FriendIdCallback() {
            @Override
            public void onSuccess(Set<String> ids) {
                connectedUserIds = ids;
                fetchUsers("");   // now reload users (filtered)
            }
            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                fetchUsers("");   // still load even if error, to avoid empty screen
            }
        });
    }

    private void fetchUsers(String query) {
        String token = sessionManager.getAccessToken();
        authHelper.fetchProfiles(token, query, new SupabaseAuthHelper.ProfileFetchCallback() {
            @Override
            public void onSuccess(List<UserProfile> profiles) {
                // Remove profiles whose IDs are in connectedUserIds
                List<UserProfile> filtered = new ArrayList<>();
                for (UserProfile p : profiles) {
                    if (!connectedUserIds.contains(p.getId())) {
                        filtered.add(p);
                    }
                }
                adapter.updateList(filtered);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Error loading users: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
