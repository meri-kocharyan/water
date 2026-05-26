package com.example.water;

public class Comment {
    private String id;
    private String chapter_id;
    private String user_id;
    private String text;
    private String created_at;
    // optional: we can also store the commenter's username (we'll do a join later)
    private String username;  // we'll fill this from a joined view

    public String getId() { return id; }
    public String getChapter_id() { return chapter_id; }
    public String getUser_id() { return user_id; }
    public String getText() { return text; }
    public String getCreated_at() { return created_at; }
    public String getUsername() { return username; }
}