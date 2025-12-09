package com.example.project_ifloodguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences; // Import SharedPreferences
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FloodMonitoringService extends Service {

    private DatabaseReference dbRef;
    private DatabaseReference historyRef;

    // We will load this from memory now!
    private String lastKnownStatus = "NORMAL";

    private static final String CHANNEL_ID = "flood_service_channel";
    private static final String ALERT_CHANNEL_ID = "flood_alert_channel";
    private SharedPreferences prefs; // Variable to access phone memory

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Setup Firebase Refs
        dbRef = FirebaseDatabase.getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("waterLevel");

        historyRef = FirebaseDatabase.getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("waterLevelHistory");

        // ⭐ 2. LOAD LAST STATUS FROM MEMORY ⭐
        // This prevents the app from "forgetting" the status when restarted.
        prefs = getSharedPreferences("FloodGuardPrefs", MODE_PRIVATE);
        lastKnownStatus = prefs.getString("lastStatus", "NORMAL");

        // 3. Start Listening
        startFirebaseListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, HomePageActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("IFloodGuard Active")
                .setContentText("Monitoring flood levels in background...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        return START_STICKY;
    }

    private void startFirebaseListener() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    Double distance = 0.0;
                    Object distObj = snapshot.child("distance").getValue();
                    if (distObj instanceof Long) distance = ((Long) distObj).doubleValue();
                    else if (distObj instanceof Double) distance = (Double) distObj;

                    if (status != null) {
                        checkRiskAndNotify(status, distance);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkRiskAndNotify(String currentStatus, Double distance) {
        // Only trigger if status has CHANGED from what we remember
        if (!currentStatus.equals(lastKnownStatus)) {

            String formattedDist = String.format(Locale.US, "%.2f", distance);

            if (currentStatus.contains("DANGER")) {
                sendAlertNotification("🚨 FLOOD DANGER!", "Level: " + formattedDist + "cm. Evacuate!");
                saveToHistory(currentStatus, distance);
            } else if (currentStatus.contains("WARNING")) {
                sendAlertNotification("⚠️ Flood Warning", "Level: " + formattedDist + "cm.");
                saveToHistory(currentStatus, distance);
            }

            // ⭐ UPDATE MEMORY ⭐
            lastKnownStatus = currentStatus;
            prefs.edit().putString("lastStatus", currentStatus).apply();
        }
    }

    private void sendAlertNotification(String title, String message) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create Channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(ALERT_CHANNEL_ID, "Flood Alerts", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)

                // ⭐ 1. FIXED NAME: Shows "IFloodGuard" clearly ⭐
                .setContentTitle("IFloodGuard")

                // ⭐ 2. BIGGER BOX: This style makes the notification expandable ⭐
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title) // The warning (e.g., "🚨 FLOOD DANGER!")
                        .bigText(message))         // The message becomes larger and readable

                .setContentText(message) // Fallback for small screens
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(2, builder.build());
    }

    private void saveToHistory(String status, Double distance) {
        String pushId = historyRef.push().getKey();
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("distance", distance);
        map.put("timestamp", System.currentTimeMillis() / 1000);
        if (pushId != null) historyRef.child(pushId).setValue(map);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Background Monitor", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}