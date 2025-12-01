package com.example.project_ifloodguard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Random;

public class ManageUsersActivity extends AppCompatActivity {

    EditText etName, etEmail;
    Spinner spinnerRole;
    TextView tvPass;
    Button btnCreate;

    DatabaseReference usersDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_manage_users);

        etName = findViewById(R.id.etNewName);
        etEmail = findViewById(R.id.etNewEmail);
        spinnerRole = findViewById(R.id.spinnerRole);
        tvPass = findViewById(R.id.tvGeneratedPass);
        btnCreate = findViewById(R.id.btnCreateUser);

        // Setup Role Spinner
        String[] roles = {"Staff", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        spinnerRole.setAdapter(adapter);

        // Main DB Reference (Admin's connection)
        usersDB = FirebaseDatabase
                .getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users");

        btnCreate.setOnClickListener(v -> createStaffAccount());
    }

    private void createStaffAccount() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        if(name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Generate Random Password (e.g., Temp1234)
        int randomNum = new Random().nextInt(9000) + 1000;
        String tempPassword = "Temp" + randomNum;
        tvPass.setText("Generated: " + tempPassword);

        // 2. Create User using SECONDARY Auth (To avoid logging out Admin)
        createFirebaseUser(name, email, tempPassword, role);
    }

    private void createFirebaseUser(String name, String email, String password, String role) {
        Toast.makeText(this, "Creating User...", Toast.LENGTH_SHORT).show();

        // --- TRICK: Initialize a Secondary App ---
        FirebaseOptions options = FirebaseApp.getInstance().getOptions(); // Copy main options
        FirebaseApp secondaryApp;

        try {
            secondaryApp = FirebaseApp.initializeApp(this, options, "Secondary");
        } catch (IllegalStateException e) {
            // Already initialized
            secondaryApp = FirebaseApp.getInstance("Secondary");
        }

        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);

        // Create the user on the secondary connection
        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser newUser = task.getResult().getUser();
                        if (newUser != null) {
                            saveUserToDB(newUser.getUid(), name, email, role, password);
                        }
                        // Logout the secondary auth immediately so it doesn't interfere
                        secondaryAuth.signOut();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDB(String uid, String name, String email, String role, String password) {
        // We use the MAIN database reference (Admin is still logged in here)
        HashMap<String, Object> map = new HashMap<>();
        map.put("fullName", name);
        map.put("email", email);
        map.put("role", role);
        map.put("phone", "-"); // Placeholder

        usersDB.child(uid).setValue(map)
                .addOnSuccessListener(aVoid -> {
                    // 3. Open Email App
                    sendEmailToStaff(name, email, password);
                });
    }

    private void sendEmailToStaff(String name, String email, String password) {
        String subject = "Your FloodGuard App Credentials";
        String body = "Hello " + name + ",\n\n" +
                "An account has been created for you.\n\n" +
                "Email: " + email + "\n" +
                "Password: " + password + "\n\n" +
                "Please login and update your profile immediately.\n\n" +
                "- Admin Team";

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // Only email apps
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(Intent.createChooser(intent, "Send Email via..."));
            finish(); // Close this page
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email app installed.", Toast.LENGTH_SHORT).show();
        }
    }
}