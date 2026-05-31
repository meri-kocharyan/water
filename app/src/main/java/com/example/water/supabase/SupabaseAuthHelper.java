package com.example.water.supabase;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.water.Book;
import com.example.water.Chapter;
import com.example.water.Comment;
import com.example.water.FandomStat;
import com.example.water.FriendRequest;
import com.example.water.Message;
import com.example.water.Notification;
import com.example.water.UserProfile;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseAuthHelper {
    private static final String SUPABASE_URL = "https://tbspnnujtxombnenshmj.supabase.co";
    private static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRic3BubnVqdHhvbWJuZW5zaG1qIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzcyOTAwMTEsImV4cCI6MjA5Mjg2NjAxMX0.zn94JQmcinDhxS3t683FTmiiQeVSNhKP0kY950Yy7RM";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;

    public void fetchProfiles(String token, String query, ProfileFetchCallback profileFetchCallback) {
    }

    public interface AuthCallback {
        void onSuccess(String accessToken, String refreshToken, String email, String userId);
        void onError(String error);
    }

    public SupabaseAuthHelper() {
        this.client = new OkHttpClient();
    }

    public void signUp(String email, String password, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/signup")
                .header("apikey", ANON_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "no body";
                    Log.e("FETCH_PROFILES", "HTTP " + response.code() + ": " + errorBody);
                    notifyErrorCallback((ProfileFetchCallback) callback, "Failed to fetch users");
                    return;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d("AUTH_RESPONSE", responseBody);
                if (response.isSuccessful()) {
                    try {
                        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                        String accessToken = json.get("access_token").getAsString();
                        String refreshToken = json.get("refresh_token").getAsString();
                        String userEmail = json.getAsJsonObject("user").get("email").getAsString();
                        String userId = json.getAsJsonObject("user").get("id").getAsString();
                        notifySuccess(callback, accessToken, refreshToken, userEmail, userId);
                    } catch (Exception e) {
                        notifyError(callback, "Failed to parse response");
                    }
                } else {
                    notifyError(callback, "Sign up failed: " + responseBody);
                }
            }
        });
    }

    public void signIn(String email, String password, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/token?grant_type=password")
                .header("apikey", ANON_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d("AUTH_RESPONSE", responseBody);
                if (response.isSuccessful()) {
                    try {
                        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                        String accessToken = json.get("access_token").getAsString();
                        String refreshToken = json.get("refresh_token").getAsString();
                        String userEmail = json.getAsJsonObject("user").get("email").getAsString();
                        String userId = json.getAsJsonObject("user").get("id").getAsString();
                        notifySuccess(callback, accessToken, refreshToken, userEmail, userId);
                    } catch (Exception e) {
                        notifyError(callback, "Failed to parse response");
                    }
                } else {
                    notifyError(callback, "Sign in failed (HTTP " + response.code() + "): " + responseBody);
                }
            }
        });
    }

    public void logout(String accessToken, AuthCallback callback, String userId) {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/logout")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create("", JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccess(callback, null, null, null, userId); // tokens not needed
                } else {
                    notifyError(callback, "Logout failed");
                }
            }
        });
    }

    private void notifySuccess(AuthCallback callback, String accessToken, String refreshToken, String email, String userId) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(accessToken, refreshToken, email, userId));
    }

    private void notifyError(AuthCallback callback, String error) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onError(error));
    }

    public void createProfile(String userId, String email, String accessToken, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("id", userId);         // the auth uid
        body.addProperty("user_id", userId);
        body.addProperty("email", email);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/profiles")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    notifyError(callback, "Profile creation failed: " + response.message());
                }
            }
        });
    }


    public interface ProfileFetchCallback {
        void onSuccess(List<UserProfile> profiles);
        void onError(String error);
    }

    public void fetchProfiles(String accessToken, String searchQuery, String excludeId, ProfileFetchCallback callback, String userId) {
        String url = SUPABASE_URL + "/rest/v1/friend_requests?select=sender_id,receiver_id,status" +
                "&or=(sender_id.eq." + userId + ",receiver_id.eq." + userId + ")" +
                "&status=neq.rejected";
        if (searchQuery != null && !searchQuery.isEmpty()) {
            url += "&email=ilike.*" + searchQuery + "*";
        }
        if (excludeId != null && !excludeId.isEmpty()) {
            url += "&id=neq." + excludeId;
        }
        url += "&order=email.asc&limit=50";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyErrorCallback(callback, e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String body = response.body() != null ? response.body().string() : "";
                    String errorMsg = "Fetch users failed (HTTP " + response.code() + "): " + body;
                    notifyErrorCallback(callback, errorMsg);
                    return;
                }
                String json = response.body() != null ? response.body().string() : "[]";
                try {
                    UserProfile[] profiles = new Gson().fromJson(json, UserProfile[].class);
                    List<UserProfile> list = Arrays.asList(profiles);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
                } catch (Exception e) {
                    notifyErrorCallback(callback, e.getMessage());
                }
            }
        });
    }

    private void notifyErrorCallback(ProfileFetchCallback callback, String error) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onError(error));
    }

    public interface FriendRequestCallback {
        void onSuccess(List<FriendRequest> requests);
        void onError(String error);
    }


    // Send a friend request
    public void sendFriendRequest(String accessToken, String senderId, String senderEmail,
                                  String receiverId, String receiverEmail, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("sender_id", senderId);
        body.addProperty("receiver_id", receiverId);
        body.addProperty("sender_email", senderEmail);
        body.addProperty("receiver_email", receiverEmail);
        body.addProperty("status", "pending");

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/friend_requests")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    String err = response.body() != null ? response.body().string() : "";
                    notifyError(callback, "Failed to send request: " + err);
                }
            }
        });
    }

    // Get pending friend requests (incoming)
    public void fetchPendingRequests(String accessToken, String userId, FriendRequestCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/friend_requests?select=*&receiver_id=eq." + userId
                + "&status=eq.pending&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyFriendError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    FriendRequest[] arr = new Gson().fromJson(json, FriendRequest[].class);
                    List<FriendRequest> list = Arrays.asList(arr);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
                } else {
                    notifyFriendError(callback, "Failed to load pending requests");
                }
            }
        });
    }

    // Accept or reject a request (update status)
    public void respondToRequest(String accessToken, String requestId, String newStatus, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("status", newStatus);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/friend_requests?id=eq." + requestId)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .patch(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    notifyError(callback, "Failed to update request");
                }
            }
        });
    }

    // Get accepted friends (for chat list)
    public void fetchFriends(String accessToken, String userId, FriendRequestCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/friend_requests?select=*" +
                "&or=(sender_id.eq." + userId + ",receiver_id.eq." + userId + ")" +
                "&status=eq.accepted&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyFriendError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    FriendRequest[] arr = new Gson().fromJson(json, FriendRequest[].class);
                    List<FriendRequest> list = Arrays.asList(arr);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
                } else {
                    notifyFriendError(callback, "Failed to load friends");
                }
            }
        });
    }



    public void fetchFriendsWithLastMessage(String accessToken, String userId,
                                            FriendRequestCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("uid", userId);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/rpc/get_friends_with_last_message")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyFriendError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    FriendRequest[] arr = new Gson().fromJson(json, FriendRequest[].class);
                    List<FriendRequest> list = Arrays.asList(arr);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
                } else {
                    notifyFriendError(callback, "Failed to load friends");
                }
            }
        });
    }


    public void fetchConnectedUserIds(String accessToken, String userId,
                                      FriendIdCallback callback) {
        // friend_requests where (sender_id = me or receiver_id = me) and status != 'rejected'
        String url = SUPABASE_URL + "/rest/v1/friend_requests?select=sender_id,receiver_id,status" +
                "&or=(sender_id.eq." + userId + ",receiver_id.eq." + userId + ")" +
                "&status=neq.rejected";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    FriendRequest[] arr = new Gson().fromJson(json, FriendRequest[].class);
                    Set<String> ids = new HashSet<>();
                    for (FriendRequest r : arr) {
                        if (r.getSender_id().equals(userId)) {
                            ids.add(r.getReceiver_id());
                        } else {
                            ids.add(r.getSender_id());
                        }
                    }
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(ids));
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Failed to load connected users"));
                }
            }
        });
    }

    // Callback interface
    public interface FriendIdCallback {
        void onSuccess(Set<String> connectedUserIds);
        void onError(String error);
    }
    
    

    private void notifyFriendError(FriendRequestCallback callback, String error) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onError(error));
    }


    public interface MessageCallback {
        void onSuccess(List<Message> messages);
        void onError(String error);
    }


    // Fetch messages between two users (ordered by time)
    public void fetchMessages(String accessToken, String userId1, String userId2, MessageCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/messages?select=*" +
                "&or=(and(sender_id.eq." + userId1 + ",receiver_id.eq." + userId2 +
                "),and(sender_id.eq." + userId2 + ",receiver_id.eq." + userId1 + "))" +
                "&order=created_at.asc";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyMessageError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Message[] arr = new Gson().fromJson(json, Message[].class);
                    List<Message> list = Arrays.asList(arr);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
                } else {
                    notifyMessageError(callback, "Failed to load messages");
                }
            }
        });
    }

    // Send a message
    public void sendMessage(String accessToken, String senderId, String receiverId,
                            String content, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("sender_id", senderId);
        body.addProperty("receiver_id", receiverId);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/messages")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    notifyError(callback, "Failed to send message");
                }
            }
        });
    }

    private void notifyMessageError(MessageCallback callback, String error) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onError(error));
    }






    public interface ProfileFetchSingleCallback {
        void onSuccess(String username, String avatarUrl);
        void onError(String error);
    }

    public void fetchMyProfile(String accessToken, String userId, ProfileFetchSingleCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=username,avatar_url";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    try {
                        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                        if (arr.size() > 0) {
                            JsonObject obj = arr.get(0).getAsJsonObject();
                            String username = obj.has("username") && !obj.get("username").isJsonNull() ?
                                    obj.get("username").getAsString() : null;
                            String avatar = obj.has("avatar_url") && !obj.get("avatar_url").isJsonNull() ?
                                    obj.get("avatar_url").getAsString() : null;
                            new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(username, avatar));
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> callback.onError("Profile not found"));
                        }
                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to load profile"));
                }
            }
        });
    }












    // Callback interface
    public interface UserProfileCallback {
        void onSuccess(UserProfile profile);
        void onError(String error);
    }

    // Fetch a single profile by user ID (or by user_id)
    public void fetchUserProfile(String accessToken, String userId, UserProfileCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/profiles?user_id=eq." + userId + "&select=*";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    UserProfile[] arr = new Gson().fromJson(json, UserProfile[].class);
                    if (arr.length > 0) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(arr[0]));
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError("Profile not found"));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to fetch profile"));
                }
            }
        });
    }




























    public void createProfileWithUsername(String userId, String email, String username,
                                          String accessToken, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("id", userId);
        body.addProperty("user_id", userId);
        body.addProperty("email", email);
        body.addProperty("username", username);
        body.addProperty("avatar_url", "default"); // placeholder, we'll replace later

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/profiles")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() || response.code() == 201) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    String err = response.body() != null ? response.body().string() : "";
                    notifyError(callback, "Profile creation failed: " + err);
                }
            }
        });
    }
















    public interface BookCallback {
        void onSuccess(Book book);
        void onError(String error);
    }

    public interface BooksCallback {
        void onSuccess(List<Book> books);
        void onError(String error);
    }

    // Publish a new book

    public void publishBook(String accessToken, String authorId, String title,
                            String description, List<String> tags,
                            boolean isAnonymous, boolean commentsDisabled,
                            BookCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("author_id", authorId);
        body.addProperty("title", title);
        body.addProperty("description", description);
        body.add("tags", new Gson().toJsonTree(tags));
        body.addProperty("is_anonymous", isAnonymous);
        body.addProperty("comments_disabled", commentsDisabled);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/books")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Book[] books = new Gson().fromJson(json, Book[].class);
                    if (books.length > 0) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(books[0]));
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError("Book created but no response"));
                    }
                } else {
                    String err = response.body() != null ? response.body().string() : "";
                    Log.e("PUBLISH_ERROR", "Full Supabase error: " + err);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Publish failed: " + err));
                }
            }
        });
    }

    public void fetchBooks(String accessToken, String searchQuery, BooksCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/books_with_stats?select=*";
        if (searchQuery != null && !searchQuery.isEmpty()) {
            url += "&or=(title.ilike.*" + searchQuery + "*,description.ilike.*" + searchQuery + "*)";
        }
        url += "&order=created_at.desc&limit=50";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + (accessToken != null ? accessToken : ANON_KEY))
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Book[] arr = new Gson().fromJson(json, Book[].class);
                    List<Book> list = Arrays.asList(arr);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to load books"));
                }
            }
        });
    }

    // Fetch a single book by ID
    public void fetchBookById(String accessToken, String bookId, BookCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/books_with_stats?id=eq." + bookId + "&select=*";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + (accessToken != null ? accessToken : ANON_KEY))
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Book[] arr = new Gson().fromJson(json, Book[].class);
                    if (arr.length > 0) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(arr[0]));
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError("Book not found"));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to fetch book"));
                }
            }
        });
    }











    // Fetch books by author
    public void fetchMyBooks(String accessToken, String authorId, BooksCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/books_with_stats?author_id=eq." + authorId +
                "&select=*&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Book[] arr = new Gson().fromJson(json, Book[].class);
                    List<Book> list = Arrays.asList(arr);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to load your books"));
                }
            }
        });
    }









    public interface ChapterCallback {
        void onSuccess(Chapter chapter);
        void onError(String error);
    }

    public interface ChaptersCallback {
        void onSuccess(List<Chapter> chapters);
        void onError(String error);
    }



    public void createChapter(String accessToken, String bookId, int chapterNumber,
                              String title, String content, String summary,
                              String notesAbove, String notesBelow, ChapterCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("book_id", bookId);
        body.addProperty("chapter_number", chapterNumber);
        body.addProperty("title", title.isEmpty() ? "Chapter " + chapterNumber : title);
        body.addProperty("content", content);
        body.addProperty("summary", summary);
        body.addProperty("notes_above", notesAbove);
        body.addProperty("notes_below", notesBelow);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/chapters")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Chapter[] arr = new Gson().fromJson(json, Chapter[].class);
                    if (arr.length > 0) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(arr[0]));
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError("Chapter created but no response"));
                    }
                } else {
                    String err = response.body() != null ? response.body().string() : "";
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Chapter creation failed: " + err));
                }
            }
        });
    }












    // Fetch chapters for a given book, ordered by chapter_number
    public void fetchChaptersByBookId(String accessToken, String bookId,
                                      ChaptersCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/chapters?book_id=eq." + bookId +
                "&select=*&order=chapter_number.asc";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + (accessToken != null ? accessToken : ANON_KEY))
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Chapter[] arr = new Gson().fromJson(json, Chapter[].class);
                    List<Chapter> list = Arrays.asList(arr);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to load chapters"));
                }
            }
        });
    }









    public void updateBook(String accessToken, String bookId, String title,
                           String description, List<String> tags,
                           boolean isAnonymous, boolean commentsDisabled,
                           AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("title", title);
        body.addProperty("description", description);
        body.add("tags", new Gson().toJsonTree(tags));
        body.addProperty("is_anonymous", isAnonymous);
        body.addProperty("comments_disabled", commentsDisabled);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/books?id=eq." + bookId)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .patch(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    String err = response.body() != null ? response.body().string() : "";
                    notifyError(callback, "Update failed: " + err);
                }
            }
        });
    }









    public void updateChapter(String accessToken, String chapterId, String title,
                              String content, String summary, String notesAbove,
                              String notesBelow, AuthCallback callback) {
        JsonObject body = new JsonObject();
        if (title != null) body.addProperty("title", title);
        if (content != null) body.addProperty("content", content);
        if (summary != null) body.addProperty("summary", summary);
        if (notesAbove != null) body.addProperty("notes_above", notesAbove);
        if (notesBelow != null) body.addProperty("notes_below", notesBelow);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/chapters?id=eq." + chapterId)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .patch(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    notifyError(callback, "Chapter update failed");
                }
            }
        });
    }










    // Fetch single chapter by ID
    public void fetchChapterById(String accessToken, String chapterId, ChapterCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/chapters?id=eq." + chapterId + "&select=*";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + (accessToken != null ? accessToken : ANON_KEY))
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Chapter[] arr = new Gson().fromJson(json, Chapter[].class);
                    if (arr.length > 0) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(arr[0]));
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError("Chapter not found"));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to fetch chapter"));
                }
            }
        });
    }






    public void searchBooks(String accessToken, String query, BooksCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/books_with_stats?select=*";
        if (query != null && !query.isEmpty()) {
            url += "&or=(title.ilike.*" + query + "*,author_username.ilike.*" + query + "*)";
        }
        url += "&order=created_at.desc&limit=50";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + (accessToken != null ? accessToken : ANON_KEY))
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Book[] arr = new Gson().fromJson(json, Book[].class);
                    List<Book> list = Arrays.asList(arr);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Search failed"));
                }
            }
        });
    }







    public interface CommentsCallback {
        void onSuccess(List<Comment> comments);
        void onError(String error);
    }

    public void fetchComments(String accessToken, String chapterId, CommentsCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/comments_with_username?chapter_id=eq." + chapterId +
                "&select=*&order=created_at.asc";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + (accessToken != null ? accessToken : ANON_KEY))
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Comment[] arr = new Gson().fromJson(json, Comment[].class);
                    List<Comment> list = Arrays.asList(arr);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to load comments"));
                }
            }
        });
    }






    public void postComment(String accessToken, String chapterId, String userId,
                            String text, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("chapter_id", chapterId);
        body.addProperty("user_id", userId);
        body.addProperty("text", text);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/comments")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    notifyError(callback, "Failed to post comment");
                }
            }
        });
    }






    public interface NotificationsCallback {
        void onSuccess(List<Notification> notifications);
        void onError(String error);
    }

    public void fetchNotifications(String accessToken, String userId, NotificationsCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/notifications?user_id=eq." + userId +
                "&select=*&order=created_at.desc&limit=50";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Notification[] arr = new Gson().fromJson(json, Notification[].class);
                    List<Notification> list = Arrays.asList(arr);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to fetch notifications"));
                }
            }
        });
    }




    public void markAllNotificationsRead(String accessToken, String userId, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("is_read", true);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/notifications?user_id=eq." + userId + "&is_read=eq.false")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .patch(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    notifyError(callback, "Failed to mark notifications read");
                }
            }
        });
    }




    public void createNotification(String accessToken, String userId, String type,
                                   String message, String chapterId, String bookId,
                                   AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("p_user_id", userId);
        body.addProperty("p_type", type);
        body.addProperty("p_message", message);
        if (chapterId != null) body.addProperty("p_related_chapter_id", chapterId);
        if (bookId != null) body.addProperty("p_related_book_id", bookId);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/rpc/create_notification")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    notifyError(callback, "Failed to create notification");
                }
            }
        });
    }







    public void advancedSearch(String accessToken,
                               String titleQuery,
                               String authorQuery,
                               String fandom,
                               List<String> warnings,
                               String rating,
                               List<String> categories,
                               String language,
                               String charactersQuery,
                               String relationshipsQuery,
                               String freeformsQuery,
                               int minWords, int maxWords,
                               BooksCallback callback) {
        StringBuilder urlBuilder = new StringBuilder(SUPABASE_URL + "/rest/v1/books_with_stats?select=*");
        List<String> filters = new ArrayList<>();

        // Title search
        if (titleQuery != null && !titleQuery.isEmpty()) {
            filters.add("title.ilike.*" + titleQuery + "*");
        }

        // Author search (exact or pattern)
        if (authorQuery != null && !authorQuery.isEmpty()) {
            filters.add("author_username.ilike.*" + authorQuery + "*");
        }

        // Fandom (exact match in tags)
        if (fandom != null && !fandom.isEmpty() && !fandom.equals("All")) {
            filters.add("tags.cs.{Fandom:" + fandom + "}");
        }

        // Rating
        if (rating != null && !rating.isEmpty() && !rating.equals("All")) {
            filters.add("tags.cs.{Rating:" + rating + "}");
        }

        // Warnings (each selected)
        if (warnings != null) {
            for (String w : warnings) {
                filters.add("tags.cs.{Warning:" + w + "}");
            }
        }

        // Categories
        if (categories != null) {
            for (String cat : categories) {
                filters.add("tags.cs.{Category:" + cat + "}");
            }
        }

        // Language
        if (language != null && !language.isEmpty() && !language.equals("All")) {
            filters.add("tags.cs.{Language:" + language + "}");
        }

        // Characters (pattern match inside Character: prefix)
        if (charactersQuery != null && !charactersQuery.isEmpty()) {
            filters.add("tags.cs.{Character:" + charactersQuery + "*}");
        }

        // Relationships (pattern match inside Relationship: prefix)
        if (relationshipsQuery != null && !relationshipsQuery.isEmpty()) {
            filters.add("tags.cs.{Relationship:" + relationshipsQuery + "*}");
        }

        // Freeforms (additional tags) – search as plain text inside the array
        if (freeformsQuery != null && !freeformsQuery.isEmpty()) {
            filters.add("tags.cs.{" + freeformsQuery + "}");
        }

        // Word count range
        if (minWords > 0) filters.add("word_count.gte." + minWords);
        if (maxWords > 0) filters.add("word_count.lte." + maxWords);

        // Build query string
        if (!filters.isEmpty()) {
            urlBuilder.append("&and=(").append(String.join(",", filters)).append(")");
        }
        urlBuilder.append("&order=created_at.desc&limit=50");

        String url = urlBuilder.toString();

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + (accessToken != null ? accessToken : ANON_KEY))
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Book[] arr = new Gson().fromJson(json, Book[].class);
                    List<Book> list = Arrays.asList(arr);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Search failed"));
                }
            }
        });
    }










    public void uploadAvatar(String accessToken, String userId, byte[] imageBytes,
                             String fileExtension, AuthCallback callback) {
        if (imageBytes == null || imageBytes.length == 0) {
            notifyError(callback, "Image data is empty");
            return;
        }

        String fileName = userId + "." + (fileExtension.startsWith(".") ? fileExtension.substring(1) : fileExtension);
        String url = SUPABASE_URL + "/storage/v1/object/avatars/" + fileName;
        String mimeType = fileExtension.equalsIgnoreCase("png") ? "image/png" : "image/jpeg";

        Log.d("UPLOAD_DEBUG", "Uploading " + imageBytes.length + " bytes to " + url + " as " + mimeType);

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", mimeType)
                .put(RequestBody.create(imageBytes, MediaType.parse(mimeType)))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("UPLOAD_DEBUG", "Network failure", e);
                notifyError(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("UPLOAD_DEBUG", "Upload successful");
                    notifySuccess(callback, null, null, null, null);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "no body";
                    Log.e("UPLOAD_DEBUG", "Upload failed: HTTP " + response.code() + " " + errorBody);
                    notifyError(callback, "Upload failed (HTTP " + response.code() + "): " + errorBody);
                }
            }
        });
    }

    public void updateAvatarUrl(String accessToken, String userId, String avatarUrl,
                                AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("avatar_url", avatarUrl);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/profiles?user_id=eq." + userId)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .patch(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    notifyError(callback, "Failed to update avatar URL");
                }
            }
        });
    }





    public void deleteBook(String accessToken, String bookId, AuthCallback callback) {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/books?id=eq." + bookId)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    notifyError(callback, "Delete failed");
                }
            }
        });
    }



    public interface TagsCallback {
        void onSuccess(List<String> tagNames);
        void onError(String error);
    }



    public void fetchFandoms(String accessToken, TagsCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/fandoms?select=name&order=name.asc";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + (accessToken != null ? accessToken : ANON_KEY))
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                    List<String> names = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        names.add(arr.get(i).getAsJsonObject().get("name").getAsString());
                    }
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(names));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to fetch fandoms"));
                }
            }
        });
    }





