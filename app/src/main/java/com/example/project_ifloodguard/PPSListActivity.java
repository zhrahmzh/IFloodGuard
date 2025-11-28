package com.example.project_ifloodguard;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
        setContentView(R.layout.content_pps_list); // Using your layout file

        // 1. Setup Firebase
        // NOTE: We store PPS lists under a new node called "pps_list"
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
        fab.setOnClickListener(v -> showUpdateDialog(null)); // Null means ADD new

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

                    if (name != null) {
                        ppsList.add(new PPSModel(id, name, capacity));
                    }
                }
                adapter.notifyDataSetChanged();

                // Show "Empty" text if list is empty
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
        builder.setTitle(model == null ? "Tambah PPS Baru" : "Kemaskini PPS");

        // Create the input fields
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.content_profile_update, null, false);
        // NOTE: We are dynamically creating a simple layout here instead of XML to save time
        // or we can use a standard input layout. Let's use standard Java views for simplicity.

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("Nama PPS (Contoh: Dewan A)");
        if(model != null) inputName.setText(model.name);
        layout.addView(inputName);

        final EditText inputCapacity = new EditText(this);
        inputCapacity.setHint("Kapasiti (Contoh: 500)");
        inputCapacity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if(model != null) inputCapacity.setText(model.capacity);
        layout.addView(inputCapacity);

        builder.setView(layout);

        // Save Button
        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            String cap = inputCapacity.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(PPSListActivity.this, "Nama diperlukan!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (model == null) {
                // ADD NEW
                String id = dbRef.push().getKey();
                Map<String, Object> map = new HashMap<>();
                map.put("name", name);
                map.put("capacity", cap);
                if(id != null) dbRef.child(id).setValue(map);
            } else {
                // UPDATE EXISTING
                Map<String, Object> map = new HashMap<>();
                map.put("name", name);
                map.put("capacity", cap);
                dbRef.child(model.id).updateChildren(map);
            }
        });

        // Delete Button (Only if editing)
        if (model != null) {
            builder.setNeutralButton("Padam", (dialog, which) -> {
                new AlertDialog.Builder(this)
                        .setTitle("Padam PPS?")
                        .setMessage("Adakah anda pasti?")
                        .setPositiveButton("Ya", (d, w) -> dbRef.child(model.id).removeValue())
                        .setNegativeButton("Tidak", null)
                        .show();
            });
        }

        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // --- INTERNAL MODEL CLASS ---
    public static class PPSModel {
        String id, name, capacity;
        public PPSModel(String id, String name, String capacity) {
            this.id = id; this.name = name; this.capacity = capacity;
        }
    }

    // --- INTERNAL ADAPTER CLASS ---
    class PPSAdapter extends RecyclerView.Adapter<PPSAdapter.PPSViewHolder> {
        List<PPSModel> list;
        public PPSAdapter(List<PPSModel> list) { this.list = list; }

        @NonNull
        @Override
        public PPSViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pps, parent, false);
            return new PPSViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PPSViewHolder holder, int position) {
            PPSModel item = list.get(position);
            holder.tvName.setText(item.name);
            holder.tvCap.setText("Kapasiti: " + item.capacity);

            // Click to Edit
            holder.itemView.setOnClickListener(v -> showUpdateDialog(item));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class PPSViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvCap;
            public PPSViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvPPSName);
                tvCap = itemView.findViewById(R.id.tvCapacity);
            }
        }
    }
}