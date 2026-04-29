package com.example.water;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {

    private List<Book> books;
    private OnBookClickListener listener;

    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    public BookAdapter(List<Book> books, OnBookClickListener listener) {
        this.books = books;
        this.listener = listener;
    }

    public void updateList(List<Book> newList) {
        this.books = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Book book = books.get(position);
        holder.tvTitle.setText(book.getTitle());

        // Extract fandom from tags (take first "Fandom:" tag)
        String fandom = "";
        if (book.getTags() != null) {
            for (String tag : book.getTags()) {
                if (tag.startsWith("Fandom:")) {
                    fandom = tag.substring(7); // remove "Fandom:"
                    break;
                }
            }
        }
        holder.tvFandom.setText(fandom);


        String summary = book.getDescription();
        if (summary == null || summary.trim().isEmpty()) {
            summary = "No summary";
        }
        holder.tvSummary.setText(summary);

        // Warning: we'll show the first "Warning:" tag, or leave empty if none
        String warning = "";
        StringBuilder otherTags = new StringBuilder();
        if (book.getTags() != null) {
            for (String tag : book.getTags()) {
                if (tag.startsWith("Warning:")) {
                    if (warning.isEmpty()) {
                        warning = tag.substring(8);
                    }
                } else if (!tag.startsWith("Language:") && !tag.startsWith("Rating:") && !tag.startsWith("Fandom:")) {
                    if (otherTags.length() > 0) otherTags.append(", ");
                    otherTags.append(tag);
                }
            }
        }
        holder.tvWarning.setText(warning);
        holder.tvOtherTags.setText(otherTags.toString());

        // Language from tags
        String language = "";
        if (book.getTags() != null) {
            for (String tag : book.getTags()) {
                if (tag.startsWith("Language:")) {
                    language = tag.substring(9);
                    break;
                }
            }
        }

        String rating = "";
        if (book.getTags() != null) {
            for (String tag : book.getTags()) {
                if (tag.startsWith("Rating:")) {
                    rating = tag.substring(7);
                    break;
                }
            }
        }

        // Meta: language, words, chapters
        String meta = language;

        if (!rating.isEmpty()) {
            if (!meta.isEmpty()) meta += " | ";
            meta += "Rating: " + rating;
        }


        if (!meta.isEmpty()) meta += " | ";
        meta += book.getWord_count() + " words";
        meta += " | " + book.getChapter_count() + " chapters";
        holder.tvMeta.setText(meta);

        // Show divider for all but last item
        //holder.divider.setVisibility(position == books.size() - 1 ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onBookClick(book);
        });
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvFandom, tvSummary, tvWarning, tvOtherTags, tvMeta;
        View divider;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvBookTitle);
            tvFandom = itemView.findViewById(R.id.tvFandom);
            tvSummary = itemView.findViewById(R.id.tvSummary);
            tvWarning = itemView.findViewById(R.id.tvWarning);
            tvOtherTags = itemView.findViewById(R.id.tvOtherTags);
            tvMeta = itemView.findViewById(R.id.tvMeta);
        }
    }
}