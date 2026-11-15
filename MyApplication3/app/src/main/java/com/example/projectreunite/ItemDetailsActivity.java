package com.example.projectreunite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ItemDetailsActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private int reportId = -1;

    private TextView tvItemName, tvLocation, tvDate, tvFiler, tvDescription, tvReportStatus, tvCategory, tvReportCode;
    private ImageView ivItemPhoto;
    private Button btnMarkFound, btnContact;

    private SharedPreferences getUserSavedPrefs() {
        SharedPreferences mainPrefs = getSharedPreferences("reunite", MODE_PRIVATE);
        int userId = mainPrefs.getInt("user_id", 0);
        String userName = mainPrefs.getString("user_name", "user");
        String prefKey = "saved_items_user_" + (userId > 0 ? userId : userName.replaceAll("\\s+", "_"));
        return getSharedPreferences(prefKey, MODE_PRIVATE);
    }

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
        tvReportStatus = findViewById(R.id.tvReportStatus);
        tvCategory = findViewById(R.id.tvCategory);
        tvReportCode = findViewById(R.id.tvReportCode);
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
                    String category = data.optString("category_name", data.optString("category", "General"));

                    String photo = data.optString("picture_image", "");
                    if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = data.optString("photo_image", "");
                    if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = data.optString("image_url", "");
                    if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = data.optString("photo", "");
                    if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = data.optString("image", "");
                    if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = data.optString("file_name", "");

                    SharedPreferences mainPrefs = getSharedPreferences("reunite", MODE_PRIVATE);
                    String currentUserName = mainPrefs.getString("user_name", "");
                    int currentUserId = mainPrefs.getInt("user_id", 0);
                    int userWhoFiled = data.optInt("user_id_who_filed", data.optInt("user_id", 0));

                    boolean isOwner = (userWhoFiled > 0 && currentUserId > 0 && userWhoFiled == currentUserId)
                            || (!filedBy.isEmpty() && filedBy.equalsIgnoreCase(currentUserName));

                    String reportCode = data.optString("report_code", data.optString("report_id", ""));
                    if (reportCode.isEmpty()) reportCode = "RPT-" + id;

                    String reportType = data.optString("report_type", "");
                    boolean isLost = reportCode.toUpperCase().contains("LOST") || reportType.equalsIgnoreCase("LOST");

                    final String finalPhoto = photo;
                    final String finalReportCode = reportCode;
                    final String finalCategory = category;

                    runOnUiThread(() -> {
                        tvItemName.setText(itemName);
                        tvDescription.setText(desc);
                        tvLocation.setText(location);
                        tvDate.setText(date);
                        tvFiler.setText(filedBy);

                        if (tvReportCode != null) tvReportCode.setText("Report Code: " + finalReportCode);
                        if (tvCategory != null) tvCategory.setText(finalCategory);

                        if (tvReportStatus != null) {
                            tvReportStatus.setText(isLost ? "LOST" : "FOUND");
                            int statusTextColor = getResources().getColor(isLost ? R.color.status_lost : R.color.status_found);
                            int statusBgColor = getResources().getColor(isLost ? R.color.status_lost_bg : R.color.status_found_bg);
                            tvReportStatus.setTextColor(statusTextColor);
                            GradientDrawable gd = new GradientDrawable();
                            gd.setColor(statusBgColor);
                            gd.setCornerRadius(16);
                            tvReportStatus.setBackground(gd);
                        }

                        if (!finalPhoto.isEmpty()) {
                            displayImage(ivItemPhoto, finalPhoto);
                        }

                        if (isOwner) {
                            btnContact.setVisibility(View.GONE);
                            if (isLost) {
                                btnMarkFound.setVisibility(View.VISIBLE);
                                btnMarkFound.setText("Mark as Found");
                                btnMarkFound.setEnabled(true);
                                btnMarkFound.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary_green)));
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

                            // User-Scoped Bookmark Logic
                            SharedPreferences savedPrefs = getUserSavedPrefs();
                            final boolean[] isSaved = {savedPrefs.getBoolean(String.valueOf(id), false)};

                            updateSaveButtonUI(btnMarkFound, isSaved[0]);

                            btnMarkFound.setOnClickListener(v -> {
                                isSaved[0] = !isSaved[0];
                                savedPrefs.edit().putBoolean(String.valueOf(id), isSaved[0]).apply();
                                updateSaveButtonUI(btnMarkFound, isSaved[0]);
                                Toast.makeText(ItemDetailsActivity.this, isSaved[0] ? "Item saved to your profile!" : "Item removed from saved items", Toast.LENGTH_SHORT).show();
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
                runOnUiThread(() -> Toast.makeText(this, "Failed to load details: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void displayImage(ImageView imageView, String photoData) {
        if (photoData == null || photoData.isEmpty() || photoData.equalsIgnoreCase("null")) {
            imageView.setVisibility(View.GONE);
            return;
        }

        imageView.setTag(photoData);

        String cleanData = photoData.trim();
        boolean isBase64 = false;
        if (cleanData.startsWith("data:image") || cleanData.contains("base64,")) {
            isBase64 = true;
        } else if (!cleanData.startsWith("http://") && !cleanData.startsWith("https://") && !cleanData.contains(".") && cleanData.length() > 50) {
            isBase64 = true;
        }

        if (isBase64) {
            executor.execute(() -> {
                try {
                    String base64Str = cleanData;
                    if (base64Str.contains("base64,")) {
                        base64Str = base64Str.substring(base64Str.indexOf("base64,") + 7);
                    }
                    byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    if (bitmap != null) {
                        imageView.post(() -> {
                            if (photoData.equals(imageView.getTag())) {
                                imageView.setImageBitmap(bitmap);
                                imageView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                } catch (Exception ignored) {}
            });
        } else {
            String imageUrl;
            if (cleanData.startsWith("http://") || cleanData.startsWith("https://")) {
                imageUrl = cleanData;
            } else {
                String baseUrl = ApiClient.BASE_URL.endsWith("/") ? ApiClient.BASE_URL : ApiClient.BASE_URL + "/";
                if (cleanData.startsWith("uploads/")) {
                    imageUrl = baseUrl + cleanData;
                } else if (cleanData.startsWith("/uploads/")) {
                    imageUrl = baseUrl + cleanData.substring(1);
                } else if (cleanData.startsWith("/")) {
                    imageUrl = baseUrl + cleanData.substring(1);
                } else {
                    imageUrl = baseUrl + "uploads/" + cleanData;
                }
            }

            executor.execute(() -> {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)");
                    conn.connect();

                    InputStream is = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    if (bitmap != null) {
                        imageView.post(() -> {
                            if (photoData.equals(imageView.getTag())) {
                                imageView.setImageBitmap(bitmap);
                                imageView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                } catch (Exception ignored) {}
            });
        }
    }

    private void updateSaveButtonUI(Button button, boolean isSaved) {
        button.setEnabled(true);
        if (isSaved) {
            button.setText("Saved to Profile");
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.gray)));
        } else {
            button.setText("Save Item");
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary_green)));
        }
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

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}