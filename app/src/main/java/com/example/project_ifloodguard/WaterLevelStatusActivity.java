package com.example.project_ifloodguard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WaterLevelStatusActivity extends AppCompatActivity {

    TextView tvDistance, tvStatus, tvLastUpdate;
    DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_water_level);

        // Match IDs with XML
        tvDistance = findViewById(R.id.txtWaterLevel);
        tvStatus = findViewById(R.id.txtStatus);
        tvLastUpdate = findViewById(R.id.txtTimestamp);

        // Firebase reference
        dbRef = FirebaseDatabase
                .getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("waterLevel");

        // Live updates
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    Double distance = snapshot.child("distance").getValue(Double.class);
                    String status = snapshot.child("status").getValue(String.class);
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                    // Prevent crash if null
                    if (distance != null)
                        tvDistance.setText("Water Level: " + distance + " cm");

                    if (status != null)
                        tvStatus.setText("Status: " + status);

                    if (timestamp != null)
                        tvLastUpdate.setText("Last Update: " + convertTime(timestamp));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvStatus.setText("Error: " + error.getMessage());
            }
        });
    }

    // Convert timestamp to Malaysia time format
    private String convertTime(long millis) {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
        return sdf.format(new java.util.Date(millis));
    }
}
