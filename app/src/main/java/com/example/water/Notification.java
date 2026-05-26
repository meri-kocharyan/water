package com.example.water;

public class Notification {
    private String id;
    private String user_id;
    private String type;
    private String message;
    private String related_chapter_id;
    private String related_book_id;
    private boolean is_read;
    private String created_at;

    // getters
    public String getId() { return id; }
    public String getUser_id() { return user_id; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getRelated_chapter_id() { return related_chapter_id; }
    public String getRelated_book_id() { return related_book_id; }
    public boolean isIs_read() { return is_read; }
    public String getCreated_at() { return created_at; }
}
