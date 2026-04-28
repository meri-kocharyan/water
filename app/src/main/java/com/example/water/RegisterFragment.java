package com.example.water;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.water.supabase.SupabaseAuthHelper;

public class RegisterFragment extends Fragment {

    private EditText editEmail, editPassword, editConfirmPassword;
    private Button btnRegister;
    private TextView txtLoginInstead;

    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.register, container, false);

        editEmail = view.findViewById(R.id.editEmail);
        editPassword = view.findViewById(R.id.editPassword);
        editConfirmPassword = view.findViewById(R.id.editConfirmPassword);
        btnRegister = view.findViewById(R.id.btnRegister);
        txtLoginInstead = view.findViewById(R.id.txtLoginInstead);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        btnRegister.setOnClickListener(v -> performRegister());
        txtLoginInstead.setOnClickListener(v -> {
            // Go back to login (pop back stack)
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }

    private void performRegister() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String confirm = editConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        authHelper.signUp(email, password, new SupabaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String accessToken, String refreshToken, String email, String userId) {
                sessionManager.saveAuthData(accessToken, refreshToken, email, userId);
                // Create profile row
                authHelper.createProfile(userId, email, accessToken, new SupabaseAuthHelper.AuthCallback() {
                    @Override
                    public void onSuccess(String a, String b, String c, String d) {
                        // Profile created, now go to main
                        requireActivity().runOnUiThread(() -> {
                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            if (getActivity() != null) getActivity().finish();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Warning: " + error, Toast.LENGTH_LONG).show()
                        );
                        // Still let them in even if profile creation fails
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        if (getActivity() != null) getActivity().finish();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }
}