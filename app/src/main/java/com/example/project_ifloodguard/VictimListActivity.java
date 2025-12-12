package com.example.project_ifloodguard;

import android.content.ContentValues;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VictimListActivity extends AppCompatActivity {

    String centerId, centerName;
    String userRole = "Staff"; // Default role

    DatabaseReference dbVictims;
    DatabaseReference dbCenterCount;

    RecyclerView recyclerView;
    VictimAdapter adapter;
    List<VictimModel> fullList, displayedList;
    EditText searchInput;
    TextView tvEmpty, tvTitle;
    ImageView btnExport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_victim_list);

        // 1. Get Data & Role
        if (getIntent().hasExtra("CENTER_ID")) {
            centerId = getIntent().getStringExtra("CENTER_ID");
            centerName = getIntent().getStringExtra("CENTER_NAME");
        }
        if (getIntent().hasExtra("USER_ROLE")) {
            userRole = getIntent().getStringExtra("USER_ROLE");
        }

        // 2. Setup Firebase
        dbVictims = FirebaseDatabase.getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("pps_list").child(centerId).child("victims");

        dbCenterCount = FirebaseDatabase.getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("pps_list").child(centerId).child("current_occupancy");

        // 3. Init UI
        tvTitle = findViewById(R.id.tvCenterTitle);
        tvTitle.setText(centerName);
        tvEmpty = findViewById(R.id.tvEmptyVictims);
        searchInput = findViewById(R.id.etSearchVictim);
        btnExport = findViewById(R.id.btnExport);
        ImageView btnBack = findViewById(R.id.btnBack);
        FloatingActionButton fab = findViewById(R.id.fabAddVictim);

        // ⭐ 4. PERMISSION LOGIC (Fixed) ⭐
        if ("Admin".equals(userRole)) {
            // ADMIN:
            // 1. Can Download (Show Button)
            btnExport.setVisibility(View.VISIBLE);
            btnExport.setOnClickListener(v -> showExportDialog());

            // 2. CANNOT Add Victims (Hide FAB)
            fab.setVisibility(View.GONE);
        } else {
            // STAFF:
            // 1. Cannot Download (Hide Button)
            btnExport.setVisibility(View.GONE);

            // 2. Can Add Victims (Show FAB)
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> showAddVictimDialog());
        }

        // Search Logic
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

        btnBack.setOnClickListener(v -> finish());

        // Setup List
        recyclerView = findViewById(R.id.recyclerVictims);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fullList = new ArrayList<>();
        displayedList = new ArrayList<>();
        adapter = new VictimAdapter(displayedList);
        recyclerView.setAdapter(adapter);

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

        loadVictims();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
    }

    private void loadVictims() {
        dbVictims.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullList.clear();
                int activeCount = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    String name = data.child("name").getValue(String.class);
                    String ic = data.child("ic").getValue(String.class);
                    String status = data.child("status").getValue(String.class);

                    if (status == null) status = "Active";

                    if (name != null) {
                        fullList.add(new VictimModel(data.getKey(), name, ic, status));
                        if (status.equals("Active")) {
                            activeCount++;
                        }
                    }
                }

                // Sort A-Z
                java.util.Collections.sort(fullList, (a, b) -> a.name.compareToIgnoreCase(b.name));

                // Update count and UI
                dbCenterCount.setValue(activeCount);
                filter(searchInput.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filter(String keyword) {
        displayedList.clear();
        String search = keyword.toLowerCase().trim();
        for (VictimModel item : fullList) {
            if (item.name.toLowerCase().contains(search) || item.ic.contains(search)) {
                displayedList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showExportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Export Victim List")
                .setMessage("Download list as CSV file?")
                .setPositiveButton("Download", (dialog, which) -> exportToCSV())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void exportToCSV() {
        if (fullList.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvData = new StringBuilder();
        csvData.append("Name,IC Number,Status\n");

        for (VictimModel victim : fullList) {
            csvData.append(victim.name).append(",'")
                    .append(victim.ic).append(",")
                    .append(victim.status).append("\n");
        }

        try {
            String fileName = "VictimList_" + centerName.replaceAll(" ", "_") + "_" + System.currentTimeMillis() + ".csv";
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    outputStream.write(csvData.toString().getBytes());
                    outputStream.close();
                    Toast.makeText(this, "Saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Export Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddVictimDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Register New Victim");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("Full Name");
        layout.addView(inputName);

        final EditText inputIC = new EditText(this);
        inputIC.setHint("IC Number (12 Digits)");
        inputIC.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputIC.setFilters(new InputFilter[] { new InputFilter.LengthFilter(12) });
        layout.addView(inputIC);

        builder.setView(layout);
        builder.setPositiveButton("Register", null);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        android.widget.Button btnRegister = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnRegister.setEnabled(false);
        btnRegister.setTextColor(Color.GRAY);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String name = inputName.getText().toString().trim();
                String ic = inputIC.getText().toString().trim();
                if (!name.isEmpty() && ic.length() == 12) {
                    btnRegister.setEnabled(true);
                    btnRegister.setTextColor(Color.parseColor("#1565C0"));
                } else {
                    btnRegister.setEnabled(false);
                    btnRegister.setTextColor(Color.GRAY);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        inputName.addTextChangedListener(watcher);
        inputIC.addTextChangedListener(watcher);

        btnRegister.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String ic = inputIC.getText().toString().trim();
            if (name.isEmpty() || ic.length() != 12) return;
            dialog.dismiss();
            saveVictimToFirebase(name, ic);
        });
    }

    private void saveVictimToFirebase(String name, String ic) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("ic", ic);
        map.put("status", "Active");
        dbVictims.push().setValue(map)
                .addOnSuccessListener(a -> Toast.makeText(this, "Victim Registered", Toast.LENGTH_SHORT).show());
    }

    private void showCheckOutDialog(VictimModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Return Home")
                .setMessage("Has " + model.name + " returned home?")
                .setPositiveButton("Yes, Returned", (dialog, which) -> {
                    dbVictims.child(model.id).child("status").setValue("Returned")
                            .addOnSuccessListener(a -> Toast.makeText(this, "Status Updated", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirm(VictimModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record")
                .setMessage("Delete " + model.name + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbVictims.child(model.id).removeValue();
                    Toast.makeText(this, "Record Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- MODEL ---
    class VictimModel {
        String id, name, ic, status;
        public VictimModel(String id, String name, String ic, String status) {
            this.id = id; this.name = name; this.ic = ic; this.status = status;
        }
    }

    // --- ADAPTER ---
    class VictimAdapter extends RecyclerView.Adapter<VictimAdapter.Holder> {
        List<VictimModel> list;
        public VictimAdapter(List<VictimModel> list) { this.list = list; }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_victim, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            VictimModel item = list.get(position);
            holder.tvName.setText(item.name);
            holder.tvIC.setText("IC: " + item.ic);

            // ⭐ 1. ADMIN VIEW: READ-ONLY (No Buttons) ⭐
            if ("Admin".equals(userRole)) {
                // HIDE ALL BUTTONS
                holder.btnCheckOut.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.GONE);

                // Just show text status
                if ("Returned".equals(item.status)) {
                    holder.tvName.setTextColor(Color.GRAY);
                    holder.tvName.setText(item.name + " (Returned)");
                } else {
                    holder.tvName.setTextColor(Color.BLACK);
                }
            }

            // ⭐ 2. STAFF VIEW: MANAGE VICTIMS (Show Buttons) ⭐
            else {
                if ("Returned".equals(item.status)) {
                    // If Returned: Gray text, Hide "Check Out", Show "Delete" (Clean up)
                    holder.tvName.setTextColor(Color.GRAY);
                    holder.tvName.setText(item.name + " (Returned)");
                    holder.btnCheckOut.setVisibility(View.GONE);
                    holder.btnDelete.setVisibility(View.VISIBLE);
                } else {
                    // If Active: Black text, Show ALL Buttons
                    holder.tvName.setTextColor(Color.BLACK);
                    holder.btnCheckOut.setVisibility(View.VISIBLE);
                    holder.btnDelete.setVisibility(View.VISIBLE);
                }
            }

            // Set Listeners (Only works if buttons are visible)
            holder.btnDelete.setOnClickListener(v -> showDeleteConfirm(item));
            holder.btnCheckOut.setOnClickListener(v -> showCheckOutDialog(item));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvName, tvIC;
            ImageView btnDelete, btnCheckOut;
            public Holder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvVictimName);
                tvIC = itemView.findViewById(R.id.tvVictimIC);
                btnDelete = itemView.findViewById(R.id.btnDeleteVictim);
                btnCheckOut = itemView.findViewById(R.id.btnCheckOut);
            }
        }
    }
}