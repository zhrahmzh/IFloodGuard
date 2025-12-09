package com.example.project_ifloodguard;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    Button btnReset;
    EditText etEmail;
    TextView tvBack;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Init Views
        btnReset = findViewById(R.id.btnReset);
        etEmail = findViewById(R.id.etForgotEmail);
        tvBack = findViewById(R.id.tvBackToLogin);

        auth = FirebaseAuth.getInstance();

        // Reset Button Logic
        btnReset.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Email is required");
                return;
            }

            resetPassword(email);
        });

        // Back Button Logic
        tvBack.setOnClickListener(v -> finish()); // Just closes this page
    }

    private void resetPassword(String email) {
        // Show loading toast (optional)
        Toast.makeText(this, "Sending...", Toast.LENGTH_SHORT).show();

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(ForgotPasswordActivity.this, "Reset link sent to your email!", Toast.LENGTH_LONG).show();
                        finish(); // Close page after success
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ForgotPasswordActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}