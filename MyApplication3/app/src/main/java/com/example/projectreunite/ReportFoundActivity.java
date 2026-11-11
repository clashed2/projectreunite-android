package com.example.projectreunite;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportFoundActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String encodedImage = null;
    private ImageView ivPhotoPreview;
    private Spinner spinnerCategory;

    private final ArrayList<String> categoryNames = new ArrayList<>();
    private final HashMap<String, Integer> categoryMap = new HashMap<>();

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream is = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        ivPhotoPreview.setImageBitmap(bitmap);
                        ivPhotoPreview.setVisibility(View.VISIBLE);
                        findViewById(R.id.tvAddPhoto).setVisibility(View.GONE);
                        encodedImage = encodeImage(bitmap);
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_found);

        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnUploadPhoto).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        findViewById(R.id.btnSubmitFound).setOnClickListener(v -> submitReport());

        fetchCategories();
    }

    private void fetchCategories() {
        executor.execute(() -> {
            try {
                JSONObject response = ApiClient.get("api_categories.php");
                if ("success".equals(response.optString("status"))) {
                    JSONArray data = response.getJSONArray("data");
                    categoryNames.clear();
                    categoryMap.clear();
                    categoryNames.add("Select Category");

                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        int id = obj.getInt("id");
                        String name = obj.getString("name");
                        categoryNames.add(name);
                        categoryMap.put(name, id);
                    }

                    runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryNames);
                        spinnerCategory.setAdapter(adapter);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    categoryNames.add("Select Category");
                    categoryNames.add("Bag");
                    categoryNames.add("Electronics");
                    categoryNames.add("Wallet");
                    categoryNames.add("ID/Cards");
                    categoryNames.add("Clothing");
                    categoryNames.add("Other");

                    categoryMap.put("Bag", 1);
                    categoryMap.put("Electronics", 2);
                    categoryMap.put("Wallet", 3);
                    categoryMap.put("ID/Cards", 4);
                    categoryMap.put("Clothing", 5);
                    categoryMap.put("Other", 6);

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryNames);
                    spinnerCategory.setAdapter(adapter);
                });
            }
        });
    }

    private String encodeImage(Bitmap bitmap) {
        int maxWidth = 1024;
        int maxHeight = 1024;
        float ratio = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
        if (ratio < 1.0) {
            int width = Math.round(ratio * bitmap.getWidth());
            int height = Math.round(ratio * bitmap.getHeight());
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    private void submitReport() {
        EditText item = findViewById(R.id.etItemName);
        EditText description = findViewById(R.id.etDescription);
        EditText location = findViewById(R.id.etLocation);
        Button btnSubmit = findViewById(R.id.btnSubmitFound);

        int userId = getSharedPreferences("reunite", MODE_PRIVATE).getInt("user_id", 0);
        String selectedCategoryName = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "";

        String itemName = item.getText().toString().trim();
        String desc = description.getText().toString().trim();
        String loc = location.getText().toString().trim();

        if (userId == 0) { Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show(); return; }
        if (itemName.isEmpty() || loc.isEmpty()) { Toast.makeText(this, "Item name and location are required", Toast.LENGTH_SHORT).show(); return; }
        if (selectedCategoryName.equals("Select Category")) { Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show(); return; }

        int realCategoryId = categoryMap.getOrDefault(selectedCategoryName, 1);
        btnSubmit.setEnabled(false);

        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("user_id_who_filed", userId);
                body.put("item_name", itemName);
                body.put("description", desc);
                body.put("details_description", itemName + " - " + desc);
                body.put("location", loc);
                body.put("status", "active");
                body.put("report_type", "FOUND");

                JSONArray categoryArray = new JSONArray();
                categoryArray.put(realCategoryId);
                body.put("category_ids", categoryArray);

                if (encodedImage != null) body.put("encoded_image", encodedImage);

                JSONObject response = ApiClient.post("api_reports.php", body);

                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    if ("success".equals(response.optString("status"))) {
                        Toast.makeText(this, "Found report submitted!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, SuccessActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "API Error: " + response.optString("message"), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Connection Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}