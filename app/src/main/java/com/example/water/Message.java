package com.example.water;

public class Message {
    private String id;
    private String sender_id;
    private String receiver_id;
    private String content;
    private String created_at;

    public String getId() { return id; }
    public String getSender_id() { return sender_id; }
    public String getReceiver_id() { return receiver_id; }
    public String getContent() { return content; }
    public String getCreated_at() { return created_at; }
}