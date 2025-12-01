package com.example.project_ifloodguard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertHistoryActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    HistoryAdapter adapter;
    List<HistoryModel> list;
    DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_alert_history);
        // Inside onCreate...

        // 1. Find the back button
        android.widget.ImageView btnBack = findViewById(R.id.btnBack);

        // 2. Set the click listener to close the page
        btnBack.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();
        adapter = new HistoryAdapter(list);
        recyclerView.setAdapter(adapter);

        // Connect to "waterLevelHistory" node
        dbRef = FirebaseDatabase
                .getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("waterLevelHistory");

        // Load last 50 records
        dbRef.limitToLast(50).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot shot : snapshot.getChildren()) {
                    Double dist = shot.child("distance").getValue(Double.class);
                    String status = shot.child("status").getValue(String.class);
                    Long time = shot.child("timestamp").getValue(Long.class);

                    if (dist != null && status != null) {
                        list.add(new HistoryModel(status, dist, time));
                    }
                }
                // Reverse list to show newest first
                Collections.reverse(list);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- DATA MODEL ---
    public static class HistoryModel {
        String status;
        Double distance;
        Long timestamp;

        public HistoryModel(String status, Double distance, Long timestamp) {
            this.status = status;
            this.distance = distance;
            this.timestamp = timestamp;
        }
    }

    // --- ADAPTER ---
    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        List<HistoryModel> data;
        public HistoryAdapter(List<HistoryModel> data) { this.data = data; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert_history, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryModel item = data.get(position);

            holder.tvStatus.setText(item.status);
            holder.tvLevel.setText(String.format(Locale.US, "%.2f cm", item.distance));

            // Inside onBindViewHolder
            if (item.timestamp != null) {
                // FIX: Multiply by 1000L (L ensures it handles the math as Long)
                Date date = new Date(item.timestamp * 1000L);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
                holder.tvTime.setText(sdf.format(date));
            } else {
                holder.tvTime.setText("--:--");
            }

            // Color Coding
            if (item.status.contains("DANGER")) {
                holder.colorBar.setBackgroundColor(Color.RED);
                holder.tvStatus.setTextColor(Color.RED);
            } else if (item.status.contains("WARNING")) {
                holder.colorBar.setBackgroundColor(Color.parseColor("#FFA726")); // Orange
                holder.tvStatus.setTextColor(Color.parseColor("#FFA726"));
            } else {
                holder.colorBar.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            }
        }

        @Override
        public int getItemCount() { return data.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStatus, tvLevel, tvTime;
            View colorBar;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvStatus = itemView.findViewById(R.id.tvHistoryStatus);
                tvLevel = itemView.findViewById(R.id.tvHistoryLevel);
                tvTime = itemView.findViewById(R.id.tvHistoryTime);
                colorBar = itemView.findViewById(R.id.viewStatusColor);
            }
        }
    }
}