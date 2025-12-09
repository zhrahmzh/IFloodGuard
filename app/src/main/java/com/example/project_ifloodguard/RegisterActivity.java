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
// 1. CHANGED: Import Firestore
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    // UI Variables
    private EditText fullNameInput, emailInput, phoneInput, passwordInput, confirmPasswordInput;
    private Button registerButton;
    private TextView loginRedirect;
    private ImageView eyeIcon, confirmEyeIcon;

    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;
    TextInputLayout passwordLayout;

    // Firebase Variables
    private FirebaseAuth auth;
    private FirebaseFirestore firestore; // 2. CHANGED: Use FirebaseFirestore

    // Strong Password Pattern
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    "(?=.*[0-9])" +         // at least 1 digit
                    "(?=.*[a-z])" +         // at least 1 lower case letter
                    "(?=.*[A-Z])" +         // at least 1 upper case letter
                    "(?=.*[@#$%^&+=!_])" +    // at least 1 special character
                    "(?=\\S+$)" +           // no white spaces
                    ".{8,}" +               // at least 8 characters
                    "$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Views
        fullNameInput = findViewById(R.id.fullNameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        passwordLayout = findViewById(R.id.passwordLayout); // <--- ADD THIS

        registerButton = findViewById(R.id.registerButton);
        loginRedirect = findViewById(R.id.loginRedirect);
        eyeIcon = findViewById(R.id.eyeIcon);
        confirmEyeIcon = findViewById(R.id.confirmEyeIcon);

        // Listeners
        eyeIcon.setOnClickListener(v -> togglePasswordVisibility());
        confirmEyeIcon.setOnClickListener(v -> toggleConfirmPasswordVisibility());

        // 3. CHANGED: Initialize Firestore
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        registerButton.setOnClickListener(v -> {
            String name = fullNameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!phone.matches("[0-9]+")) {
                phoneInput.setError("Enter only numbers (No '-' or spaces)");
                return;
            }

            // 2. Check length (Must be 10 or 11 digits)
            if (phone.length() < 10 || phone.length() > 11) {
                phoneInput.setError("Phone number must be 10 or 11 digits");
                return;
            }

            if (!password.equals(confirmPassword)) {
                confirmPasswordInput.setError("Passwords do not match!");
                return;
            }

            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                // FIX 1: Use passwordLayout to show text BELOW the box
                passwordLayout.setError("Must have 8+ chars, 1 Uppercase, 1 Lowercase, 1 Number & 1 Symbol");
                return;
            } else {
                // Clear the error if password is correct
                passwordLayout.setError(null);
            }

            // FIX 2: Actually call the register function!
            registerUser(name, email, phone, password);
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

    private void toggleConfirmPasswordVisibility() {
        if (confirmPasswordVisible) {
            confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            confirmEyeIcon.setImageResource(android.R.drawable.ic_menu_view);
            confirmPasswordVisible = false;
        } else {
            confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            confirmEyeIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            confirmPasswordVisible = true;
        }
        confirmPasswordInput.setSelection(confirmPasswordInput.getText().length());
    }

    private void registerUser(String name, String email, String phone, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = auth.getCurrentUser().getUid();
                        determineRoleAndSave(uid, name, email, phone);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 4. CHANGED: Logic to check Firestore for "Admin" logic
    private void determineRoleAndSave(String uid, String name, String email, String phone) {
        // Check "Users" collection size
        firestore.collection("Users").limit(1).get()
                .addOnSuccessListener(querySnapshot -> {
                    String role = "Staff"; // Default
                    if (querySnapshot.isEmpty()) {
                        role = "Admin"; // No documents found, so this is the first user
                    }
                    saveUserToFirestore(uid, name, email, phone, role);
                })
                .addOnFailureListener(e -> {
                    // If checking fails, default to Staff
                    saveUserToFirestore(uid, name, email, phone, "Staff");
                });
    }

    // 5. CHANGED: Save to Cloud Firestore
    private void saveUserToFirestore(String uid, String name, String email, String phone, String role) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("fullName", name);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("role", role);

        firestore.collection("Users").document(uid).set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registered as " + role, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}