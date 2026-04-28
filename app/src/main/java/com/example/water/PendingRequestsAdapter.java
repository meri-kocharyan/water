package com.example.water;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PendingRequestsAdapter extends RecyclerView.Adapter<PendingRequestsAdapter.ViewHolder> {

    private List<FriendRequest> requestList;
    private OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onAccept(FriendRequest request);
        void onReject(FriendRequest request);
    }

    public PendingRequestsAdapter(List<FriendRequest> requests, OnRequestActionListener listener) {
        this.requestList = requests;
        this.listener = listener;
    }

    public void updateList(List<FriendRequest> newList) {
        this.requestList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRequest req = requestList.get(position);
        holder.tvEmail.setText(req.getSender_email());
        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(req);
        });
        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(req);
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmail;
        Button btnAccept, btnReject;

        ViewHolder(View itemView) {
            super(itemView);
            tvEmail = itemView.findViewById(R.id.tvSenderEmail);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}