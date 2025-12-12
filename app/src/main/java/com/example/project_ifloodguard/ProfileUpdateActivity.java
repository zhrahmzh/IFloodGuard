package com.example.project_ifloodguard;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileUpdateActivity extends AppCompatActivity {

    TextInputEditText etName, etPhone, etEmail, etAddress, etRole;
    Button btnUpdate;
    ImageView ivProfilePic, btnBack;

    FirebaseFirestore firestore;
    DocumentReference userRef;
    String userId;

    // Image Handling Variables
    private static final int PICK_IMAGE_REQUEST = 1;
    private String encodedImageString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_profile_update);

        // Init Views
        etName = findViewById(R.id.etProfileName);
        etPhone = findViewById(R.id.etProfilePhone);
        etEmail = findViewById(R.id.etProfileEmail);
        etAddress = findViewById(R.id.etProfileAddress);
        etRole = findViewById(R.id.etProfileRole);

        btnUpdate = findViewById(R.id.btnUpdateProfile);
        ivProfilePic = findViewById(R.id.ivProfilePic);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        findViewById(R.id.cardProfilePic).setOnClickListener(v -> openFileChooser());

        // ⭐ KEYBOARD FIX: Hide when touching background ⭐
        findViewById(android.R.id.content).setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });

        // Setup Firestore Connection
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            etEmail.setText(user.getEmail());
            etEmail.setEnabled(false); // Lock Email

            firestore = FirebaseFirestore.getInstance();
            userRef = firestore.collection("Users").document(userId);

            loadUserData();
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // ⭐ UPDATE BUTTON: Show Confirmation Dialog instead of saving directly ⭐
        btnUpdate.setOnClickListener(v -> showSaveConfirmationDialog());
    }

    // ⭐ NEW: CONFIRMATION DIALOG ⭐
    private void showSaveConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Save Changes?")
                .setMessage("Are you sure you want to save these changes?")
                .setPositiveButton("Yes", (dialog, which) -> updateUserData()) // Calls the actual save method
                .setNegativeButton("No", null)
                .show();
    }

    // ⭐ KEYBOARD HELPER METHOD ⭐
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ivProfilePic.setImageBitmap(bitmap);
                encodedImageString = encodeImage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String encodeImage(Bitmap bitmap) {
        int width = 300;
        int height = (int) (bitmap.getHeight() * ((float) width / bitmap.getWidth()));
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, width, height, false);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private Bitmap decodeImage(String encodedImage) {
        byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private void loadUserData() {
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("fullName");
                String phone = documentSnapshot.getString("phone");
                String address = documentSnapshot.getString("address");
                String role = documentSnapshot.getString("role");
                String imgStr = documentSnapshot.getString("profileImage");

                if(name != null) etName.setText(name);
                if(phone != null) etPhone.setText(phone);
                if(address != null) etAddress.setText(address);
                if(role != null) etRole.setText(role);

                if(imgStr != null && !imgStr.isEmpty()) {
                    ivProfilePic.setImageBitmap(decodeImage(imgStr));
                    encodedImageString = imgStr;
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUserData() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String role = etRole.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("phone", phone);
        updates.put("address", address);
        updates.put("role", role);

        if (!encodedImageString.isEmpty()) {
            updates.put("profileImage", encodedImageString);
        }

        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                    finish(); // Close Activity
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}