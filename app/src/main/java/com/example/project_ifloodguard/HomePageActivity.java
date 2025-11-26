package com.example.project_ifloodguard;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class HomePageActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navView;
    ActionBarDrawerToggle toggle;
    FrameLayout contentFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        contentFrame = findViewById(R.id.content_frame);

        // Setup toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Home");

        // Drawer toggle (hamburger icon)
        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Load default content
        loadFragment(R.layout.content_home_page);

        // Handle menu item clicks
        navView.setNavigationItemSelectedListener(item -> {
            handleNavigation(item);
            return true;
        });
    }

    private void handleNavigation(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            getSupportActionBar().setTitle("Home");
            loadFragment(R.layout.content_home_page);

        } else if (id == R.id.nav_sensor) {
            startActivity(new Intent(this, WaterLevelStatusActivity.class));


    } else if (id == R.id.nav_alerts) {
            getSupportActionBar().setTitle("Alert History");
            loadFragment(R.layout.content_alert_history);

        } else if (id == R.id.nav_pps) {
            getSupportActionBar().setTitle("PPS List");
            loadFragment(R.layout.content_pps_list);

        } else if (id == R.id.nav_contacts) {
            getSupportActionBar().setTitle("Emergency Responder Contact");
            loadFragment(R.layout.content_emergency_contact);

        } else if (id == R.id.nav_about) {
            getSupportActionBar().setTitle("Profile Update");
            loadFragment(R.layout.content_profile_update);

        } else if (id == R.id.nav_logout) {
            // Firebase logout
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            // Redirect to LoginActivity
            Intent intent = new Intent(this, LoginActivity.class); // Replace with your login activity class
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawers();
    }

    // Load the layout into content_frame dynamically
    private void loadFragment(int layoutResId) {
        contentFrame.removeAllViews();
        getLayoutInflater().inflate(layoutResId, contentFrame, true);
    }

    // Close drawer on back press
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
