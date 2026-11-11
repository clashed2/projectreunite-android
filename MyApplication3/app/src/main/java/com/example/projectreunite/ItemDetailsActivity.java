package com.example.projectreunite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ItemDetailsActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private int reportId = -1;

    private TextView tvItemName, tvLocation, tvDate, tvFiler, tvDescription;
    private ImageView ivItemPhoto;
    private Button btnMarkFound, btnContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_details);

        reportId = getIntent().getIntExtra("report_id", -1);

        tvItemName = findViewById(R.id.tvItemName);
        tvLocation = findViewById(R.id.tvLocation);
        tvDate = findViewById(R.id.tvDate);
        tvFiler = findViewById(R.id.tvFiler);
        tvDescription = findViewById(R.id.tvDescription);
        ivItemPhoto = findViewById(R.id.ivItemPhoto);
        btnMarkFound = findViewById(R.id.btnMarkFound);
        btnContact = findViewById(R.id.btnContact);

        btnMarkFound.setVisibility(View.GONE);
        btnContact.setVisibility(View.GONE);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (reportId != -1) {
            fetchReportDetails(reportId);
        }
    }

    private void fetchReportDetails(int id) {
        executor.execute(() -> {
            try {
                JSONObject response = ApiClient.get("api_reports.php?id=" + id);
                if (response.getString("status").equals("success")) {
                    JSONObject data = response.getJSONObject("data");

                    String rawDetails = data.optString("details_description", "Item");
                    String itemName = rawDetails.contains(" - ") ? rawDetails.split(" - ")[0] : rawDetails;
                    String desc = rawDetails.contains(" - ") ? rawDetails.substring(rawDetails.indexOf(" - ") + 3) : "No description provided.";

                    String location = data.optString("location", "Unknown Location");
                    String date = data.optString("date_filed", "Unknown Date");
                    String filedBy = data.optString("filed_by_user", "Anonymous");

                    String photo = data.optString("photo_image", "");
                    if (photo.isEmpty() || photo.equals("null")) photo = data.optString("image_url", "");

                    String currentUserName = getSharedPreferences("reunite", MODE_PRIVATE).getString("user_name", "");
                    boolean isOwner = filedBy.equals(currentUserName);

                    String reportCode = data.optString("report_code", data.optString("report_id", ""));
                    boolean isLost = reportCode.contains("LOST") || data.optString("report_type", "").equalsIgnoreCase("LOST");

                    final String finalPhoto = photo;

                    runOnUiThread(() -> {
                        tvItemName.setText(itemName);
                        tvDescription.setText(desc);
                        tvLocation.setText("📍 " + location);
                        tvDate.setText("🕒 " + date);
                        tvFiler.setText(filedBy);

                        if (!finalPhoto.isEmpty()) {
                            ivItemPhoto.setVisibility(View.VISIBLE);
                            String baseUrl = ApiClient.BASE_URL.endsWith("/") ? ApiClient.BASE_URL : ApiClient.BASE_URL + "/";
                            loadImage(ivItemPhoto, baseUrl + "uploads/" + finalPhoto);
                        }

                        if (isOwner) {
                            btnContact.setVisibility(View.GONE);
                            if (isLost) {
                                btnMarkFound.setVisibility(View.VISIBLE);
                                btnMarkFound.setText("Mark as Found");
                                btnMarkFound.setOnClickListener(v -> updateItemStatus(id));
                            } else {
                                btnMarkFound.setVisibility(View.VISIBLE);
                                btnMarkFound.setText("Item Resolved");
                                btnMarkFound.setEnabled(false);
                                btnMarkFound.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.gray)));
                            }
                        } else {
                            btnContact.setVisibility(View.VISIBLE);
                            btnMarkFound.setVisibility(View.VISIBLE);

                            // Smart Bookmark Logic
                            SharedPreferences savedPrefs = getSharedPreferences("saved_items", MODE_PRIVATE);
                            boolean isSaved = savedPrefs.getBoolean(String.valueOf(id), false);

                            btnMarkFound.setText(isSaved ? "Saved to Profile" : "Save Item");
                            if (isSaved) btnMarkFound.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.gray)));

                            btnMarkFound.setOnClickListener(v -> {
                                if (!savedPrefs.getBoolean(String.valueOf(id), false)) {
                                    savedPrefs.edit().putBoolean(String.valueOf(id), true).apply();
                                    btnMarkFound.setText("Saved to Profile");
                                    btnMarkFound.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.gray)));
                                    Toast.makeText(ItemDetailsActivity.this, "Item saved! View it in your Profile.", Toast.LENGTH_SHORT).show();
                                }
                            });

                            // Routing to Chat
                            btnContact.setOnClickListener(v -> {
                                Intent intent = new Intent(ItemDetailsActivity.this, MessagesActivity.class);
                                intent.putExtra("CHAT_USER", filedBy);
                                intent.putExtra("CHAT_ITEM", itemName);
                                startActivity(intent);
                            });
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateItemStatus(int id) {
        btnMarkFound.setEnabled(false);
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("report_type", "FOUND");
                JSONObject response = ApiClient.patch("api_reports.php?id=" + id, body);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Status successfully updated to FOUND!", Toast.LENGTH_SHORT).show();
                    fetchReportDetails(id);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnMarkFound.setEnabled(true);
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void loadImage(ImageView imageView, String url) {
        executor.execute(() -> {
            try {
                InputStream is = new URL(url).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                if (bitmap != null) {
                    imageView.post(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Exception ignored) {}
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}