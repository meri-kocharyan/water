package com.example.water;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.water.supabase.SupabaseAuthHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private ImageView ivAvatar;
    private TextView tvUsername, tvEmail, tvJoinDate;
    private Button btnLogout;
    private SessionManager sessionManager;
    private SupabaseAuthHelper authHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvJoinDate = view.findViewById(R.id.tvJoinDate);
        btnLogout = view.findViewById(R.id.btnLogout);

        sessionManager = new SessionManager(requireContext());
        authHelper = new SupabaseAuthHelper();

        // Load user profile
        loadProfile();

        // Logout
        btnLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        // Avatar click (for future upload) – just a toast for now
        ivAvatar.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Upload avatar coming soon!", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void loadProfile() {
        String token = sessionManager.getAccessToken();
        String userId = sessionManager.getUserId();
        if (token == null || userId == null) return;

        authHelper.fetchUserProfile(token, userId, new SupabaseAuthHelper.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                tvUsername.setText(profile.getUsername() != null ? profile.getUsername() : "Unknown");
                tvEmail.setText(profile.getEmail());
                // Format created_at
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    Date date = inputFormat.parse(profile.getCreated_at().replace("Z", ""));
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                    tvJoinDate.setText("Joined " + outputFormat.format(date));
                } catch (Exception e) {
                    tvJoinDate.setText("Joined recently");
                }
                // Load avatar (if URL exists, use Glide; else default)
                String avatarUrl = profile.getAvatar_url();
                if (avatarUrl != null && !avatarUrl.isEmpty() && !avatarUrl.equals("default")) {
                    // Use Glide (you must add the dependency)
                    // Glide.with(requireContext()).load(avatarUrl).circleCrop().into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                // Fallback: show data from local session
                tvUsername.setText("User");
                tvEmail.setText(sessionManager.getUserEmail());
                tvJoinDate.setText("Joined recently");
            }
        });
    }
}
