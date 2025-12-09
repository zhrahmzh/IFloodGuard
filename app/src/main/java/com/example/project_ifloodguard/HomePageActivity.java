package com.example.project_ifloodguard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomePageActivity extends AppCompatActivity {

    // --- UI Variables ---
    FrameLayout contentFrame;
    TextView tvHomeLevel, tvHomeStatus, tvHomeTime, tvHomeLocation;

    // --- Firebase ---
    DatabaseReference dbRef;

    // --- User Role ---
    String userRole = "Staff"; // Default to Staff for safety

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        // ⭐ START BACKGROUND SERVICE ⭐
        // The Service handles Notifications & History now.
        Intent serviceIntent = new Intent(this, FloodMonitoringService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // 1. Get Role from Login Activity
        if (getIntent().hasExtra("USER_ROLE")) {
            userRole = getIntent().getStringExtra("USER_ROLE");
        }

        // 2. Setup Toolbar
        contentFrame = findViewById(R.id.content_frame);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Home");

        // 3. Firebase Connection (Realtime Database for IoT Data)
        dbRef = FirebaseDatabase
                .getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("waterLevel");

        // 4. Load Layout & Views
        loadFragment(R.layout.content_home_page);
        refreshHomeViews();
        startFirebaseListener(); // This now ONLY updates the UI

        // 5. SETUP BOTTOM NAVIGATION
        setupBottomNavigation();

        // 6. HANDLE BACK BUTTON
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showLogoutConfirmation();
            }
        });
    } // <--- ✅ CORRECT CLOSING BRACE LOCATION

    // --- LOGOUT CONFIRMATION POPUP ---
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    // --- PERFORM LOGOUT LOGIC ---
    private void performLogout() {
        // Stop the background service when logging out (Optional, but good practice)
        stopService(new Intent(this, FloodMonitoringService.class));

        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // --- BOTTOM NAVIGATION LOGIC ---
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.bottom_home);

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.bottom_home) {
                    return true;
                }
                else if (id == R.id.bottom_pps) {
                    Intent intent = new Intent(HomePageActivity.this, PPSListActivity.class);
                    intent.putExtra("USER_ROLE", userRole); // Pass Role
                    startActivity(intent);
                    return true;
                }
                else if (id == R.id.bottom_history) {
                    Intent intent = new Intent(HomePageActivity.this, AlertHistoryActivity.class);
                    intent.putExtra("USER_ROLE", userRole); // Pass Role
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    // --- TOOLBAR SETTINGS MENU LOGIC ---
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings_icon) {
            Intent intent = new Intent(HomePageActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- FIREBASE LISTENER (UI UPDATES ONLY) ---
    private void startFirebaseListener() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Object distObj = snapshot.child("distance").getValue();
                    Double distance = 0.0;
                    if (distObj instanceof Long) distance = ((Long) distObj).doubleValue();
                    else if (distObj instanceof Double) distance = (Double) distObj;

                    String status = snapshot.child("status").getValue(String.class);
                    String location = snapshot.child("location").getValue(String.class);

                    // Update UI Elements
                    if (tvHomeStatus != null && status != null) {
                        tvHomeStatus.setText(status);
                        String formattedDist = String.format(Locale.US, "%.2f", distance);
                        tvHomeLevel.setText(formattedDist + " cm");

                        String currentTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                        tvHomeTime.setText("Updated: " + currentTime);

                        if (location != null && !location.isEmpty()) {
                            tvHomeLocation.setText(location);
                        } else {
                            tvHomeLocation.setText("Location Unknown");
                        }

                        // Colors
                        View layoutStatus = (View) tvHomeStatus.getParent();
                        if (status.contains("DANGER")) {
                            tvHomeStatus.setTextColor(ContextCompat.getColor(HomePageActivity.this, R.color.status_danger_red));
                            tvHomeLevel.setTextColor(ContextCompat.getColor(HomePageActivity.this, R.color.status_danger_red));
                            if(layoutStatus != null) layoutStatus.setBackgroundColor(ContextCompat.getColor(HomePageActivity.this, R.color.status_danger_bg));
                        } else if (status.contains("WARNING")) {
                            tvHomeStatus.setTextColor(ContextCompat.getColor(HomePageActivity.this, R.color.status_warning_yellow));
                            tvHomeLevel.setTextColor(ContextCompat.getColor(HomePageActivity.this, R.color.status_warning_yellow));
                            if(layoutStatus != null) layoutStatus.setBackgroundColor(ContextCompat.getColor(HomePageActivity.this, R.color.status_warning_bg));
                        } else {
                            tvHomeStatus.setTextColor(ContextCompat.getColor(HomePageActivity.this, R.color.status_normal_green));
                            tvHomeLevel.setTextColor(ContextCompat.getColor(HomePageActivity.this, R.color.brand_blue));
                            if(layoutStatus != null) layoutStatus.setBackgroundColor(ContextCompat.getColor(HomePageActivity.this, R.color.status_normal_bg));
                        }
                    }

                    // ⚠️ REMOVED "checkRiskAndNotify" TO PREVENT DOUBLE ALERTS
                    // The Background Service handles the Notification & History Saving now.
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.bottom_home);
        }
    }

    private void refreshHomeViews() {
        tvHomeLevel = findViewById(R.id.tvHomeLevel);
        tvHomeStatus = findViewById(R.id.tvHomeStatus);
        tvHomeTime = findViewById(R.id.tvHomeTime);
        tvHomeLocation = findViewById(R.id.tvHomeLocation);

        View btnContacts = findViewById(R.id.btnQuickContacts);
        TextView tvEmergencyHeader = findViewById(R.id.tvEmergencyHeader);

        if (btnContacts != null) {
            if ("Staff".equals(userRole)) {
                btnContacts.setVisibility(View.GONE);
                if (tvEmergencyHeader != null) tvEmergencyHeader.setVisibility(View.GONE);
            } else {
                btnContacts.setVisibility(View.VISIBLE);
                if (tvEmergencyHeader != null) tvEmergencyHeader.setVisibility(View.VISIBLE);
                btnContacts.setOnClickListener(v -> {
                    Intent intent = new Intent(HomePageActivity.this, EmergencyContactActivity.class);
                    intent.putExtra("USER_ROLE", userRole);
                    startActivity(intent);
                });
            }
        }
    }

    private void loadFragment(int layoutResId) {
        contentFrame.removeAllViews();
        getLayoutInflater().inflate(layoutResId, contentFrame, true);
    }
}