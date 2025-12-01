package com.example.project_ifloodguard;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PPSListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    PPSAdapter adapter;
    List<PPSModel> ppsList;
    DatabaseReference dbRef;
    TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_pps_list);

        // Inside onCreate...

        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            finish(); // This closes the current page and goes back to Home
        });

        // 1. Setup Firebase
        dbRef = FirebaseDatabase
                .getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("pps_list");

        // 2. Setup UI
        recyclerView = findViewById(R.id.recyclerViewPPS);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvEmpty = findViewById(R.id.tvEmpty);

        ppsList = new ArrayList<>();
        adapter = new PPSAdapter(ppsList);
        recyclerView.setAdapter(adapter);

        // 3. Setup "Add New" Button
        FloatingActionButton fab = findViewById(R.id.fabAddPPS);
        fab.setOnClickListener(v -> showUpdateDialog(null));

        // 4. Load Data
        loadPPSData();
    }

    private void loadPPSData() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ppsList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String id = data.getKey();
                    String name = data.child("name").getValue(String.class);
                    String capacity = data.child("capacity").getValue(String.class);

                    // Safe read for integer
                    Integer current = data.child("current_occupancy").getValue(Integer.class);
                    if (current == null) current = 0;

                    if (name != null) {
                        ppsList.add(new PPSModel(id, name, capacity, current));
                    }
                }
                adapter.notifyDataSetChanged();
                if (ppsList.isEmpty()) tvEmpty.setVisibility(View.VISIBLE);
                else tvEmpty.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PPSListActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- DIALOG FOR ADD / EDIT ---
    private void showUpdateDialog(PPSModel model) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(model == null ? "Tambah PPS Baru" : "Kemaskini Data PPS");

        // Create Layout Programmatically with LABELS to avoid confusion
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // --- FIELD 1: NAMA ---
        TextView lblName = new TextView(this);
        lblName.setText("Nama Pusat Pemindahan");
        lblName.setTypeface(null, Typeface.BOLD);
        lblName.setTextColor(Color.parseColor("#1565C0"));
        layout.addView(lblName);

        final EditText inputName = new EditText(this);
        inputName.setHint("Contoh: SK Seksyen 7");
        if(model != null) inputName.setText(model.name);
        layout.addView(inputName);

        // Spacer
        layout.addView(new View(this), new android.widget.LinearLayout.LayoutParams(1, 30));

        // --- FIELD 2: KAPASITI (ORANG) ---
        TextView lblCap = new TextView(this);
        lblCap.setText("Kapasiti Maksimum (Bil. Orang)");
        lblCap.setTypeface(null, Typeface.BOLD);
        layout.addView(lblCap);

        final EditText inputCapacity = new EditText(this);
        inputCapacity.setHint("Contoh: 300");
        inputCapacity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if(model != null) inputCapacity.setText(model.capacity);
        layout.addView(inputCapacity);

        // Spacer
        layout.addView(new View(this), new android.widget.LinearLayout.LayoutParams(1, 30));

        // --- FIELD 3: MANGSA SEMASA (ORANG) ---
        TextView lblCur = new TextView(this);
        lblCur.setText("Mangsa Semasa (Bil. Orang)");
        lblCur.setTypeface(null, Typeface.BOLD);
        lblCur.setTextColor(Color.parseColor("#F57C00")); // Orange color for emphasis
        layout.addView(lblCur);

        final EditText inputCurrent = new EditText(this);
        inputCurrent.setHint("Contoh: 150");
        inputCurrent.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if(model != null) inputCurrent.setText(String.valueOf(model.currentOccupancy));
        layout.addView(inputCurrent);

        builder.setView(layout);

        // Save Button Logic
        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            String cap = inputCapacity.getText().toString().trim();
            String curStr = inputCurrent.getText().toString().trim();

            if (name.isEmpty() || cap.isEmpty()) {
                Toast.makeText(PPSListActivity.this, "Sila isi Nama & Kapasiti!", Toast.LENGTH_SHORT).show();
                return;
            }

            int currentVal = curStr.isEmpty() ? 0 : Integer.parseInt(curStr);

            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("capacity", cap);
            map.put("current_occupancy", currentVal);

            if (model == null) {
                // ADD NEW
                dbRef.push().setValue(map);
            } else {
                // UPDATE EXISTING
                dbRef.child(model.id).updateChildren(map);
            }
        });

        // Delete Button
        if (model != null) {
            builder.setNeutralButton("Padam", (dialog, which) -> {
                new AlertDialog.Builder(this)
                        .setTitle("Padam PPS?")
                        .setMessage("Adakah anda pasti mahu memadam pusat ini?")
                        .setPositiveButton("Ya", (d, w) -> dbRef.child(model.id).removeValue())
                        .setNegativeButton("Tidak", null)
                        .show();
            });
        }

        builder.setNegativeButton("Batal", null);
        builder.show();
    }

    // --- MODEL CLASS ---
    public static class PPSModel {
        String id, name, capacity;
        int currentOccupancy;

        public PPSModel(String id, String name, String capacity, int currentOccupancy) {
            this.id = id;
            this.name = name;
            this.capacity = capacity;
            this.currentOccupancy = currentOccupancy;
        }
    }

    // --- ADAPTER CLASS ---
    class PPSAdapter extends RecyclerView.Adapter<PPSAdapter.PPSViewHolder> {
        List<PPSModel> list;
        public PPSAdapter(List<PPSModel> list) { this.list = list; }

        @NonNull
        @Override
        public PPSViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_pps, parent, false);
            return new PPSViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PPSViewHolder holder, int position) {
            PPSModel item = list.get(position);

            holder.tvName.setText(item.name);

            // Calculate Percentage
            int current = item.currentOccupancy;
            int max = 1;
            try {
                max = Integer.parseInt(item.capacity);
            } catch (Exception e) { max = 1; }

            int percent = (int)((current / (float)max) * 100);
            if(percent > 100) percent = 100; // Cap at 100%

            // Update UI Texts
            holder.tvCapacity.setText("Kapasiti: " + current + " / " + max + " orang");
            holder.tvPercent.setText(percent + "% Penuh");
            holder.progressBar.setProgress(percent);

            // Change Color based on fullness
            if (percent >= 90) {
                holder.tvPercent.setTextColor(Color.RED);
            } else {
                holder.tvPercent.setTextColor(Color.parseColor("#F57C00")); // Orange
            }

            // Click to Edit
            holder.itemView.setOnClickListener(v -> showUpdateDialog(item));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class PPSViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvCapacity, tvPercent;
            ProgressBar progressBar;

            public PPSViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvPPSName);
                tvCapacity = itemView.findViewById(R.id.tvCapacity);
                tvPercent = itemView.findViewById(R.id.tvPercent);
                progressBar = itemView.findViewById(R.id.progressCapacity);
            }
        }
    }
}