package com.example.project_ifloodguard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem; // Import needed
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomePageActivity extends AppCompatActivity {

    FrameLayout contentFrame;
    TextView tvHomeLevel, tvHomeStatus, tvHomeTime, tvHomeLocation;
    DatabaseReference dbRef;
    private static final String CHANNEL_ID = "flood_alert_channel";
    String userRole = "Staff";

    // ⭐ BROADCAST RECEIVER TO UPDATE BADGE INSTANTLY ⭐
    private final BroadcastReceiver badgeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateHistoryBadge();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        // Start Service
        Intent serviceIntent = new Intent(this, FloodMonitoringService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        if (getIntent().hasExtra("USER_ROLE")) {
            userRole = getIntent().getStringExtra("USER_ROLE");
        }

        createNotificationChannel();
        checkPermission();

        contentFrame = findViewById(R.id.content_frame);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Home");

        dbRef = FirebaseDatabase
                .getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("waterLevel");

        loadFragment(R.layout.content_home_page);
        refreshHomeViews();
        startFirebaseListener();
        setupBottomNavigation();

        // ⭐ HANDLE BACK BUTTON WITH CONFIRMATION ⭐
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmation();
            }
        });
    }

    // ⭐ REGISTER RECEIVER ON START ⭐
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("UPDATE_BADGE_EVENT");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(badgeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(badgeReceiver, filter);
        }
        updateHistoryBadge();
    }

    // ⭐ UNREGISTER ON STOP ⭐
    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(badgeReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.bottom_home);
        }
        updateHistoryBadge();
    }

    private void updateHistoryBadge() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            SharedPreferences prefs = getSharedPreferences("FloodGuardPrefs", MODE_PRIVATE);
            int count = prefs.getInt("unread_alert_count", 0);

            BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.bottom_history);

            if (count > 0) {
                badge.setVisible(true);
                badge.setNumber(count);
                badge.setBackgroundColor(Color.RED);
                badge.setBadgeTextColor(Color.WHITE);
            } else {
                badge.setVisible(false);
            }
        }
    }

    // ⭐ SETTINGS MENU LOGIC (ADDED BACK!) ⭐
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings_icon) {
            startActivity(new Intent(HomePageActivity.this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("No", null)
                .show();
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    moveTaskToBack(true);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void performLogout() {
        stopService(new Intent(this, FloodMonitoringService.class));
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.bottom_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.bottom_home) return true;
                else if (id == R.id.bottom_pps) {
                    Intent intent = new Intent(HomePageActivity.this, PPSListActivity.class);
                    intent.putExtra("USER_ROLE", userRole);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.bottom_history) {
                    Intent intent = new Intent(HomePageActivity.this, AlertHistoryActivity.class);
                    intent.putExtra("USER_ROLE", userRole);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

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

                    if (tvHomeStatus != null && status != null) {
                        tvHomeStatus.setText(status);
                        String formattedDist = String.format(Locale.US, "%.2f", distance);
                        tvHomeLevel.setText(formattedDist + " cm");

                        String currentTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                        tvHomeTime.setText("Updated: " + currentTime);

                        if (location != null) tvHomeLocation.setText(location);

                        // Find the Badge Background (The Pill)
                        android.graphics.drawable.GradientDrawable statusBg =
                                (android.graphics.drawable.GradientDrawable) tvHomeStatus.getBackground();

                        if (status.contains("DANGER")) {
                            // Text Colors
                            tvHomeLevel.setTextColor(ContextCompat.getColor(HomePageActivity.this, R.color.status_danger_text));

                            // Badge Color (Red)
                            statusBg.setColor(ContextCompat.getColor(HomePageActivity.this, R.color.status_danger_text));
                            tvHomeStatus.setText("DANGER ALERT"); // More urgent text

                        } else if (status.contains("WARNING")) {
                            // Text Colors
                            tvHomeLevel.setTextColor(ContextCompat.getColor(HomePageActivity.this, R.color.status_warning_text));

                            // Badge Color (Orange)
                            statusBg.setColor(ContextCompat.getColor(HomePageActivity.this, R.color.status_warning_text));
                            tvHomeStatus.setText("WARNING");

                        } else {
                            // Text Colors
                            tvHomeLevel.setTextColor(ContextCompat.getColor(HomePageActivity.this, R.color.brand_primary));

                            // Badge Color (Green)
                            statusBg.setColor(ContextCompat.getColor(HomePageActivity.this, R.color.status_normal_text));
                            tvHomeStatus.setText("NORMAL");
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void refreshHomeViews() {
        tvHomeLevel = findViewById(R.id.tvHomeLevel);
        tvHomeStatus = findViewById(R.id.tvHomeStatus);
        tvHomeTime = findViewById(R.id.tvHomeTime);
        tvHomeLocation = findViewById(R.id.tvHomeLocation);
        View btnContacts = findViewById(R.id.btnQuickContacts);
        TextView tvEmergencyHeader = findViewById(R.id.tvEmergencyHeader);

        // ⭐ SETUP INFO BUTTON ⭐
        View btnInfo = findViewById(R.id.btnLevelInfo);
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> showLevelInfoDialog());
        }

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

    // ⭐ NEW METHOD: SHOW DIALOG ⭐
    private void showLevelInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_level_info, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle "Got it" button
        View btnClose = view.findViewById(R.id.btnCloseDialog);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
    }

    private void loadFragment(int layoutResId) {
        contentFrame.removeAllViews();
        getLayoutInflater().inflate(layoutResId, contentFrame, true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Flood Alerts", NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }
}