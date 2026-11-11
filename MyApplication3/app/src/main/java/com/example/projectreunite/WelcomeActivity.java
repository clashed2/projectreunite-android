package com.example.projectreunite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Auto-Login Check
        SharedPreferences prefs = getSharedPreferences("reunite", MODE_PRIVATE);
        if (prefs.getInt("user_id", 0) != 0) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_welcome);

        findViewById(R.id.btnGetStarted).setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, SignUpActivity.class));
        });

        findViewById(R.id.btnLogIn).setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
        });
    }
}