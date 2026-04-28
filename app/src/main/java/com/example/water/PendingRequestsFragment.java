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

public class PendingRequestsFragment extends Fragment {

    private RecyclerView recyclerView;
    private PendingRequestsAdapter adapter;
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pending_requests, container, false);

        recyclerView = view.findViewById(R.id.rvPendingRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Update the pending requests layout to include RecyclerView
        // We'll change fragment_pending_requests.xml to just a RecyclerView
        // Actually, we'll update the layout file to have a RecyclerView with id rvPendingRequests.

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        adapter = new PendingRequestsAdapter(new ArrayList<>(), new PendingRequestsAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(FriendRequest req) {
                respond(req.getId(), "accepted");
            }

            @Override
            public void onReject(FriendRequest req) {
                respond(req.getId(), "rejected");
            }
        });
        recyclerView.setAdapter(adapter);

        loadPendingRequests();

        return view;
    }

    private void loadPendingRequests() {
        String token = sessionManager.getAccessToken();
        String userId = sessionManager.getUserId();
        if (token == null || userId == null) return;
        authHelper.fetchPendingRequests(token, userId, new SupabaseAuthHelper.FriendRequestCallback() {
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

    private void respond(String requestId, String newStatus) {
        String token = sessionManager.getAccessToken();
        authHelper.respondToRequest(token, requestId, newStatus, new SupabaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String a, String b, String c, String d) {
                Toast.makeText(getContext(), "Request " + newStatus, Toast.LENGTH_SHORT).show();
                loadPendingRequests(); // refresh
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
