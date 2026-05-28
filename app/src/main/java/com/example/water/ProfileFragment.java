package com.example.water;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.water.supabase.SupabaseAuthHelper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private ImageView ivAvatar;
    private TextView tvUsername, tvEmail, tvJoinDate;
    private Button btnLogout;
    private SessionManager sessionManager;
    private SupabaseAuthHelper authHelper;

    private ActivityResultLauncher<String> pickImageLauncher;

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

        // Register the image picker launcher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadImage(uri);
                    }
                });

        // Open image picker when avatar is tapped
        ivAvatar.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });

        btnLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        loadProfile();

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
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    Date date = inputFormat.parse(profile.getCreated_at().replace("Z", ""));
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                    tvJoinDate.setText("Joined " + outputFormat.format(date));
                } catch (Exception e) {
                    tvJoinDate.setText("Joined recently");
                }

                // Load avatar with Glide
                String avatarUrl = profile.getAvatar_url();
                if (avatarUrl != null && !avatarUrl.isEmpty() && !avatarUrl.equals("default")) {
                    Glide.with(requireContext())
                            .load(avatarUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_default_avatar)
                            .error(R.drawable.ic_default_avatar)
                            .into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                ivAvatar.setImageResource(R.drawable.ic_default_avatar);
            }
        });
    }

    private void uploadImage(Uri imageUri) {
        try {
            // Read bytes from the image URI
            InputStream inputStream = requireActivity().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Determine file extension (default to jpg)
            String ext = "jpg";
            // Compress to JPEG
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();

            String token = sessionManager.getAccessToken();
            String userId = sessionManager.getUserId();
            if (token == null || userId == null) return;

            authHelper.uploadAvatar(token, userId, imageBytes, ext, new SupabaseAuthHelper.AuthCallback() {
                @Override
                public void onSuccess(String a, String b, String c, String d) {
                    // Construct public URL
                    String publicUrl = "https://tbspnnujtxombnenshmj.supabase.co/storage/v1/object/public/avatars/"
                            + userId + "." + ext;
                    // Update profile with avatar URL
                    authHelper.updateAvatarUrl(token, userId, publicUrl, new SupabaseAuthHelper.AuthCallback() {
                        @Override
                        public void onSuccess(String a1, String b1, String c1, String d1) {
                            Toast.makeText(getContext(), "Avatar updated!", Toast.LENGTH_SHORT).show();
                            loadProfile(); // refresh to show new avatar
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(getContext(), "Failed to update avatar URL: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Upload failed: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }
}
