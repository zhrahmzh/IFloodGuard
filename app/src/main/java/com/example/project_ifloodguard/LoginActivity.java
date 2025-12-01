package com.example.project_ifloodguard;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView; // Import TextView
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginButton;
    ImageView eyeIcon;
    TextView registerRedirect; // <--- 1. ADD THIS VARIABLE

    boolean passwordVisible = false;
    FirebaseAuth auth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Init Views
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        eyeIcon = findViewById(R.id.eyeIcon);

        // <--- 2. CONNECT THE REGISTER LINK
        registerRedirect = findViewById(R.id.registerRedirect);

        // Init Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Listeners
        eyeIcon.setOnClickListener(v -> togglePasswordVisibility());
        loginButton.setOnClickListener(v -> loginUser());

        // <--- 3. ADD THE CLICK ACTION
        registerRedirect.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void togglePasswordVisibility() {
        if (passwordVisible) {
            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            eyeIcon.setImageResource(android.R.drawable.ic_menu_view);
            passwordVisible = false;
        } else {
            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            eyeIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            passwordVisible = true;
        }
        passwordInput.setSelection(passwordInput.getText().length());
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return;
        }
        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkUserRole(auth.getCurrentUser().getUid());
                    } else {
                        Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRole(String uid) {
        firestore.collection("Users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String role = document.getString("role");
                            if (role == null) role = "Staff";

                            Toast.makeText(LoginActivity.this, "Welcome " + role, Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(LoginActivity.this, HomePageActivity.class);
                            intent.putExtra("USER_ROLE", role);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "User profile not found!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}