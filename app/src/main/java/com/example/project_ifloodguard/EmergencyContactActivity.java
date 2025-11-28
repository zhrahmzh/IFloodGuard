package com.example.project_ifloodguard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
    List<ResponderModel> list;
    DatabaseReference dbRef;
    TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_emergency_contact);

        // 1. Firebase Node: "responders_list"
        dbRef = FirebaseDatabase
                .getInstance("https://ifloodguard-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("responders_list");

        // 2. Setup UI
        recyclerView = findViewById(R.id.recyclerResponders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvEmpty = findViewById(R.id.tvEmptyResponders);

        list = new ArrayList<>();
        adapter = new ResponderAdapter(list);
        recyclerView.setAdapter(adapter);

        // 3. Add Button Logic
        FloatingActionButton fab = findViewById(R.id.fabAddResponder);
        fab.setOnClickListener(v -> showUpdateDialog(null));

        // 4. Load Data
        loadData();
    }

    private void loadData() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String id = data.getKey();
                    String name = data.child("name").getValue(String.class);
                    String type = data.child("type").getValue(String.class);
                    String phone = data.child("phone").getValue(String.class);

                    if (name != null) {
                        list.add(new ResponderModel(id, name, type, phone));
                    }
                }
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- DIALOG FOR ADD / EDIT ---
    private void showUpdateDialog(ResponderModel model) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(model == null ? "Tambah Agensi" : "Kemaskini Info");

        // Create Layout Programmatically
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("Nama (Contoh: Balai Bomba Skudai)");
        if(model != null) inputName.setText(model.name);
        layout.addView(inputName);

        final EditText inputType = new EditText(this);
        inputType.setHint("Jenis (Contoh: BOMBA / POLIS / JPAM)");
        if(model != null) inputType.setText(model.type);
        layout.addView(inputType);

        final EditText inputPhone = new EditText(this);
        inputPhone.setHint("No Telefon (Contoh: 075551234)");
        inputPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        if(model != null) inputPhone.setText(model.phone);
        layout.addView(inputPhone);

        builder.setView(layout);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            String type = inputType.getText().toString().trim();
            String phone = inputPhone.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) return;

            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("type", type);
            map.put("phone", phone);

            if (model == null) {
                dbRef.push().setValue(map); // Add New
            } else {
                dbRef.child(model.id).updateChildren(map); // Update
            }
        });

        if (model != null) {
            builder.setNeutralButton("Padam", (d, w) -> {
                dbRef.child(model.id).removeValue();
            });
        }
        builder.setNegativeButton("Batal", null);
        builder.show();
    }

    // --- MODEL ---
    public static class ResponderModel {
        String id, name, type, phone;
        public ResponderModel(String id, String name, String type, String phone) {
            this.id = id; this.name = name; this.type = type; this.phone = phone;
        }
    }

    // --- ADAPTER ---
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

            // 1. CLICK TO EDIT
            holder.itemView.setOnClickListener(v -> showUpdateDialog(item));

            // 2. CLICK TO CALL
            holder.btnCall.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + item.phone));
                startActivity(intent);
            });
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