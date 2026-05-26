package com.example.water;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    public void updateList(List<Notification> newList) {
        this.notifications = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notif = notifications.get(position);
        holder.tvMessage.setText(notif.getMessage());

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(notif.getCreated_at().replace("Z", ""));
            SimpleDateFormat out = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            holder.tvTime.setText(out.format(date));
        } catch (Exception e) {
            holder.tvTime.setText("");
        }

        // Highlight if unread
        if (!notif.isIs_read()) {
            holder.tvMessage.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
            holder.itemView.setBackgroundColor(0x22000000); // light grey
        } else {
            holder.tvMessage.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.darker_gray));
            holder.itemView.setBackgroundColor(0x00000000);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(notif);
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        ViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
        }
    }
}
