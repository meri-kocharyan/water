package com.example.water;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private List<Message> messages;
    private String myUserId;

    public MessageAdapter(List<Message> messages, String myUserId) {
        this.messages = messages;
        this.myUserId = myUserId;
    }

    public void updateList(List<Message> newList) {
        this.messages = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_with_avatar, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message msg = messages.get(position);
        boolean isMine = msg.getSender_id().equals(myUserId);

        // Format time
        String timeStr = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(msg.getCreated_at().replace("Z", ""));
            SimpleDateFormat outFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeStr = outFormat.format(date);
        } catch (Exception ignored) {}

        if (isMine) {
            // Show right container, hide left
            holder.leftContainer.setVisibility(View.GONE);
            holder.rightContainer.setVisibility(View.VISIBLE);

            holder.tvRightContent.setText(msg.getContent());
            holder.tvRightTime.setText(timeStr);
            // My avatar is on right side – load my profile image (currently default)
            Glide.with(holder.ivLeftAvatar.getContext())
                    .load(R.drawable.ic_default_avatar)   // placeholder for now
                    .circleCrop()
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(holder.ivLeftAvatar);
        } else {
            // Show left container, hide right
            holder.leftContainer.setVisibility(View.VISIBLE);
            holder.rightContainer.setVisibility(View.GONE);

            holder.tvLeftContent.setText(msg.getContent());
            holder.tvLeftTime.setText(timeStr);
            // Friend avatar (use same default for now – you can later load based on friend ID)
            holder.ivLeftAvatar.setImageResource(R.drawable.ic_default_avatar);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout leftContainer, rightContainer;
        TextView tvLeftContent, tvLeftTime, tvRightContent, tvRightTime;
        ImageView ivLeftAvatar, ivRightAvatar;

        ViewHolder(View itemView) {
            super(itemView);
            leftContainer = itemView.findViewById(R.id.leftBubbleContainer);
            rightContainer = itemView.findViewById(R.id.rightBubbleContainer);
            tvLeftContent = itemView.findViewById(R.id.tvLeftContent);
            tvLeftTime = itemView.findViewById(R.id.tvLeftTime);
            tvRightContent = itemView.findViewById(R.id.tvRightContent);
            tvRightTime = itemView.findViewById(R.id.tvRightTime);
            ivLeftAvatar = itemView.findViewById(R.id.ivLeftAvatar);
            ivRightAvatar = itemView.findViewById(R.id.ivRightAvatar);
        }
    }
}