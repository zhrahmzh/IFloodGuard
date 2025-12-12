package com.example.project_ifloodguard;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
    List<HistoryModel> fullList, displayedList;
    DatabaseReference dbRef;

    EditText searchInput;
    TextView tvEmpty;
    String userRole = "Staff"; // Default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_alert_history);

        // ⭐ 1. RESET BADGE COUNT TO 0 (Mark as Read) ⭐
        android.content.SharedPreferences prefs = getSharedPreferences("FloodGuardPrefs", MODE_PRIVATE);
        prefs.edit().putInt("unread_alert_count", 0).apply();

        if (getIntent().hasExtra("USER_ROLE")) {
            userRole = getIntent().getStringExtra("USER_ROLE");
        }

        // 2. Setup Firebase
        dbRef = FirebaseDatabase
                .getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("waterLevelHistory");

        // 3. Init UI
        tvEmpty = findViewById(R.id.tvEmptyHistory);
        searchInput = findViewById(R.id.etSearchHistory);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 4. Search Bar Logic (Keyboard & Touch)
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        searchInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (searchInput.getRight() - searchInput.getCompoundDrawables()[2].getBounds().width())) {
                    hideKeyboard();
                    return true;
                }
            }
            return false;
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 5. Setup List
        recyclerView = findViewById(R.id.recyclerHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fullList = new ArrayList<>();
        displayedList = new ArrayList<>();
        adapter = new HistoryAdapter(displayedList);
        recyclerView.setAdapter(adapter);

        loadHistory();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
    }

    private void loadHistory() {
        // Load last 50 records
        dbRef.limitToLast(50).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullList.clear();
                for (DataSnapshot shot : snapshot.getChildren()) {
                    Double dist = shot.child("distance").getValue(Double.class);
                    String status = shot.child("status").getValue(String.class);
                    Long time = shot.child("timestamp").getValue(Long.class);

                    if (dist != null && status != null) {
                        // Store the Key ID so we can delete it later
                        fullList.add(new HistoryModel(shot.getKey(), status, dist, time));
                    }
                }
                // Reverse list to show Newest First
                Collections.reverse(fullList);

                filter(searchInput.getText().toString());
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filter(String keyword) {
        displayedList.clear();
        String search = keyword.toLowerCase().trim();

        for (HistoryModel item : fullList) {
            // Convert Timestamp to readable date for searching
            String dateString = "";
            if (item.timestamp != null) {
                Date date = new Date(item.timestamp * 1000L);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                dateString = sdf.format(date).toLowerCase();
            }

            // Search by Status (DANGER) or Date
            if (item.status.toLowerCase().contains(search) || dateString.contains(search)) {
                displayedList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // --- DELETE CONFIRMATION ---
    private void showDeleteDialog(HistoryModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record?")
                .setMessage("Are you sure you want to delete this record?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    dbRef.child(model.id).removeValue();
                    Toast.makeText(this, "Record Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- MODEL ---
    public static class HistoryModel {
        String id; // Added ID for deletion
        String status;
        Double distance;
        Long timestamp;

        public HistoryModel(String id, String status, Double distance, Long timestamp) {
            this.id = id;
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

            if (item.timestamp != null) {
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

            // Role Logic: Only Admin can delete
            if ("Admin".equals(userRole)) {
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(v -> showDeleteDialog(item));
            } else {
                holder.btnDelete.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return data.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStatus, tvLevel, tvTime;
            View colorBar;
            ImageView btnDelete; // Added Delete Button
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvStatus = itemView.findViewById(R.id.tvHistoryStatus);
                tvLevel = itemView.findViewById(R.id.tvHistoryLevel);
                tvTime = itemView.findViewById(R.id.tvHistoryTime);
                colorBar = itemView.findViewById(R.id.viewStatusColor);
                btnDelete = itemView.findViewById(R.id.btnDeleteHistory); // Matches XML ID
            }
        }
    }
}