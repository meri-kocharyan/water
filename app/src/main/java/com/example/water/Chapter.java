package com.example.water;

public class Chapter {
    private String id;
    private String book_id;
    private int chapter_number;
    private String title;
    private String content;
    private String summary;
    private String notes_above;
    private String notes_below;
    private String created_at;

    // Getters
    public String getId() { return id; }
    public String getBook_id() { return book_id; }
    public int getChapter_number() { return chapter_number; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSummary() { return summary; }
    public String getNotes_above() { return notes_above; }
    public String getNotes_below() { return notes_below; }
    public String getCreated_at() { return created_at; }






    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setNotes_above(String notesAbove) { this.notes_above = notesAbove; }
    public void setNotes_below(String notesBelow) { this.notes_below = notesBelow; }
}
