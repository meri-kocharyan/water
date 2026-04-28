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

public class LoginFragment extends Fragment {

    private EditText editEmail, editPassword;
    private Button btnLoginSubmit;
    private TextView txtRegister, txtForgotPassword;

    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login, container, false);

        editEmail = view.findViewById(R.id.editEmail);
        editPassword = view.findViewById(R.id.editPassword);
        btnLoginSubmit = view.findViewById(R.id.btnLoginSubmit);
        txtRegister = view.findViewById(R.id.txtRegister);
        txtForgotPassword = view.findViewById(R.id.txtForgotPassword);

        // We need context for SessionManager; after inflation we have it
        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        btnLoginSubmit.setOnClickListener(v -> performLogin());
        txtRegister.setOnClickListener(v -> {
            // Replace current fragment with RegisterFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new RegisterFragment())
                    .addToBackStack(null)
                    .commit();
        });
        txtForgotPassword.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Forgot password? (not implemented yet)", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void performLogin() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        authHelper.signIn(email, password, new SupabaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String accessToken, String refreshToken, String email, String userId) {
                sessionManager.saveAuthData(accessToken, refreshToken, email, userId);
                // Restart MainActivity to reflect logged-in state
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) getActivity().finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });


    }
}