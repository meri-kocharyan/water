package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
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

public class ChatDetailFragment extends Fragment {

    private static final String ARG_FRIEND_ID = "friend_id";
    private static final String ARG_FRIEND_EMAIL = "friend_email";

    private String friendId, friendEmail;
    private RecyclerView rvMessages;
    private EditText etInput;
    private ImageButton btnSend;
    private MessageAdapter adapter;
    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;
    private TextView tvFriendName;

    public static ChatDetailFragment newInstance(String friendId, String friendEmail) {
        ChatDetailFragment fragment = new ChatDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FRIEND_ID, friendId);
        args.putString(ARG_FRIEND_EMAIL, friendEmail);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            friendId = getArguments().getString(ARG_FRIEND_ID);
            friendEmail = getArguments().getString(ARG_FRIEND_EMAIL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_detail, container, false);

        tvFriendName = view.findViewById(R.id.tvFriendName);
        tvFriendName.setText(friendEmail);

        rvMessages = view.findViewById(R.id.rvMessages);
        rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);   // newest items at the bottom
        rvMessages.setLayoutManager(layoutManager);

        etInput = view.findViewById(R.id.etMessageInput);
        btnSend = view.findViewById(R.id.btnSend);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        adapter = new MessageAdapter(new ArrayList<>(), sessionManager.getUserId());
        rvMessages.setAdapter(adapter);

        loadMessages();

        btnSend.setOnClickListener(v -> sendMessage());

        return view;
    }

    private void loadMessages() {
        String token = sessionManager.getAccessToken();
        String myId = sessionManager.getUserId();
        if (token == null || myId == null) return;
        authHelper.fetchMessages(token, myId, friendId, new SupabaseAuthHelper.MessageCallback() {
            @Override
            public void onSuccess(List<Message> messages) {
                adapter.updateList(messages);
                rvMessages.scrollToPosition(adapter.getItemCount() - 1);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String content = etInput.getText().toString().trim();
        if (content.isEmpty()) return;
        String token = sessionManager.getAccessToken();
        String myId = sessionManager.getUserId();
        if (token == null || myId == null) return;
        authHelper.sendMessage(token, myId, friendId, content, new SupabaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String a, String b, String c, String d) {
                etInput.setText("");
                loadMessages(); // refresh
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}