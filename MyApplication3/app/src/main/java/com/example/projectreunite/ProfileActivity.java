package com.example.projectreunite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ReportAdapter adapter;
    private final List<Report> displayedReports = new ArrayList<>();
    private final List<Report> myReportsList = new ArrayList<>();
    private final List<Report> savedReportsList = new ArrayList<>();

    private TextView btnMyItems, btnSavedItems, tvSectionTitle, tvProfileEmpty, tvMyReportsCount, tvSavedCount;
    private boolean showingMyItems = true;

    private String currentUserName = "";
    private int currentUserId = 0;

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
        setContentView(R.layout.activity_profile);

        SharedPreferences prefs = getSharedPreferences("reunite", MODE_PRIVATE);
        currentUserName = prefs.getString("user_name", "Unknown User");
        String email = prefs.getString("user_email", "No email provided");
        currentUserId = prefs.getInt("user_id", 0);

        TextView tvName = findViewById(R.id.tvProfileName);
        TextView tvEmail = findViewById(R.id.tvProfileEmail);
        tvName.setText(currentUserName);
        tvEmail.setText(email);

        btnMyItems = findViewById(R.id.btnMyItems);
        btnSavedItems = findViewById(R.id.btnSavedItems);
        tvSectionTitle = findViewById(R.id.tvSectionTitle);
        tvProfileEmpty = findViewById(R.id.tvProfileEmpty);
        tvMyReportsCount = findViewById(R.id.tvMyReportsCount);
        tvSavedCount = findViewById(R.id.tvSavedCount);

        RecyclerView rv = findViewById(R.id.rvProfileReports);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(displayedReports);
        rv.setAdapter(adapter);

        btnMyItems.setOnClickListener(v -> switchTab(true));
        btnSavedItems.setOnClickListener(v -> switchTab(false));

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navAdd).setOnClickListener(v ->
                startActivity(new Intent(this, ReportLostActivity.class)));
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        findViewById(R.id.navSearch).setOnClickListener(v -> {
            startActivity(new Intent(this, SearchActivity.class));
            finish();
        });
        findViewById(R.id.navMessages).setOnClickListener(v -> {
            startActivity(new Intent(this, MessagesActivity.class));
            finish();
        });
        findViewById(R.id.navProfile).setOnClickListener(v -> {});
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchProfileData();
    }

    private void switchTab(boolean myItems) {
        showingMyItems = myItems;
        if (myItems) {
            btnMyItems.setBackgroundResource(R.drawable.bg_rounded_primary);
            btnMyItems.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnSavedItems.setBackground(null);
            btnSavedItems.setTextColor(ContextCompat.getColor(this, R.color.gray));
            if (tvSectionTitle != null) tvSectionTitle.setText("My Reported Missing Items");
        } else {
            btnSavedItems.setBackgroundResource(R.drawable.bg_rounded_primary);
            btnSavedItems.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnMyItems.setBackground(null);
            btnMyItems.setTextColor(ContextCompat.getColor(this, R.color.gray));
            if (tvSectionTitle != null) tvSectionTitle.setText("Saved Items");
        }
        updateDisplayedList();
    }

    private void fetchProfileData() {
        executor.execute(() -> {
            try {
                JSONObject response = ApiClient.get("api_reports.php");
                if ("success".equals(response.optString("status"))) {
                    JSONArray data = response.getJSONArray("data");

                    myReportsList.clear();
                    savedReportsList.clear();

                    SharedPreferences savedPrefs = getUserSavedPrefs();

                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        String rawDetails = obj.optString("details_description", "Unnamed Item - ");
                        String name = rawDetails.contains(" - ") ? rawDetails.split(" - ")[0] : rawDetails;
                        String desc = rawDetails.contains(" - ") ? rawDetails.substring(rawDetails.indexOf(" - ") + 3) : "";

                        String photo = obj.optString("photo_image", "");
                        if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = obj.optString("image_url", "");
                        if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = obj.optString("photo", "");
                        if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = obj.optString("image", "");
                        if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = obj.optString("file_name", "");
                        if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = obj.optString("picture", "");

                        int userIdWhoFiled = obj.optInt("user_id_who_filed", obj.optInt("user_id", 0));
                        String filedBy = obj.optString("filed_by_user", obj.optString("username", "Anonymous"));
                        String category = obj.optString("category_name", obj.optString("category", "General"));

                        Report report = new Report(
                                obj.getInt("id"),
                                obj.optString("report_code", obj.optString("report_id")),
                                name,
                                desc,
                                obj.optString("location", "Unknown"),
                                obj.optString("date_filed"),
                                photo,
                                filedBy,
                                userIdWhoFiled,
                                category
                        );

                        boolean isMyItem = (userIdWhoFiled > 0 && currentUserId > 0 && userIdWhoFiled == currentUserId)
                                || (filedBy != null && !filedBy.isEmpty() && filedBy.equalsIgnoreCase(currentUserName));

                        if (isMyItem) {
                            myReportsList.add(report);
                        }

                        boolean isSaved = savedPrefs.getBoolean(String.valueOf(report.id), false);
                        if (isSaved) {
                            savedReportsList.add(report);
                        }
                    }

                    runOnUiThread(() -> {
                        if (tvMyReportsCount != null) tvMyReportsCount.setText(String.valueOf(myReportsList.size()));
                        if (tvSavedCount != null) tvSavedCount.setText(String.valueOf(savedReportsList.size()));
                        updateDisplayedList();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to update profile data", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateDisplayedList() {
        displayedReports.clear();
        if (showingMyItems) {
            displayedReports.addAll(myReportsList);
        } else {
            displayedReports.addAll(savedReportsList);
        }
        adapter.notifyDataSetChanged();

        if (tvProfileEmpty != null) {
            if (displayedReports.isEmpty()) {
                tvProfileEmpty.setText(showingMyItems ? "You haven't reported any missing items yet." : "No saved items in your bookmarks.");
                tvProfileEmpty.setVisibility(View.VISIBLE);
            } else {
                tvProfileEmpty.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}