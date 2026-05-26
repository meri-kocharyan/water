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

public class InboxFragment extends Fragment {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inbox, container, false);

        rvNotifications = view.findViewById(R.id.rvNotifications);
        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(new ArrayList<>(), notif -> {
            // Navigate to the chapter/book if available
            if (notif.getRelated_chapter_id() != null) {
                ChapterViewFragment frag = ChapterViewFragment.newInstance(notif.getRelated_chapter_id());
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, frag)
                        .addToBackStack(null)
                        .commit();
                // Mark as read automatically? We'll mark all read when opening inbox.
            } else if (notif.getRelated_book_id() != null) {
                BookDetailFragment frag = BookDetailFragment.newInstance(notif.getRelated_book_id());
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, frag)
                        .addToBackStack(null)
                        .commit();
            }
        });
        rvNotifications.setAdapter(adapter);

        loadNotifications();
        markAllRead();

        return view;
    }

    private void loadNotifications() {
        String token = sessionManager.getAccessToken();
        String userId = sessionManager.getUserId();
        if (token == null || userId == null) return;
        authHelper.fetchNotifications(token, userId, new SupabaseAuthHelper.NotificationsCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                adapter.updateList(notifications);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAllRead() {
        String token = sessionManager.getAccessToken();
        String userId = sessionManager.getUserId();
        if (token == null || userId == null) return;
        authHelper.markAllNotificationsRead(token, userId, new SupabaseAuthHelper.AuthCallback() {
            @Override public void onSuccess(String a, String b, String c, String d) {}
            @Override public void onError(String error) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
        markAllRead();
    }
}