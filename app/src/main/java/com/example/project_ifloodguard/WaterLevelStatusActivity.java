package com.example.project_ifloodguard;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WaterLevelStatusActivity extends AppCompatActivity {

    TextView txtWaterLevel, txtStatus, txtTimestamp;
    DatabaseReference sensorRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_water_level);

        txtWaterLevel = findViewById(R.id.txtWaterLevel);
        txtStatus = findViewById(R.id.txtStatus);
        txtTimestamp = findViewById(R.id.txtTimestamp);

        // Firebase reference
        sensorRef = FirebaseDatabase.getInstance()
                .getReference("sensorData");

        // Real-time listener
        sensorRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String waterLevel = snapshot.child("waterLevel").getValue().toString();
                    String status = snapshot.child("status").getValue().toString();
                    String timestamp = snapshot.child("timestamp").getValue().toString();

                    txtWaterLevel.setText("Water Level: " + waterLevel + " cm");
                    txtStatus.setText("Status: " + status);
                    txtTimestamp.setText("Last updated: " + timestamp);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }
}
