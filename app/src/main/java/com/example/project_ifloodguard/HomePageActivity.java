package com.example.project_ifloodguard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
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

    // --- UI Variables ---
    DrawerLayout drawerLayout;
    NavigationView navView;
    ActionBarDrawerToggle toggle;
    FrameLayout contentFrame;

    // --- Dashboard Variables ---
    TextView tvHomeLevel, tvHomeStatus, tvHomeTime, tvHomeLocation;

    // --- Firebase & Notifications ---
    DatabaseReference dbRef;
    private static final String CHANNEL_ID = "flood_alert_channel";
    private String lastKnownStatus = "NORMAL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        // 1. Setup Notification System
        createNotificationChannel();
        checkPermission();

        // 2. Setup Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        contentFrame = findViewById(R.id.content_frame);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Home");

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 3. Firebase Connection
        dbRef = FirebaseDatabase
                .getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("waterLevel");

        // 4. Load Home Page
        loadFragment(R.layout.content_home_page);
        refreshHomeViews();
        startFirebaseListener();

        // 5. Navigation Listener
        navView.setNavigationItemSelectedListener(item -> {
            handleNavigation(item);
            return true;
        });
    }

    // --- CORE LOGIC: LISTENING TO IOT DATA ---
    private void startFirebaseListener() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double distance = snapshot.child("distance").getValue(Double.class);
                    String status = snapshot.child("status").getValue(String.class);
                    String location = snapshot.child("location").getValue(String.class);

                    if (tvHomeStatus != null && status != null && distance != null) {
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

                        if(status.equalsIgnoreCase("DANGER") || status.contains("DANGER")) {
                            tvHomeStatus.setTextColor(Color.RED);
                        } else if(status.equalsIgnoreCase("WARNING")) {
                            tvHomeStatus.setTextColor(Color.parseColor("#FFA726"));
                        } else {
                            tvHomeStatus.setTextColor(Color.parseColor("#4CAF50"));
                        }
                    }

                    if (status != null) {
                        checkRiskAndNotify(status, distance);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- UI HELPER: CONNECTING BUTTONS ---
    private void refreshHomeViews() {
        tvHomeLevel = findViewById(R.id.tvHomeLevel);
        tvHomeStatus = findViewById(R.id.tvHomeStatus);
        tvHomeTime = findViewById(R.id.tvHomeTime);
        tvHomeLocation = findViewById(R.id.tvHomeLocation);

        try {
            View btnProfile = findViewById(R.id.btnQuickProfile);
            View btnPPS = findViewById(R.id.btnQuickPPS);
            View btnHistory = findViewById(R.id.btnQuickHistory);
            View btnContacts = findViewById(R.id.btnQuickContacts);
            View btnSOS = findViewById(R.id.btnSOS);

            if (btnProfile != null) {
                btnProfile.setOnClickListener(v -> {
                    getSupportActionBar().setTitle("Update Info");
                    loadFragment(R.layout.content_profile_update);
                    tvHomeStatus = null;
                });
            }
            if (btnPPS != null) {
                btnPPS.setOnClickListener(v -> {
                    // Start PPS Activity
                    Intent intent = new Intent(HomePageActivity.this, PPSListActivity.class);
                    startActivity(intent);
                });
            }
            if (btnHistory != null) {
                btnHistory.setOnClickListener(v -> {
                    // OLD CODE (Delete this):
                    // getSupportActionBar().setTitle("Alert History");
                    // loadFragment(R.layout.content_alert_history);
                    // tvHomeStatus = null;

                    // NEW CODE (Use this):
                    Intent intent = new Intent(HomePageActivity.this, AlertHistoryActivity.class);
                    startActivity(intent);
                });
            }
            if (btnContacts != null) {
                btnContacts.setOnClickListener(v -> {
                    Intent intent = new Intent(HomePageActivity.this, EmergencyContactActivity.class);
                    startActivity(intent);
                });
            }
            if (btnSOS != null) {
                btnSOS.setOnClickListener(v -> {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                    dialIntent.setData(Uri.parse("tel:999"));
                    startActivity(dialIntent);
                });
            }
        } catch (Exception e) {}
    }

    // --- NAVIGATION DRAWER LOGIC ---
    private void handleNavigation(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            getSupportActionBar().setTitle("Home");
            loadFragment(R.layout.content_home_page);
            refreshHomeViews();

        } else if (id == R.id.nav_sensor) {
            startActivity(new Intent(this, WaterLevelStatusActivity.class));

        } else if (id == R.id.nav_alerts) {
            // OLD CODE (Delete this):
            // getSupportActionBar().setTitle("Alert History");
            // loadFragment(R.layout.content_alert_history);
            // tvHomeStatus = null;

            // NEW CODE (Use this):
            Intent intent = new Intent(this, AlertHistoryActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_pps) {
            // Start PPS Activity
            Intent intent = new Intent(this, PPSListActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_contacts) {
            Intent intent = new Intent(this, EmergencyContactActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_about) {
            getSupportActionBar().setTitle("Profile");
            loadFragment(R.layout.content_profile_update);
            tvHomeStatus = null;

        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
        drawerLayout.closeDrawers();
    }

    // --- HELPER FUNCTIONS ---
    private void loadFragment(int layoutResId) {
        contentFrame.removeAllViews();
        getLayoutInflater().inflate(layoutResId, contentFrame, true);
    }

    private void checkRiskAndNotify(String currentStatus, Double distance) {
        if (!currentStatus.equals(lastKnownStatus)) {
            String formattedDist = String.format(Locale.US, "%.2f", distance);
            if (currentStatus.contains("DANGER")) {
                sendNotification("🚨 FLOOD DANGER!", "Water Level Critical: " + formattedDist + "cm. Evacuate!");
            } else if (currentStatus.contains("WARNING")) {
                sendNotification("⚠️ Flood Warning", "Water Level Rising: " + formattedDist + "cm.");
            }
            lastKnownStatus = currentStatus;
        }
    }

    private void sendNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(1, builder.build());
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