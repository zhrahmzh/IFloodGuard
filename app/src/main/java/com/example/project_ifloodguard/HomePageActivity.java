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

    // --- User Role ---
    String userRole = "Staff"; // Default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        // 1. Get Role
        if (getIntent().hasExtra("USER_ROLE")) {
            userRole = getIntent().getStringExtra("USER_ROLE");
        }

        // 2. Setup Notifications
        createNotificationChannel();
        checkPermission();

        // 3. Setup Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        contentFrame = findViewById(R.id.content_frame);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Home");

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 4. Firebase Connection
        dbRef = FirebaseDatabase
                .getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("waterLevel");

        // 5. Load Layout
        loadFragment(R.layout.content_home_page);

        // 6. Connect Buttons & Apply Role Rules
        refreshHomeViews();

        // 7. Start Listening
        startFirebaseListener();

        // 8. Navigation Listener
        navView.setNavigationItemSelectedListener(item -> {
            handleNavigation(item);
            return true;
        });
    }

    // --- LISTENING TO IOT DATA ---
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

                        if (location != null && !location.isEmpty()) {
                            tvHomeLocation.setText(location);
                        } else {
                            tvHomeLocation.setText("Location Unknown");
                        }

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
                    if (status != null) {
                        checkRiskAndNotify(status, distance);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- UI HELPER: CONNECTING BUTTONS & RESTRICTING STAFF ---
    private void refreshHomeViews() {
        tvHomeLevel = findViewById(R.id.tvHomeLevel);
        tvHomeStatus = findViewById(R.id.tvHomeStatus);
        tvHomeTime = findViewById(R.id.tvHomeTime);
        tvHomeLocation = findViewById(R.id.tvHomeLocation);

        View btnPPS = findViewById(R.id.btnQuickPPS);
        View btnContacts = findViewById(R.id.btnQuickContacts);
        View btnHistory = findViewById(R.id.btnQuickHistory);

        // --- NEW LOGIC: HIDE BUTTONS FOR STAFF ---
        if (userRole.equalsIgnoreCase("Staff")) {
            // Staff sees ONLY History
            if (btnPPS != null) btnPPS.setVisibility(View.GONE);
            if (btnContacts != null) btnContacts.setVisibility(View.GONE);
            if (btnHistory != null) btnHistory.setVisibility(View.VISIBLE);
        } else {
            // Admin sees EVERYTHING
            if (btnPPS != null) btnPPS.setVisibility(View.VISIBLE);
            if (btnContacts != null) btnContacts.setVisibility(View.VISIBLE);
            if (btnHistory != null) btnHistory.setVisibility(View.VISIBLE);
        }

        // Set Click Listeners
        if (btnPPS != null) {
            btnPPS.setOnClickListener(v -> {
                Intent intent = new Intent(HomePageActivity.this, PPSListActivity.class);
                intent.putExtra("USER_ROLE", userRole);
                startActivity(intent);
            });
        }

        if (btnContacts != null) {
            btnContacts.setOnClickListener(v -> {
                Intent intent = new Intent(HomePageActivity.this, EmergencyContactActivity.class);
                intent.putExtra("USER_ROLE", userRole);
                startActivity(intent);
            });
        }

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                Intent intent = new Intent(HomePageActivity.this, AlertHistoryActivity.class);
                intent.putExtra("USER_ROLE", userRole);
                startActivity(intent);
            });
        }
    }

    private void handleNavigation(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            getSupportActionBar().setTitle("Home");
            loadFragment(R.layout.content_home_page);
            refreshHomeViews();

        } else if (id == R.id.nav_profile) {
            getSupportActionBar().setTitle("My Profile");
            Intent intent = new Intent(this, ProfileUpdateActivity.class);
            intent.putExtra("USER_ROLE", userRole);
            startActivity(intent);

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

    private void loadFragment(int layoutResId) {
        contentFrame.removeAllViews();
        getLayoutInflater().inflate(layoutResId, contentFrame, true);
    }

    private void checkRiskAndNotify(String currentStatus, Double distance) {
        if (!currentStatus.equals(lastKnownStatus)) {
            String formattedDist = String.format(Locale.US, "%.2f", distance);
            if (currentStatus.contains("DANGER")) {
                sendNotification("🚨 FLOOD DANGER!", "Level: " + formattedDist + "cm. Evacuate!");
            } else if (currentStatus.contains("WARNING")) {
                sendNotification("⚠️ Flood Warning", "Level: " + formattedDist + "cm.");
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