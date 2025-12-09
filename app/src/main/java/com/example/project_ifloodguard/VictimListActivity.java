package com.example.project_ifloodguard;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent; // Import MotionEvent
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VictimListActivity extends AppCompatActivity {

    String centerId, centerName;
    DatabaseReference dbVictims;
    DatabaseReference dbCenterCount;

    RecyclerView recyclerView;
    VictimAdapter adapter;
    List<VictimModel> fullList, displayedList;
    EditText searchInput;
    TextView tvEmpty, tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_victim_list);

        // 1. Get Data
        if (getIntent().hasExtra("CENTER_ID")) {
            centerId = getIntent().getStringExtra("CENTER_ID");
            centerName = getIntent().getStringExtra("CENTER_NAME");
        } else {
            finish();
            return;
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

        // Init Search Input
        searchInput = findViewById(R.id.etSearchVictim);

        // --- KEYBOARD BUTTON HANDLER (Enter Key) ---
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        // ⭐ NEW: SEARCH ICON CLICK HANDLER (Touch Listener) ⭐
        searchInput.setOnTouchListener((v, event) -> {
            // Only detect when user LIFTS finger (ACTION_UP)
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // Check if touch is on the Right Side (where the icon is)
                if (event.getRawX() >= (searchInput.getRight() - searchInput.getCompoundDrawables()[2].getBounds().width())) {
                    // User clicked the Icon! Hide Keyboard.
                    hideKeyboard();
                    return true; // Stop click from doing anything else (like opening keyboard again)
                }
            }
            return false; // Normal typing behavior
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        FloatingActionButton fab = findViewById(R.id.fabAddVictim);
        fab.setOnClickListener(v -> showAddVictimDialog());

        // 4. Setup List
        recyclerView = findViewById(R.id.recyclerVictims);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fullList = new ArrayList<>();
        displayedList = new ArrayList<>();
        adapter = new VictimAdapter(displayedList);
        recyclerView.setAdapter(adapter);

        // 5. Search Logic
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

    // ⭐ HELPER METHOD TO HIDE KEYBOARD ⭐
    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }

    private void loadVictims() {
        dbVictims.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String name = data.child("name").getValue(String.class);
                    String ic = data.child("ic").getValue(String.class);
                    if (name != null) {
                        fullList.add(new VictimModel(data.getKey(), name, ic));
                    }
                }
                dbCenterCount.setValue(fullList.size());
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

    // ==========================================
    // REGISTER DIALOG
    // ==========================================
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
        android.widget.Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        btnRegister.setEnabled(false);
        btnRegister.setTextColor(Color.GRAY);
        btnCancel.setTextColor(Color.parseColor("#D32F2F"));

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
            showConfirmRegisterDialog(name, ic);
        });

        btnCancel.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String ic = inputIC.getText().toString().trim();
            if (!name.isEmpty() || !ic.isEmpty()) {
                showConfirmCancelDialog(dialog);
            } else {
                dialog.dismiss();
            }
        });
    }

    private void showConfirmRegisterDialog(String name, String ic) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Registration")
                .setMessage("Register this victim?\n\nName: " + name + "\nIC: " + ic)
                .setPositiveButton("Yes, Register", (dialog, which) -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", name);
                    map.put("ic", ic);
                    dbVictims.push().setValue(map)
                            .addOnSuccessListener(a -> Toast.makeText(this, "Victim Registered Successfully", Toast.LENGTH_SHORT).show());
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

    private void showDeleteConfirm(VictimModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to remove " + model.name + "?")
                .setPositiveButton("Yes, Remove", (dialog, which) -> {
                    dbVictims.child(model.id).removeValue()
                            .addOnSuccessListener(a -> Toast.makeText(this, "Victim Removed", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- MODEL ---
    class VictimModel {
        String id, name, ic;
        public VictimModel(String id, String name, String ic) {
            this.id = id; this.name = name; this.ic = ic;
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
            holder.btnDelete.setOnClickListener(v -> showDeleteConfirm(item));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvName, tvIC;
            ImageView btnDelete;
            public Holder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvVictimName);
                tvIC = itemView.findViewById(R.id.tvVictimIC);
                btnDelete = itemView.findViewById(R.id.btnDeleteVictim);
            }
        }
    }
}