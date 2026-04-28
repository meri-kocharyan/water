package com.example.water;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "user_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_EMAIL = "user_email";

    private SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static final String KEY_USER_ID = "user_id";

    public void saveAuthData(String accessToken, String refreshToken, String email, String userId) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getAccessToken() { return prefs.getString(KEY_ACCESS_TOKEN, null); }
    public String getRefreshToken() { return prefs.getString(KEY_REFRESH_TOKEN, null); }
    public String getUserEmail() { return prefs.getString(KEY_USER_EMAIL, null); }

    public void clearSession() {
        prefs.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }




    private static final String KEY_USERNAME = "username";
    private static final String KEY_AVATAR_URL = "avatar_url";

    public void saveProfile(String username, String avatarUrl) {
        prefs.edit()
                .putString(KEY_USERNAME, username)
                .putString(KEY_AVATAR_URL, avatarUrl)
                .apply();
    }

    public String getUsername() { return prefs.getString(KEY_USERNAME, null); }
    public String getAvatarUrl() { return prefs.getString(KEY_AVATAR_URL, null); }
}