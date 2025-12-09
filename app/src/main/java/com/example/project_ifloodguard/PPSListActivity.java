package com.example.project_ifloodguard;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

        // 5. Initialize Lists
        fullPPSList = new ArrayList<>();
        displayedPPSList = new ArrayList<>();
        adapter = new PPSAdapter(displayedPPSList);
        recyclerView.setAdapter(adapter);

        // 6. Setup "Add New" Button
        // RULE: Only ADMIN can CREATE a new center
        FloatingActionButton fab = findViewById(R.id.fabAddPPS);
        if ("Staff".equals(userRole)) {
            fab.setVisibility(View.GONE); // Staff cannot build new centers
        } else {
            fab.setVisibility(View.VISIBLE); // Admin can build
            fab.setOnClickListener(v -> showAdminDialog(null));
        }

        // 7. Setup Search Listener
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
    // ⭐ 1. STAFF DIALOG (Advanced Logic) ⭐
    // ==========================================
    private void showStaffUpdateDialog(PPSModel model) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Victim Count");
        builder.setMessage("Center: " + model.name);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 10);

        TextView lblInfo = new TextView(this);
        lblInfo.setText("Current Victims in Center:");
        layout.addView(lblInfo);

        // Input Field
        final EditText inputCurrent = new EditText(this);
        inputCurrent.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputCurrent.setText(String.valueOf(model.currentOccupancy)); // Original Value
        inputCurrent.setTextSize(24);
        inputCurrent.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        layout.addView(inputCurrent);

        builder.setView(layout);

        // Set Buttons (We define logic LATER to control colors)
        builder.setPositiveButton("Update Count", null); // Logic set later
        builder.setNegativeButton("Cancel", null);       // Logic set later

        // Create the Dialog but don't show yet
        AlertDialog dialog = builder.create();
        dialog.show();

        // ⭐ GET BUTTONS TO CUSTOMIZE THEM ⭐
        android.widget.Button btnUpdate = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        android.widget.Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        // 1. INITIAL STATE: "Fade" the Update button (Disable it)
        btnUpdate.setEnabled(false);
        btnUpdate.setTextColor(Color.GRAY);
        // Note: We keep Cancel enabled so they can close, but you can change color if you want.
        btnCancel.setTextColor(Color.parseColor("#D32F2F")); // Red for Cancel

        // 2. WATCH FOR TEXT CHANGES
        inputCurrent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newVal = s.toString().trim();

                // Check: Is it empty? OR Is it the same as the original number?
                boolean isSame = newVal.isEmpty() || Integer.parseInt(newVal) == model.currentOccupancy;

                if (isSame) {
                    // No change? FADE OUT
                    btnUpdate.setEnabled(false);
                    btnUpdate.setTextColor(Color.GRAY);
                } else {
                    // Changed? COLORFUL!
                    btnUpdate.setEnabled(true);
                    btnUpdate.setTextColor(Color.parseColor("#1565C0")); // Blue
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 3. OVERRIDE "UPDATE" CLICK (Validation Logic)
        btnUpdate.setOnClickListener(v -> {
            String curStr = inputCurrent.getText().toString().trim();
            int newCount = Integer.parseInt(curStr);

            // Max Capacity Check
            int maxCap = 0;
            try { maxCap = Integer.parseInt(model.capacity); } catch (Exception e) { maxCap = 9999; }

            if (newCount > maxCap) {
                Toast.makeText(this, "Error: Cannot exceed Max Capacity (" + maxCap + ")!", Toast.LENGTH_LONG).show();
                return;
            }

            dialog.dismiss(); // Close input dialog
            showConfirmUpdateDialog(model, newCount); // Show "Confirm Update" popup
        });

        // 4. OVERRIDE "CANCEL" CLICK (Safety Logic)
        btnCancel.setOnClickListener(v -> {
            String curStr = inputCurrent.getText().toString().trim();

            // Check if user typed something different
            boolean hasChanged = !curStr.isEmpty() && Integer.parseInt(curStr) != model.currentOccupancy;

            if (hasChanged) {
                // If changed, ask "Are you sure to cancel?"
                showConfirmCancelDialog(dialog);
            } else {
                // If no change, just close immediately
                dialog.dismiss();
            }
        });
    }

    // ⭐ NEW: CANCEL CONFIRMATION POPUP ⭐
    private void showConfirmCancelDialog(AlertDialog parentDialog) {
        new AlertDialog.Builder(this)
                .setTitle("Discard Changes?")
                .setMessage("Are you sure to cancel update? Unsaved changes will be lost.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // User said Yes -> Close the parent input dialog
                    parentDialog.dismiss();
                })
                .setNegativeButton("No", null) // User said No -> Go back to editing
                .show();
    }

    // ⭐ STAFF CONFIRMATION POPUP
    private void showConfirmUpdateDialog(PPSModel model, int newCount) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Update")
                .setMessage("Are you sure you want to update the count to " + newCount + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Save to Firebase
                    dbRef.child(model.id).child("current_occupancy").setValue(newCount)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Count Updated Successfully!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }

    // ==========================================
    // ⭐ 2. ADMIN DIALOG (Advanced Logic) ⭐
    // ==========================================
    private void showAdminDialog(PPSModel model) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(model == null ? "Create New Center" : "Edit Facility Details");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Name Input
        final EditText inputName = new EditText(this);
        inputName.setHint("Center Name");
        if(model != null) inputName.setText(model.name);
        layout.addView(inputName);

        // Capacity Input
        final EditText inputCapacity = new EditText(this);
        inputCapacity.setHint("Max Capacity");
        inputCapacity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if(model != null) inputCapacity.setText(model.capacity);
        layout.addView(inputCapacity);

        builder.setView(layout);

        // Define Buttons (Logic added later)
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", null);

        // Add Delete Button (Only if editing existing center)
        if (model != null) {
            builder.setNeutralButton("Delete", (d, w) -> {
                showAdminDeleteConfirmation(model);
            });
        }

        // Create & Show
        AlertDialog dialog = builder.create();
        dialog.show();

        // ⭐ GET BUTTONS TO CUSTOMIZE ⭐
        android.widget.Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        android.widget.Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        // 1. INITIAL STATE: Disable Save Button
        // (Only disable if editing. If creating new, we might want to keep it disabled until filled)
        if (model != null) {
            btnSave.setEnabled(false);
            btnSave.setTextColor(Color.GRAY);
        }
        btnCancel.setTextColor(Color.parseColor("#D32F2F")); // Red

        // 2. WATCH FOR CHANGES (Common Watcher for both fields)
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newName = inputName.getText().toString().trim();
                String newCap = inputCapacity.getText().toString().trim();

                boolean hasChanged = false;

                if (model == null) {
                    // Creating New: Enable only if both fields have text
                    hasChanged = !newName.isEmpty() && !newCap.isEmpty();
                } else {
                    // Editing: Enable only if text is DIFFERENT from original
                    boolean nameChanged = !newName.equals(model.name);
                    boolean capChanged = !newCap.equals(model.capacity);
                    hasChanged = nameChanged || capChanged;
                }

                if (hasChanged) {
                    btnSave.setEnabled(true);
                    btnSave.setTextColor(Color.parseColor("#1565C0")); // Blue
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

        // 3. OVERRIDE "SAVE" CLICK
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

        // 4. OVERRIDE "CANCEL" CLICK
        btnCancel.setOnClickListener(v -> {
            String newName = inputName.getText().toString().trim();
            String newCap = inputCapacity.getText().toString().trim();

            boolean hasChanged = false;
            if (model != null) {
                // Check if anything changed
                hasChanged = !newName.equals(model.name) || !newCap.equals(model.capacity);
            } else {
                // Check if user typed anything at all
                hasChanged = !newName.isEmpty() || !newCap.isEmpty();
            }

            if (hasChanged) {
                // Reuse the same helper method we made for Staff!
                showConfirmCancelDialog(dialog);
            } else {
                dialog.dismiss();
            }
        });
    }

    // ⭐ ADMIN SAVE CONFIRMATION POPUP
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

    // ⭐ NEW: ADMIN DELETE CONFIRMATION POPUP ⭐
    private void showAdminDeleteConfirmation(PPSModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete '" + model.name + "'?\n\nThis cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Actual delete happens here
                    dbRef.child(model.id).removeValue()
                            .addOnSuccessListener(a -> Toast.makeText(this, "Center Deleted", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
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

            // Calculations
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
            holder.tvAvailable.setTextColor(Color.parseColor("#388E3C")); // Green color

            if (percent >= 90) holder.tvPercent.setTextColor(Color.RED);
            else holder.tvPercent.setTextColor(Color.parseColor("#F57C00"));

            // Click Logic
            holder.itemView.setOnClickListener(v -> {
                if ("Staff".equals(userRole)) {
                    showStaffUpdateDialog(item);
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