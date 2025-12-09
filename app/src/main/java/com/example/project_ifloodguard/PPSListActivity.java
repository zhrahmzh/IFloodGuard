package com.example.project_ifloodguard;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent; // Don't forget this import!
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

    // Lists
    List<PPSModel> fullPPSList;
    List<PPSModel> displayedPPSList;

    DatabaseReference dbRef;
    TextView tvEmpty;

    // Search Element
    EditText searchInput;
    String userRole = "Staff"; // Default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_pps_list);

        // 1. Get Role
        if (getIntent().hasExtra("USER_ROLE")) {
            userRole = getIntent().getStringExtra("USER_ROLE");
        }

        // 2. Setup Back Button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // 3. Setup Firebase
        dbRef = FirebaseDatabase
                .getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("pps_list");

        // 4. Setup UI
        recyclerView = findViewById(R.id.recyclerViewPPS);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvEmpty = findViewById(R.id.tvEmpty);

        searchInput = findViewById(R.id.etSearchPPS);

        // ⭐ 1. KEYBOARD SEARCH BUTTON ⭐
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        // ⭐ 2. ICON CLICK LISTENER ⭐
        searchInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // Check if touch is on the Right Side
                if (event.getRawX() >= (searchInput.getRight() - searchInput.getCompoundDrawables()[2].getBounds().width())) {
                    hideKeyboard();
                    return true;
                }
            }
            return false;
        });

        // 5. Initialize Lists
        fullPPSList = new ArrayList<>();
        displayedPPSList = new ArrayList<>();
        adapter = new PPSAdapter(displayedPPSList);
        recyclerView.setAdapter(adapter);

        // 6. Setup "Add New" Button
        FloatingActionButton fab = findViewById(R.id.fabAddPPS);
        if ("Staff".equals(userRole)) {
            fab.setVisibility(View.GONE);
        } else {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> showAdminDialog(null));
        }

        // 7. Setup Search Listener (Filtering)
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadPPSData();
    }

    // ⭐ HELPER TO HIDE KEYBOARD ⭐
    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }

    private void loadPPSData() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullPPSList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String id = data.getKey();
                    String name = data.child("name").getValue(String.class);
                    String capacity = data.child("capacity").getValue(String.class);

                    Integer current = data.child("current_occupancy").getValue(Integer.class);
                    if (current == null) current = 0;

                    if (name != null) {
                        fullPPSList.add(new PPSModel(id, name, capacity, current));
                    }
                }
                filterData(searchInput.getText().toString());
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PPSListActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterData(String keyword) {
        displayedPPSList.clear();
        String search = keyword.toLowerCase().trim();

        if (search.isEmpty()) {
            displayedPPSList.addAll(fullPPSList);
        } else {
            for (PPSModel item : fullPPSList) {
                if (item.name.toLowerCase().contains(search)) {
                    displayedPPSList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(displayedPPSList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ==========================================
    // ADMIN DIALOG
    // ==========================================
    private void showAdminDialog(PPSModel model) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(model == null ? "Create New Center" : "Edit Facility Details");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("Center Name");
        if(model != null) inputName.setText(model.name);
        layout.addView(inputName);

        final EditText inputCapacity = new EditText(this);
        inputCapacity.setHint("Max Capacity");
        inputCapacity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if(model != null) inputCapacity.setText(model.capacity);
        layout.addView(inputCapacity);

        builder.setView(layout);
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", null);

        if (model != null) {
            builder.setNeutralButton("Delete", (d, w) -> {
                showAdminDeleteConfirmation(model);
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        android.widget.Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        android.widget.Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (model != null) {
            btnSave.setEnabled(false);
            btnSave.setTextColor(Color.GRAY);
        }
        btnCancel.setTextColor(Color.parseColor("#D32F2F"));

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newName = inputName.getText().toString().trim();
                String newCap = inputCapacity.getText().toString().trim();
                boolean hasChanged = false;
                if (model == null) {
                    hasChanged = !newName.isEmpty() && !newCap.isEmpty();
                } else {
                    boolean nameChanged = !newName.equals(model.name);
                    boolean capChanged = !newCap.equals(model.capacity);
                    hasChanged = nameChanged || capChanged;
                }
                if (hasChanged) {
                    btnSave.setEnabled(true);
                    btnSave.setTextColor(Color.parseColor("#1565C0"));
                } else {
                    btnSave.setEnabled(false);
                    btnSave.setTextColor(Color.GRAY);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        inputName.addTextChangedListener(watcher);
        inputCapacity.addTextChangedListener(watcher);

        btnSave.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String cap = inputCapacity.getText().toString().trim();
            if (name.isEmpty() || cap.isEmpty()) {
                Toast.makeText(this, "Please fill in all details", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            showAdminSaveConfirmation(model, name, cap);
        });

        btnCancel.setOnClickListener(v -> {
            String newName = inputName.getText().toString().trim();
            String newCap = inputCapacity.getText().toString().trim();
            boolean hasChanged = false;
            if (model != null) {
                hasChanged = !newName.equals(model.name) || !newCap.equals(model.capacity);
            } else {
                hasChanged = !newName.isEmpty() || !newCap.isEmpty();
            }
            if (hasChanged) {
                showConfirmCancelDialog(dialog);
            } else {
                dialog.dismiss();
            }
        });
    }

    private void showAdminSaveConfirmation(PPSModel model, String name, String capacity) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Save")
                .setMessage(model == null ? "Create this new center?" : "Save changes to this center?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", name);
                    map.put("capacity", capacity);

                    if (model == null) {
                        map.put("current_occupancy", 0);
                        dbRef.push().setValue(map)
                                .addOnSuccessListener(a -> Toast.makeText(this, "Center Created Successfully", Toast.LENGTH_SHORT).show());
                    } else {
                        dbRef.child(model.id).updateChildren(map)
                                .addOnSuccessListener(a -> Toast.makeText(this, "Details Updated Successfully", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showAdminDeleteConfirmation(PPSModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete '" + model.name + "'?\n\nThis cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbRef.child(model.id).removeValue()
                            .addOnSuccessListener(a -> Toast.makeText(this, "Center Deleted", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showConfirmCancelDialog(AlertDialog parentDialog) {
        new AlertDialog.Builder(this)
                .setTitle("Discard Changes?")
                .setMessage("Are you sure you want to discard this info?")
                .setPositiveButton("Yes", (dialog, which) -> parentDialog.dismiss())
                .setNegativeButton("No", null)
                .show();
    }

    // --- MODEL ---
    public static class PPSModel {
        String id, name, capacity;
        int currentOccupancy;
        public PPSModel(String id, String name, String capacity, int currentOccupancy) {
            this.id = id; this.name = name; this.capacity = capacity; this.currentOccupancy = currentOccupancy;
        }
    }

    // --- ADAPTER ---
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

            int current = item.currentOccupancy;
            int max = 1;
            try { max = Integer.parseInt(item.capacity); } catch (Exception e) {}

            int available = max - current;
            if (available < 0) available = 0;

            int percent = (int)((current / (float)max) * 100);
            if(percent > 100) percent = 100;

            holder.tvCapacity.setText("Max: " + max + " | Current: " + current);
            holder.tvPercent.setText(percent + "% Full");
            holder.progressBar.setProgress(percent);
            holder.tvAvailable.setText("Available Slots: " + available);
            holder.tvAvailable.setTextColor(Color.parseColor("#388E3C"));

            if (percent >= 90) holder.tvPercent.setTextColor(Color.RED);
            else holder.tvPercent.setTextColor(Color.parseColor("#F57C00"));

            holder.itemView.setOnClickListener(v -> {
                if ("Staff".equals(userRole)) {
                    android.content.Intent intent = new android.content.Intent(PPSListActivity.this, VictimListActivity.class);
                    intent.putExtra("CENTER_ID", item.id);
                    intent.putExtra("CENTER_NAME", item.name);
                    startActivity(intent);
                } else {
                    showAdminDialog(item);
                }
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class PPSViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvCapacity, tvPercent, tvAvailable;
            ProgressBar progressBar;
            public PPSViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvPPSName);
                tvCapacity = itemView.findViewById(R.id.tvCapacity);
                tvPercent = itemView.findViewById(R.id.tvPercent);
                tvAvailable = itemView.findViewById(R.id.tvAvailable);
                progressBar = itemView.findViewById(R.id.progressCapacity);
            }
        }
    }
}