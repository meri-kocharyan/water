package com.example.water;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FandomAdapter extends RecyclerView.Adapter<FandomAdapter.ViewHolder> {
    private List<String> fandoms;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String fandom);
    }

    public FandomAdapter(List<String> fandoms, OnItemClickListener listener) {
        this.fandoms = fandoms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(v);
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String fandom = fandoms.get(position);
        holder.textView.setText(fandom);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(fandom));
    }

    @Override public int getItemCount() { return fandoms.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}