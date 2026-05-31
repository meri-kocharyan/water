package com.example.water;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FandomAdapter extends RecyclerView.Adapter<FandomAdapter.ViewHolder> {

    private List<FandomStat> fandomList;
    private OnFandomClickListener listener;

    public interface OnFandomClickListener {
        void onFandomClick(FandomStat fandom);
    }

    public FandomAdapter(List<FandomStat> fandomList, OnFandomClickListener listener) {
        this.fandomList = fandomList;
        this.listener = listener;
    }

    public void updateList(List<FandomStat> newList) {
        this.fandomList = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fandom, parent, false);
        return new ViewHolder(v);
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FandomStat fandom = fandomList.get(position);
        holder.tvName.setText(fandom.getName());
        holder.tvCount.setText("(" + fandom.getBook_count() + ")");
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFandomClick(fandom);
        });
    }

    @Override public int getItemCount() { return fandomList.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCount;
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFandomName);
            tvCount = itemView.findViewById(R.id.tvFandomCount);
        }
    }
}