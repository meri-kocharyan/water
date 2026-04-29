package com.example.water;

import java.util.List;

public class Book {
    private String id;
    private String author_id;
    private String title;
    private String description;
    private List<String> tags;
    private String content;
    private String created_at;
    private String updated_at;
    // optional: author username (we'll fetch separately later)

    // getters
    public String getId() { return id; }
    public String getAuthor_id() { return author_id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getTags() { return tags; }
    public String getContent() { return content; }
    public String getCreated_at() { return created_at; }
    public String getUpdated_at() { return updated_at; }





    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setTags(List<String> tags) { this.tags = tags; }






    private int chapter_count;
    private int word_count;

    public int getChapter_count() { return chapter_count; }
    public int getWord_count() { return word_count; }





    private String author_username;

    public String getAuthor_username() { return author_username; }
}