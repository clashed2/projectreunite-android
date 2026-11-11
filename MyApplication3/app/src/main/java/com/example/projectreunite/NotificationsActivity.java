package com.example.projectreunite;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class NotificationsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Fully synced bottom navigation
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        findViewById(R.id.navSearch).setOnClickListener(v -> {
            startActivity(new Intent(this, SearchActivity.class));
            finish();
        });
        findViewById(R.id.navAdd).setOnClickListener(v ->
                startActivity(new Intent(this, ReportLostActivity.class)));
        findViewById(R.id.navMessages).setOnClickListener(v -> {
            startActivity(new Intent(this, MessagesActivity.class));
            finish();
        });
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }
}