package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.water.supabase.SupabaseAuthHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";

    private String userId; // profile owner
    private ImageView ivAvatar;
    private TextView tvUsername, tvEmail, tvJoined;
    private LinearLayout editContainer;
    private EditText etUsername, etAvatarUrl;
    private Button btnSave;

    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    public static ProfileFragment newInstance(String userId) {
        ProfileFragment frag = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvJoined = view.findViewById(R.id.tvJoined);
        editContainer = view.findViewById(R.id.editButtonsContainer);
        etUsername = view.findViewById(R.id.etUsername);
        etAvatarUrl = view.findViewById(R.id.etAvatarUrl);
        btnSave = view.findViewById(R.id.btnSave);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        loadProfile();

        // Set up edit mode if own profile
        if (userId.equals(sessionManager.getUserId())) {
            editContainer.setVisibility(View.VISIBLE);
            btnSave.setOnClickListener(v -> saveProfile());
        }

        return view;
    }

    private void loadProfile() {
        // Fetch profile from Supabase
        authHelper.fetchProfileById(userId, new SupabaseAuthHelper.ProfileFetchSingleCallback() {
            @Override
            public void onSuccess(String username, String avatarUrl) {
                // Show username or email
                // Need email as well – fetch from profiles table (we need email and created_at)
                // We'll need another method that returns full profile including email and created_at
                // For now, we'll add a method that returns the whole row.
                // Quick patch: we'll also fetch the full profile via getFullProfile
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // Better: create a method that returns the whole profile row including email and created_at
        authHelper.fetchFullProfile(userId, new SupabaseAuthHelper.FullProfileCallback() {
            @Override
            public void onSuccess(String email, String username, String avatarUrl, String createdAt) {
                tvEmail.setText(email);
                String displayName = (username != null && !username.isEmpty()) ? username : email;
                tvUsername.setText(displayName);
                String joinDate = "";
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    Date date = sdf.parse(createdAt.replace("Z", ""));
                    SimpleDateFormat out = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                    joinDate = "Joined " + out.format(date);
                } catch (Exception ignored) {}
                tvJoined.setText(joinDate);

                // Load avatar
                Glide.with(requireContext())
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_default_avatar)
                        .apply(RequestOptions.circleCropTransform())
                        .into(ivAvatar);

                // If own profile, pre-fill edit fields
                if (userId.equals(sessionManager.getUserId())) {
                    etUsername.setText(username);
                    etAvatarUrl.setText(avatarUrl);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String newUsername = etUsername.getText().toString().trim();
        String newAvatar = etAvatarUrl.getText().toString().trim();
        if (newUsername.isEmpty()) {
            Toast.makeText(getContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check uniqueness
        authHelper.checkUsernameUnique(newUsername, sessionManager.getUserId(), isUnique -> {
            if (!isUnique) {
                Toast.makeText(getContext(), "Username already taken", Toast.LENGTH_SHORT).show();
            } else {
                // Update profile
                authHelper.updateProfile(sessionManager.getAccessToken(),
                        sessionManager.getUserId(), newUsername, newAvatar,
                        new SupabaseAuthHelper.AuthCallback() {
                            @Override
                            public void onSuccess(String a, String b, String c, String d) {
                                sessionManager.saveProfile(newUsername, newAvatar);
                                Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                                loadProfile(); // refresh display
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }
}
