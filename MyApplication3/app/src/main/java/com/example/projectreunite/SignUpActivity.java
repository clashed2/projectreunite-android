package com.example.projectreunite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignUpActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        EditText etFullName = findViewById(R.id.etFullName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);
        TextView tvLogIn = findViewById(R.id.tvLogIn);

        btnCreateAccount.setOnClickListener(v -> {
            String name = etFullName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 5) {
                Toast.makeText(this, "Password must be at least 5 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            btnCreateAccount.setEnabled(false);
            executor.execute(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("name", name);
                    body.put("username", email); // PHP requires 'username'
                    body.put("email", email);
                    body.put("password", password);
                    body.put("phone_number", ""); // Optional but good to include

                    JSONObject response = ApiClient.post("api_register.php", body);
                    runOnUiThread(() -> {
                        Toast.makeText(this, response.optString("message", "Account created successfully"), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                        finish();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                } finally {
                    runOnUiThread(() -> btnCreateAccount.setEnabled(true));
                }
            });
        });

        tvLogIn.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
            finish();
        });
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
