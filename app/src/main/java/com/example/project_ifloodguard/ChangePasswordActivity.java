package com.example.project_ifloodguard;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    EditText etCurrent, etNew, etConfirm;
    Button btnUpdate;
    ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        etCurrent = findViewById(R.id.etCurrentPass);
        etNew = findViewById(R.id.etNewPass);
        etConfirm = findViewById(R.id.etConfirmPass);
        btnUpdate = findViewById(R.id.btnUpdatePass);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // ⭐ KEYBOARD FIX ⭐
        findViewById(android.R.id.content).setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });

        // ⭐ UPDATE BUTTON WITH CONFIRMATION ⭐
        btnUpdate.setOnClickListener(v -> showUpdateConfirmation());
    }

    // ⭐ NEW: CONFIRMATION DIALOG ⭐
    private void showUpdateConfirmation() {
        String currentPass = etCurrent.getText().toString().trim();
        String newPass = etNew.getText().toString().trim();
        String confirmPass = etConfirm.getText().toString().trim();

        // 1. Basic Check (Empty fields)
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Show Dialog
        new AlertDialog.Builder(this)
                .setTitle("Update Password")
                .setMessage("Are you sure you want to update your password?")
                .setPositiveButton("Yes", (dialog, which) -> attemptPasswordChange(currentPass, newPass, confirmPass))
                .setNegativeButton("No", null)
                .show();
    }

    private void attemptPasswordChange(String currentPass, String newPass, String confirmPass) {
        // 3. Validation Logic
        if (newPass.length() < 6) {
            etNew.setError("Password must be at least 6 chars");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            etConfirm.setError("Passwords do not match");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {

            // ⭐ 4. SECURITY CHECK (Re-Authentication) ⭐
            // This checks if the "Current Password" is correct BEFORE updating.
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Current Password IS Correct -> Proceed to Update
                    updateFirebasePassword(user, newPass);
                } else {
                    // Current Password IS WRONG -> Show Error
                    etCurrent.setError("Incorrect Current Password");
                    Toast.makeText(this, "Authentication Failed: Wrong Password", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateFirebasePassword(FirebaseUser user, String newPass) {
        user.updatePassword(newPass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Password Updated Successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Update Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}