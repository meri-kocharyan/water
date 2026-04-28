package com.example.water;

public class FriendRequest {
    private String id;
    private String sender_id;
    private String receiver_id;
    private String sender_email;
    private String receiver_email;
    private String status;

    // Getters
    public String getId() { return id; }
    public String getSender_id() { return sender_id; }
    public String getReceiver_id() { return receiver_id; }
    public String getSender_email() { return sender_email; }
    public String getReceiver_email() { return receiver_email; }
    public String getStatus() { return status; }




    private String other_user_id;
    private String other_user_email;
    private String last_message;
    private String last_message_time;

    // getters (no setters needed for existing code)
    public String getOther_user_id() { return other_user_id; }
    public String getOther_user_email() { return other_user_email; }
    public String getLast_message() { return last_message; }
    public String getLast_message_time() { return last_message_time; }
}
