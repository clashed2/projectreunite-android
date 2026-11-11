package com.example.projectreunite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        SharedPreferences prefs = getSharedPreferences("reunite", MODE_PRIVATE);
        String name = prefs.getString("user_name", "Unknown User");
        String email = prefs.getString("user_email", "No email provided");

        TextView tvName = findViewById(R.id.tvProfileName);
        TextView tvEmail = findViewById(R.id.tvProfileEmail);
        tvName.setText(name);
        tvEmail.setText(email);

        findViewById(R.id.btnMyItems).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            intent.putExtra("FILTER_MY_ITEMS", true);
            startActivity(intent);
        });

        // Routes to search and triggers local bookmark filter
        findViewById(R.id.btnSavedItems).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            intent.putExtra("FILTER_SAVED_ITEMS", true);
            startActivity(intent);
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navAdd).setOnClickListener(v ->
                startActivity(new Intent(this, ReportLostActivity.class)));
        findViewById(R.id.navHome).setOnClickListener(v ->
                startActivity(new Intent(this, HomeActivity.class)));
        findViewById(R.id.navSearch).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        findViewById(R.id.navMessages).setOnClickListener(v ->
                startActivity(new Intent(this, MessagesActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v -> {});
    }
}