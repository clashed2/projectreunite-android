package com.example.projectreunite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ReportAdapter adapter;
    private final List<Report> reportList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        RecyclerView rvRecentReports = findViewById(R.id.rvRecentReports);
        rvRecentReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(reportList);
        rvRecentReports.setAdapter(adapter);

        findViewById(R.id.btnReportLost).setOnClickListener(v ->
                startActivity(new Intent(this, ReportLostActivity.class)));

        findViewById(R.id.btnReportFound).setOnClickListener(v ->
                startActivity(new Intent(this, ReportFoundActivity.class)));

        findViewById(R.id.btnNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        findViewById(R.id.navAdd).setOnClickListener(v ->
                startActivity(new Intent(this, ReportLostActivity.class)));

        findViewById(R.id.navHome).setOnClickListener(v -> {});
        findViewById(R.id.navSearch).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        findViewById(R.id.navMessages).setOnClickListener(v ->
                startActivity(new Intent(this, MessagesActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchReports();
    }

    private void fetchReports() {
        executor.execute(() -> {
            try {
                JSONObject response = ApiClient.get("api_reports.php");
                if ("success".equals(response.optString("status"))) {
                    JSONArray data = response.optJSONArray("data");
                    reportList.clear();
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.optJSONObject(i);
                            if (obj != null) {
                                String rawDetails = obj.optString("details_description", "Unnamed Item - ");
                                String name = rawDetails.contains(" - ") ? rawDetails.split(" - ")[0] : rawDetails;
                                String desc = rawDetails.contains(" - ") ? rawDetails.substring(rawDetails.indexOf(" - ") + 3) : "";

                                // Safely grab the image string regardless of what key your PHP uses
                                String photo = obj.optString("photo_image", "");
                                if (photo.isEmpty() || photo.equals("null")) {
                                    photo = obj.optString("image_url", "");
                                }

                                reportList.add(new Report(
                                        obj.optInt("id"),
                                        obj.optString("report_code", obj.optString("report_id")),
                                        name,
                                        desc,
                                        obj.optString("location", "Unknown"),
                                        obj.optString("date_filed"),
                                        photo,
                                        obj.optString("filed_by_user", "Unknown")
                                ));
                            }
                        }
                    }
                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                } else {
                    String msg = response.optString("message", "Failed to fetch reports");
                    runOnUiThread(() -> Toast.makeText(HomeActivity.this, msg, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(HomeActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}