package com.example.project_ifloodguard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
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

    // Lists
    List<ResponderModel> fullList;
    List<ResponderModel> displayedList;

    DatabaseReference dbRef;
    TextView tvEmpty;

    // Filter Variables
    EditText searchInput;
    ChipGroup chipGroup;
    String currentCategory = "All";
    String userRole = "Staff"; // Default safe role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_emergency_contact);

        // 1. Get Role Logic
        if (getIntent().hasExtra("USER_ROLE")) {
            userRole = getIntent().getStringExtra("USER_ROLE");
        }

        dbRef = FirebaseDatabase
                .getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("responders_list");

        // 2. Initialize UI
        recyclerView = findViewById(R.id.recyclerResponders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvEmpty = findViewById(R.id.tvEmptyResponders);
        searchInput = findViewById(R.id.etSearchResponder);
        chipGroup = findViewById(R.id.chipGroupResponders);

        // 3. Initialize Lists
        fullList = new ArrayList<>();
        displayedList = new ArrayList<>();
        adapter = new ResponderAdapter(displayedList);
        recyclerView.setAdapter(adapter);

        // 4. Role Logic for Add Button
        FloatingActionButton fab = findViewById(R.id.fabAddResponder);

        if ("Staff".equals(userRole)) {
            fab.setVisibility(View.GONE); // Hide for Staff
        } else {
            fab.setVisibility(View.VISIBLE); // Show for Admin
            fab.setOnClickListener(v -> showUpdateDialog(null));
        }

        // 5. Search Logic
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

        // 6. Chip Filter Logic
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Chip chip = findViewById(checkedId);
            if (chip != null) {
                currentCategory = chip.getText().toString();
                filterData(searchInput.getText().toString());
            }
        });

        // REMOVED DUPLICATE FAB CODE HERE

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadData();
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

    // --- MAIN INPUT DIALOG ---
    private void showUpdateDialog(ResponderModel model) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(model == null ? "Add Agency" : "Update Info");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("Name (e.g. Skudai Fire Station)");
        if(model != null) inputName.setText(model.name);
        layout.addView(inputName);

        final EditText inputType = new EditText(this);
        inputType.setHint("Type (Police / Fire / Hospital / Center)");
        if(model != null) inputType.setText(model.type);
        layout.addView(inputType);

        final EditText inputPhone = new EditText(this);
        inputPhone.setHint("Phone Number");
        inputPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        if(model != null) inputPhone.setText(model.phone);
        layout.addView(inputPhone);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            String type = inputType.getText().toString().trim();
            String phone = inputPhone.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Name and Phone are required!", Toast.LENGTH_SHORT).show();
                return;
            }
            showSaveConfirmation(model, name, type, phone);
        });

        if (model != null) {
            builder.setNeutralButton("Delete", (d, w) -> {
                showDeleteConfirmation(model);
            });
        }

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSaveConfirmation(ResponderModel model, String name, String type, String phone) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Save")
                .setMessage("Are you sure to save this update?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    saveToFirebase(model, name, type, phone);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showDeleteConfirmation(ResponderModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure want to delete this contact?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dbRef.child(model.id).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Contact Deleted", Toast.LENGTH_SHORT).show());
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
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Added Successfully", Toast.LENGTH_SHORT).show());
        } else {
            dbRef.child(model.id).updateChildren(map)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Updated Successfully", Toast.LENGTH_SHORT).show());
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

            // DEBUG: Print the exact role the Adapter sees
            System.out.println("ADAPTER ROLE CHECK: " + userRole);

            // --- STRICT ROLE LOGIC ---
            // 1. Check if userRole is not null
            // 2. Trim spaces (e.g. "Admin " becomes "Admin")
            // 3. Ignore case (e.g. "admin" works same as "Admin")
            if (userRole != null && userRole.trim().equalsIgnoreCase("Admin")) {

                // --- ADMIN SETTINGS ---
                holder.itemView.setOnClickListener(v -> showUpdateDialog(item));

                holder.btnCall.setVisibility(View.VISIBLE); // SHOW Button
                holder.btnCall.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + item.phone));
                    startActivity(intent);
                });

            } else {

                // --- STAFF SETTINGS ---
                holder.itemView.setOnClickListener(null);
                holder.itemView.setClickable(false);

                holder.btnCall.setVisibility(View.GONE); // HIDE Button
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