// ---- Bookmarks ----

    public void addBookmark(String accessToken, String userId, String bookId, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("book_id", bookId);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/bookmarks")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() || response.code() == 201) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    String err = response.body() != null ? response.body().string() : "";
                    notifyError(callback, "Failed to add bookmark: " + err);
                }
            }
        });
    }

    public void removeBookmark(String accessToken, String userId, String bookId, AuthCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/bookmarks?user_id=eq." + userId + "&book_id=eq." + bookId;

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    notifyError(callback, "Failed to remove bookmark");
                }
            }
        });
    }

    // Check if bookmarked
    public void checkBookmark(String accessToken, String userId, String bookId, AuthCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/bookmarks?user_id=eq." + userId + "&book_id=eq." + bookId + "&select=id";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                    if (arr.size() > 0) {
                        notifySuccess(callback, null, null, null, null); // exists
                    } else {
                        notifyError(callback, "not found"); // does not exist
                    }
                } else {
                    notifyError(callback, "Failed to check bookmark");
                }
            }
        });
    }

    // Fetch all bookmarked books (with full book data via a view)
    public void fetchBookmarkedBooks(String accessToken, String userId, BooksCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("uid", userId);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/rpc/get_bookmarked_books")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Book[] arr = new Gson().fromJson(json, Book[].class);
                    List<Book> list = Arrays.asList(arr);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to fetch bookmarked books"));
                }
            }
        });
    }

    // Simpler: fetch bookmark entries, return list of book IDs
    public void fetchBookmarkIds(String accessToken, String userId, IdListCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/bookmarks?user_id=eq." + userId + "&select=book_id&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                    List<String> bookIds = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        bookIds.add(arr.get(i).getAsJsonObject().get("book_id").getAsString());
                    }
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(bookIds));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to fetch bookmark IDs"));
                }
            }
        });
    }

    public interface IdListCallback {
        void onSuccess(List<String> ids);
        void onError(String error);
    }


    public interface FandomStatsCallback {
        void onSuccess(List<FandomStat> stats);
        void onError(String error);
    }

    public void fetchFandomStats(String accessToken, boolean showAll, FandomStatsCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/fandom_stats_all?select=*&order=book_count.desc";
        if (!showAll) {
            url += "&book_count=gt.0";
        }

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + (accessToken != null ? accessToken : ANON_KEY))
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    FandomStat[] arr = new Gson().fromJson(json, FandomStat[].class);
                    List<FandomStat> list = Arrays.asList(arr);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Failed to fetch fandom stats"));
                }
            }
        });
    }




    public void resetPasswordForEmail(String email, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/recover")
                .header("apikey", ANON_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyError(callback, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccess(callback, null, null, null, null);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    notifyError(callback, "Reset failed: " + errorBody);
                }
            }
        });
    }

}
