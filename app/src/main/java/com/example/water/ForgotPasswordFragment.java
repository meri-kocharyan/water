package com.example.water;

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

public class ForgotPasswordFragment extends Fragment {

    private EditText etEmail;
    private Button btnReset;
    private TextView tvBack;
    private SupabaseAuthHelper authHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forgot_password, container, false);

        etEmail = view.findViewById(R.id.etEmail);
        btnReset = view.findViewById(R.id.btnReset);
        tvBack = view.findViewById(R.id.tvBackToLogin);
        authHelper = new SupabaseAuthHelper();

        btnReset.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(getContext(), "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            authHelper.resetPasswordForEmail(email, new SupabaseAuthHelper.AuthCallback() {
                @Override
                public void onSuccess(String a, String b, String c, String d) {
                    Toast.makeText(getContext(),
                            "If that email is registered, a reset link has been sent.",
                            Toast.LENGTH_LONG).show();
                    // Navigate back to login
                    requireActivity().getSupportFragmentManager().popBackStack();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });

        tvBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }
}