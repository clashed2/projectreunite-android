package com.example.projectreunite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvSignUp = findViewById(R.id.tvSignUp);

        // Fixed back button to return to Welcome Screen
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
            finish();
        });

        btnLogin.setOnClickListener(v -> {
            String username = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            btnLogin.setEnabled(false);
            executor.execute(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("username", username);
                    body.put("password", password);
                    JSONObject response = ApiClient.post("api_login.php", body);

                    if (response.optString("status").equals("success")) {
                        JSONObject user = response.getJSONObject("user");
                        getSharedPreferences("reunite", MODE_PRIVATE).edit()
                                .putInt("user_id", user.optInt("id", 0))
                                .putString("user_name", user.optString("name", ""))
                                .putString("user_email", user.optString("email", ""))
                                .apply();

                        runOnUiThread(() -> {
                            startActivity(new Intent(MainActivity.this, HomeActivity.class));
                            finish();
                        });
                    } else {
                        String message = response.optString("message", "Login failed");
                        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "Error connecting to server", Toast.LENGTH_LONG).show());
                } finally {
                    runOnUiThread(() -> btnLogin.setEnabled(true));
                }
            });
        });

        // Professional dialog popup instead of a standard Toast message
        tvForgotPassword.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Reset Password")
                    .setMessage("Please contact the administrator to reset your password.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SignUpActivity.class));
            finish();
        });
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}