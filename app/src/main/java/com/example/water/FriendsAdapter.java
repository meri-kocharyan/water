package com.example.water;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private List<FriendRequest> friends;
    private String myUserId;
    private OnFriendClickListener listener;

    public interface OnFriendClickListener {
        void onFriendClick(FriendRequest request);
    }

    public FriendsAdapter(List<FriendRequest> friends, String myUserId, OnFriendClickListener listener) {
        this.friends = friends;
        this.myUserId = myUserId;
        this.listener = listener;
    }

    public void updateList(List<FriendRequest> newList) {
        this.friends = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRequest req = friends.get(position);
        String displayEmail;
        if (req.getSender_id().equals(myUserId)) {
            displayEmail = req.getReceiver_email();
        } else {
            displayEmail = req.getSender_email();
        }
        holder.tvName.setText(displayEmail);
        holder.tvLastMessage.setText(""); // placeholder – we'll add later
        // For the avatar, you can later load a real URL with Glide/Picasso
        holder.ivAvatar.setImageResource(R.drawable.ic_default_avatar);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFriendClick(req);
        });


        String lastMsg = req.getLast_message();
        if (lastMsg != null && !lastMsg.isEmpty()) {
            holder.tvLastMessage.setText(lastMsg);
        } else {
            holder.tvLastMessage.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvLastMessage;

        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivFriendAvatar);
            tvName = itemView.findViewById(R.id.tvFriendName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
        }
    }
}