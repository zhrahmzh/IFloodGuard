package com.example.project_ifloodguard;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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

public class EmergencyContactActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ResponderAdapter adapter;
    List<ResponderModel> fullList;
    List<ResponderModel> displayedList;
    DatabaseReference dbRef;
    TextView tvEmpty;
    EditText searchInput;
    ChipGroup chipGroup;
    String currentCategory = "All";
    String userRole = "Staff";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_emergency_contact);

        if (getIntent().hasExtra("USER_ROLE")) {
            userRole = getIntent().getStringExtra("USER_ROLE");
        }

        dbRef = FirebaseDatabase
                .getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("responders_list");

        recyclerView = findViewById(R.id.recyclerResponders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvEmpty = findViewById(R.id.tvEmptyResponders);
        searchInput = findViewById(R.id.etSearchResponder);
        chipGroup = findViewById(R.id.chipGroupResponders);

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
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

        fullList = new ArrayList<>();
        displayedList = new ArrayList<>();
        adapter = new ResponderAdapter(displayedList);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddResponder);
        if ("Staff".equals(userRole)) {
            fab.setVisibility(View.GONE);
        } else {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> showUpdateDialog(null)); // Pass null for Add New
        }

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

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Chip chip = findViewById(checkedId);
            if (chip != null) {
                currentCategory = chip.getText().toString();
                filterData(searchInput.getText().toString());
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadData();
    }

    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }

    private void loadData() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String id = data.getKey();
                    String name = data.child("name").getValue(String.class);
                    String type = data.child("type").getValue(String.class);
                    String phone = data.child("phone").getValue(String.class);

                    if (name != null) {
                        fullList.add(new ResponderModel(id, name, type, phone));
                    }
                }
                java.util.Collections.sort(fullList, (a, b) -> a.name.compareToIgnoreCase(b.name));
                filterData(searchInput.getText().toString());
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterData(String keyword) {
        displayedList.clear();
        String search = keyword.toLowerCase().trim();

        for (ResponderModel item : fullList) {
            boolean matchesSearch = item.name.toLowerCase().contains(search);
            boolean matchesCategory = currentCategory.equals("All") ||
                    item.type.equalsIgnoreCase(currentCategory) ||
                    (currentCategory.equals("Fire") && item.type.equalsIgnoreCase("Fire Dept"));

            if (matchesSearch && matchesCategory) {
                displayedList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ⭐ COMBINED SMART DIALOG (HANDLES ADD & EDIT) ⭐
    private void showUpdateDialog(ResponderModel model) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(model == null ? "Add Agency" : "Update Info");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("Name (e.g. Skudai Fire Station)");
        if (model != null) inputName.setText(model.name);
        layout.addView(inputName);

        final EditText inputType = new EditText(this);
        inputType.setHint("Type (Police / Fire / Hospital)");
        if (model != null) inputType.setText(model.type);
        layout.addView(inputType);

        final EditText inputPhone = new EditText(this);
        inputPhone.setHint("Phone Number");
        inputPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        if (model != null) inputPhone.setText(model.phone);
        layout.addView(inputPhone);

        builder.setView(layout);

        // Setup Buttons (Negative & Neutral first)
        builder.setNegativeButton("Cancel", null);
        if (model != null) {
            builder.setNeutralButton("Delete", (d, w) -> showDeleteConfirmation(model));
        }

        // We set Positive button listener later to control enable/disable
        builder.setPositiveButton("Save", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // ⭐ BUTTON LOGIC STARTS HERE ⭐
        android.widget.Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnSave.setEnabled(false); // Start Disabled
        btnSave.setTextColor(Color.GRAY);

        // Define Watcher
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String name = inputName.getText().toString().trim();
                String type = inputType.getText().toString().trim();
                String phone = inputPhone.getText().toString().trim();

                boolean isValid = !name.isEmpty() && !type.isEmpty() && !phone.isEmpty();
                boolean hasChanges = false;

                if (model == null) {
                    // ADD NEW MODE: Enabled if fields are not empty
                    hasChanges = true;
                } else {
                    // EDIT MODE: Enabled ONLY if something changed
                    boolean nameChanged = !name.equals(model.name);
                    boolean typeChanged = !type.equals(model.type);
                    boolean phoneChanged = !phone.equals(model.phone);
                    hasChanges = nameChanged || typeChanged || phoneChanged;
                }

                if (isValid && hasChanges) {
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
        inputType.addTextChangedListener(watcher);
        inputPhone.addTextChangedListener(watcher);

        // Handle Click Manually
        btnSave.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String type = inputType.getText().toString().trim();
            String phone = inputPhone.getText().toString().trim();

            showSaveConfirmation(model, name, type, phone);
            dialog.dismiss();
        });
    }

    private void showSaveConfirmation(ResponderModel model, String name, String type, String phone) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Save")
                .setMessage("Are you sure you want to save?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    saveToFirebase(model, name, type, phone);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showDeleteConfirmation(ResponderModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Delete " + model.name + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dbRef.child(model.id).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void saveToFirebase(ResponderModel model, String name, String type, String phone) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("type", type);
        map.put("phone", phone);

        if (model == null) {
            dbRef.push().setValue(map)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show());
        } else {
            dbRef.child(model.id).updateChildren(map)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show());
        }
    }

    public static class ResponderModel {
        String id, name, type, phone;
        public ResponderModel(String id, String name, String type, String phone) {
            this.id = id; this.name = name; this.type = type; this.phone = phone;
        }
    }

    class ResponderAdapter extends RecyclerView.Adapter<ResponderAdapter.ViewHolder> {
        List<ResponderModel> list;
        public ResponderAdapter(List<ResponderModel> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_responder, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ResponderModel item = list.get(position);
            holder.tvName.setText(item.name);
            holder.tvType.setText(item.type);
            holder.tvPhone.setText(item.phone);

            if ("Admin".equals(userRole)) {
                // ADMIN: Click to Edit, Show Call Button
                holder.itemView.setOnClickListener(v -> showUpdateDialog(item)); // Calls the smart dialog
                holder.btnCall.setVisibility(View.VISIBLE);
                holder.btnCall.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + item.phone));
                    startActivity(intent);
                });
            } else {
                // STAFF: View Only
                holder.itemView.setOnClickListener(null);
                holder.itemView.setClickable(false);
                holder.btnCall.setVisibility(View.GONE);
                holder.btnCall.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvType, tvPhone;
            ImageButton btnCall;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvRespName);
                tvType = itemView.findViewById(R.id.tvRespType);
                tvPhone = itemView.findViewById(R.id.tvRespPhone);
                btnCall = itemView.findViewById(R.id.btnCallNow);
            }
        }
    }
}