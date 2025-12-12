package com.example.project_ifloodguard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 1. Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 2. "Update Details" Row
        View rowUpdate = findViewById(R.id.row_update_details);
        setupRow(rowUpdate, "Update Profile", android.R.drawable.ic_menu_edit);
        rowUpdate.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ProfileUpdateActivity.class));
        });

        // 3. "Change Password" Row (Converted to Row for consistency)
        View rowPass = findViewById(R.id.row_change_password);
        setupRow(rowPass, "Change Password", android.R.drawable.ic_lock_lock); // Lock Icon
        rowPass.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ChangePasswordActivity.class));
        });

        // 4. Logout Button (Red button at bottom)
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    // Helper to set text/icon for standard rows
    private void setupRow(View view, String title, int iconResId) {
        TextView text = view.findViewById(R.id.item_text);
        ImageView icon = view.findViewById(R.id.item_icon);

        if(text != null) text.setText(title);
        if(icon != null) icon.setImageResource(iconResId);
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }
}