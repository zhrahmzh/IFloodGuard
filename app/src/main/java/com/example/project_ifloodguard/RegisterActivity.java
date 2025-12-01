package com.example.project_ifloodguard;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore; // MUST BE FIRESTORE

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // 1. Declare UI Variables (Only the ones that exist in your XML now)
    private EditText fullNameInput, emailInput, phoneInput, passwordInput;
    private Button registerButton;
    private TextView loginRedirect;
    private ImageView eyeIcon;

    private boolean passwordVisible = false;

    // 2. Declare Firebase Variables
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 3. Initialize Views
        fullNameInput = findViewById(R.id.fullNameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);

        registerButton = findViewById(R.id.registerButton);
        loginRedirect = findViewById(R.id.loginRedirect);
        eyeIcon = findViewById(R.id.eyeIcon);

        // 4. Initialize Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // 5. Setup Listeners
        eyeIcon.setOnClickListener(v -> togglePasswordVisibility());

        registerButton.setOnClickListener(v -> {
            // Get text from inputs
            String name = fullNameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            // Validate
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            } else {
                registerUser(name, email, phone, password);
            }
        });

        if (loginRedirect != null) {
            loginRedirect.setOnClickListener(v -> {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            });
        }
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

    private void registerUser(String name, String email, String phone, String password) {
        // Create user in Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = auth.getCurrentUser().getUid();

                        // Check for Role (First User Rule)
                        determineRoleAndSave(uid, name, email, phone);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void determineRoleAndSave(String uid, String name, String email, String phone) {
        // Check if the "Users" collection is empty
        firestore.collection("Users").limit(1).get()
                .addOnSuccessListener(querySnapshot -> {
                    String role;

                    if (querySnapshot.isEmpty()) {
                        // If NO users exist, this is the first one -> ADMIN
                        role = "Admin";
                    } else {
                        // If users already exist, this is just another user -> STAFF
                        role = "Staff";
                    }

                    saveUserToFirestore(uid, name, email, phone, role);
                })
                .addOnFailureListener(e -> {
                    // In case of error checking, fail safe to Staff
                    saveUserToFirestore(uid, name, email, phone, "Staff");
                });
    }

    private void saveUserToFirestore(String uid, String name, String email, String phone, String role) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("fullName", name);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("role", role);

        firestore.collection("Users").document(uid).set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registered as " + role, Toast.LENGTH_LONG).show();

                    // Go to Login Page
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}