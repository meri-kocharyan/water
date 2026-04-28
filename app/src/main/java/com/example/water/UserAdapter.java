package com.example.water;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private List<UserProfile> userList;
    private OnAddFriendListener listener;

    public interface OnAddFriendListener {
        void onAddFriend(UserProfile user);
    }

    public UserAdapter(List<UserProfile> users, OnAddFriendListener listener) {
        this.userList = users;
        this.listener = listener;
    }

    public void updateList(List<UserProfile> newList) {
        this.userList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserProfile user = userList.get(position);
        holder.tvEmail.setText(user.getEmail());
        holder.btnAdd.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddFriend(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmail;
        Button btnAdd;

        ViewHolder(View itemView) {
            super(itemView);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            btnAdd = itemView.findViewById(R.id.btnAddFriend);
        }
    }
}
