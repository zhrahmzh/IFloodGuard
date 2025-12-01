package com.example.project_ifloodguard;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ProfileUpdateActivity extends AppCompatActivity {

    TextInputEditText etName, etPhone, etEmail;
    Button btnUpdate;
    DatabaseReference userRef;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_profile_update);

        // 1. Init Views
        etName = findViewById(R.id.etProfileName);
        etPhone = findViewById(R.id.etProfilePhone);
        etEmail = findViewById(R.id.etProfileEmail);
        btnUpdate = findViewById(R.id.btnUpdateProfile);

        // 2. Get Current User
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            etEmail.setText(user.getEmail()); // Set email from Auth

            // Point to "users/USER_ID" in database
            userRef = FirebaseDatabase
                    .getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("users").child(userId);

            // 3. Load existing data
            loadUserData();
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 4. Save Button
        btnUpdate.setOnClickListener(v -> updateUserData());
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);

                    if(name != null) etName.setText(name);
                    if(phone != null) etPhone.setText(phone);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUserData() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("phone", phone);

        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}