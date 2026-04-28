package com.example.water.supabase;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.water.FriendRequest;
import com.example.water.Message;
import com.example.water.UserProfile;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
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
        body.addProperty("content", content);

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












    // Full profile callback
    public interface FullProfileCallback {
        void onSuccess(String email, String username, String avatarUrl, String createdAt);
        void onError(String error);
    }

    public void fetchFullProfile(String userId, FullProfileCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=email,username,avatar_url,created_at";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    try {
                        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                        if (arr.size() > 0) {
                            JsonObject obj = arr.get(0).getAsJsonObject();
                            String email = getJsonString(obj, "email");
                            String username = getJsonString(obj, "username");
                            String avatarUrl = getJsonString(obj, "avatar_url");
                            String createdAt = getJsonString(obj, "created_at");
                            mainHandler(() -> callback.onSuccess(email, username, avatarUrl, createdAt));
                        } else {
                            mainHandler(() -> callback.onError("Profile not found"));
                        }
                    } catch (Exception e) {
                        mainHandler(() -> callback.onError(e.getMessage()));
                    }
                } else {
                    mainHandler(() -> callback.onError("Failed to load profile"));
                }
            }
        });
    }

    // Username uniqueness check
    public interface UsernameCheckCallback {
        void onResult(boolean isUnique);
    }

    public void checkUsernameUnique(String username, String excludeUserId, UsernameCheckCallback callback) {
        // Count profiles with that username, excluding own id
        String url = SUPABASE_URL + "/rest/v1/profiles?select=id&username=eq." + username +
                "&id=neq." + excludeUserId + "&limit=1";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", ANON_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler(() -> callback.onResult(false));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    boolean exists = json.length() > 2; // empty array => []
                    mainHandler(() -> callback.onResult(!exists));
                } else {
                    mainHandler(() -> callback.onResult(false));
                }
            }
        });
    }

    // Update profile
    public void updateProfile(String accessToken, String userId, String username, String avatarUrl,
                              AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("avatar_url", avatarUrl);

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId)
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .patch(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    mainHandler(() -> callback.onSuccess(null, null, null, null));
                } else {
                    mainHandler(() -> callback.onError("Failed to update profile"));
                }
            }
        });
    }

    private void mainHandler(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }

    private String getJsonString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }
}
