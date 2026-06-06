package com.example.water;

import android.graphics.Typeface;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    /** Converts "2018-09-02T00:00:00Z" (or similar) → "02 Sep 2018" */
    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat input = new SimpleDateFormat(pattern, Locale.getDefault());
                Date date = input.parse(raw);
                if (date != null) {
                    return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date);
                }
            } catch (ParseException ignored) {}
        }
        return raw; // return as-is if nothing matched
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Book book = books.get(position);

        // --- Parse tags ---
        String fandom = "";
        StringBuilder warnings     = new StringBuilder();
        StringBuilder relationships = new StringBuilder();
        StringBuilder characters   = new StringBuilder();
        StringBuilder freeforms    = new StringBuilder();
        String language = "";
        String rating   = "";

        if (book.getTags() != null) {
            for (String tag : book.getTags()) {
                if (tag.startsWith("Fandom:")) {
                    fandom = tag.substring(7).trim();
                } else if (tag.startsWith("Warning:")) {
                    if (warnings.length() > 0) warnings.append(", ");
                    warnings.append(tag.substring(8).trim());
                } else if (tag.startsWith("Relationship:")) {
                    if (relationships.length() > 0) relationships.append(", ");
                    relationships.append(tag.substring(13).trim());
                } else if (tag.startsWith("Character:")) {
                    if (characters.length() > 0) characters.append(", ");
                    characters.append(tag.substring(10).trim());
                } else if (tag.startsWith("Language:")) {
                    language = tag.substring(9).trim();
                } else if (tag.startsWith("Rating:")) {
                    rating = tag.substring(7).trim();
                } else {
                    // Freeform tag
                    if (freeforms.length() > 0) freeforms.append(", ");
                    freeforms.append(tag);
                }
            }
        }

        // --- Row 1: "Title by Author" — title in blue, "by Author" in black ---
        String author;
        if (book.isIs_anonymous()) {
            author = "Anonymous";
        } else {
            author = book.getAuthor_username() != null ? book.getAuthor_username() : "Unknown";
        }

        String titlePart = book.getTitle();
        String authorPart = " by " + author;
        SpannableString titleSpan = new SpannableString(titlePart + authorPart);
        titleSpan.setSpan(
                new ForegroundColorSpan(holder.tvTitle.getContext().getColor(R.color.ao3_title)),
                0, titlePart.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        titleSpan.setSpan(
                new ForegroundColorSpan(0xFF000000), // black
                titlePart.length(), titlePart.length() + authorPart.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // If anonymous, italicise the author part
        if (book.isIs_anonymous()) {
            holder.tvTitle.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        } else {
            holder.tvTitle.setTypeface(Typeface.DEFAULT);
        }

        holder.tvTitle.setText(titleSpan);
        holder.tvFandom.setText(fandom);

        // Date posted (top-right)
        String datePosted = formatDate(book.getDatePosted());
        if (!datePosted.isEmpty()) {
            holder.tvDate.setText(datePosted);
            holder.tvDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDate.setVisibility(View.GONE);
        }

        // --- Tags block ---
        // Warnings shown bold+red; hide view if empty
        String warnText = warnings.toString();
        if (!warnText.isEmpty()) {
            holder.tvWarnings.setText(warnText);
            holder.tvWarnings.setVisibility(View.VISIBLE);
        } else {
            holder.tvWarnings.setVisibility(View.GONE);
        }

        String relText = relationships.toString();
        if (!relText.isEmpty()) {
            holder.tvRelationships.setText(relText);
            holder.tvRelationships.setVisibility(View.VISIBLE);
        } else {
            holder.tvRelationships.setVisibility(View.GONE);
        }

        String charText = characters.toString();
        if (!charText.isEmpty()) {
            holder.tvCharacters.setText(charText);
            holder.tvCharacters.setVisibility(View.VISIBLE);
        } else {
            holder.tvCharacters.setVisibility(View.GONE);
        }

        String freeText = freeforms.toString();
        if (!freeText.isEmpty()) {
            holder.tvFreeforms.setText(freeText);
            holder.tvFreeforms.setVisibility(View.VISIBLE);
        } else {
            holder.tvFreeforms.setVisibility(View.GONE);
        }

        // --- Summary ---
        String summary = book.getDescription();
        if (summary != null && !summary.isEmpty()) {
            Spanned stripped = Html.fromHtml(summary, Html.FROM_HTML_MODE_LEGACY);
            holder.tvSummary.setText(stripped.toString().trim());
        } else {
            holder.tvSummary.setText("");
        }

        // --- Stats line: Language: X  Words: X  Chapters: X/X  Kudos: X  Hits: X ---
        StringBuilder stats = new StringBuilder();
        if (!language.isEmpty()) stats.append("Language: ").append(language).append("  ");
        if (!rating.isEmpty())   stats.append("Rating: ").append(rating).append("  ");

        int words    = book.getWord_count();
        int chapters = book.getChapter_count();
        stats.append("Words: ").append(words);
        stats.append("  Chapters: ").append(chapters);

        // Optional extra stats — add getters to Book if you have them
        // int kudos = book.getKudos();
        // int hits  = book.getHits();
        // if (kudos > 0) stats.append("  Kudos: ").append(kudos);
        // if (hits  > 0) stats.append("  Hits: ").append(hits);

        holder.tvStats.setText(stats.toString().trim());

        // --- Last Updated (bottom of card) ---
        String updated = formatDate(book.getLastUpdated());
        if (!updated.isEmpty()) {
            holder.tvUpdated.setText("Updated: " + updated);
            holder.tvUpdated.setVisibility(View.VISIBLE);
        } else {
            holder.tvUpdated.setVisibility(View.GONE);
        }

        // --- Click listener ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onBookClick(book);
        });
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvFandom, tvDate,
                tvWarnings, tvRelationships, tvCharacters, tvFreeforms,
                tvSummary, tvStats, tvUpdated;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle         = itemView.findViewById(R.id.tvBookTitle);
            tvFandom        = itemView.findViewById(R.id.tvFandom);
            tvDate          = itemView.findViewById(R.id.tvDate);
            tvWarnings      = itemView.findViewById(R.id.tvWarnings);
            tvRelationships = itemView.findViewById(R.id.tvRelationships);
            tvCharacters    = itemView.findViewById(R.id.tvCharacters);
            tvFreeforms     = itemView.findViewById(R.id.tvFreeforms);
            tvSummary       = itemView.findViewById(R.id.tvSummary);
            tvStats         = itemView.findViewById(R.id.tvStats);
            tvUpdated       = itemView.findViewById(R.id.tvUpdated);
        }
    }
}