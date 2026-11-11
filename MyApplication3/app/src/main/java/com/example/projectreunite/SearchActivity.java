package com.example.projectreunite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
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

public class SearchActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ReportAdapter adapter;
    private final List<Report> allReports = new ArrayList<>();
    private final List<Report> filteredReports = new ArrayList<>();
    private String currentFilter = "ALL";
    private TextView filterAll, filterLost, filterFound;
    private String currentQuery = "";

    private boolean myItemsOnly = false;
    private boolean savedItemsOnly = false;
    private String currentUserName = "";
    private SharedPreferences savedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        myItemsOnly = getIntent().getBooleanExtra("FILTER_MY_ITEMS", false);
        savedItemsOnly = getIntent().getBooleanExtra("FILTER_SAVED_ITEMS", false);
        currentUserName = getSharedPreferences("reunite", MODE_PRIVATE).getString("user_name", "");
        savedPrefs = getSharedPreferences("saved_items", MODE_PRIVATE);

        filterAll = findViewById(R.id.filterAll);
        filterLost = findViewById(R.id.filterLost);
        filterFound = findViewById(R.id.filterFound);
        EditText etSearch = findViewById(R.id.etSearchBox);

        if (myItemsOnly) {
            etSearch.setHint("Search my uploads...");
        } else if (savedItemsOnly) {
            etSearch.setHint("Search saved items...");
        }

        RecyclerView rv = findViewById(R.id.rvSearchResults);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(filteredReports);
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        filterAll.setOnClickListener(v -> setFilter("ALL"));
        filterLost.setOnClickListener(v -> setFilter("LOST"));
        filterFound.setOnClickListener(v -> setFilter("FOUND"));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        findViewById(R.id.navSearch).setOnClickListener(v -> {});
        findViewById(R.id.navAdd).setOnClickListener(v ->
                startActivity(new Intent(this, ReportLostActivity.class)));
        findViewById(R.id.navMessages).setOnClickListener(v ->
                startActivity(new Intent(this, MessagesActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            if (!myItemsOnly && !savedItemsOnly) startActivity(new Intent(this, ProfileActivity.class));
        });

        fetchReports();
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterUI();
        applyFilters();
    }

    private void updateFilterUI() {
        resetFilterButton(filterAll);
        resetFilterButton(filterLost);
        resetFilterButton(filterFound);

        TextView selected = filterAll;
        if (currentFilter.equals("LOST")) selected = filterLost;
        else if (currentFilter.equals("FOUND")) selected = filterFound;

        selected.setBackgroundResource(R.drawable.bg_rounded_primary);
        selected.setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private void resetFilterButton(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_rounded_white);
        tv.setTextColor(ContextCompat.getColor(this, R.color.gray));
    }

    private void fetchReports() {
        executor.execute(() -> {
            try {
                JSONObject response = ApiClient.get("api_reports.php");
                if ("success".equals(response.optString("status"))) {
                    JSONArray data = response.getJSONArray("data");
                    allReports.clear();
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        String rawDetails = obj.optString("details_description", "Unnamed Item - ");
                        String name = rawDetails.contains(" - ") ? rawDetails.split(" - ")[0] : rawDetails;
                        String desc = rawDetails.contains(" - ") ? rawDetails.substring(rawDetails.indexOf(" - ") + 3) : "";
                        String photo = obj.optString("photo_image", "");
                        if (photo.isEmpty() || photo.equals("null")) photo = obj.optString("image_url", "");

                        allReports.add(new Report(
                                obj.getInt("id"),
                                obj.optString("report_code", obj.optString("report_id")),
                                name,
                                desc,
                                obj.optString("location", "Unknown"),
                                obj.optString("date_filed"),
                                photo,
                                obj.optString("filed_by_user", "Anonymous")
                        ));
                    }
                    runOnUiThread(this::applyFilters);
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Fetch error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void applyFilters() {
        filteredReports.clear();
        for (Report r : allReports) {
            if (myItemsOnly && !r.filedBy.equals(currentUserName)) continue;
            if (savedItemsOnly && !savedPrefs.getBoolean(String.valueOf(r.id), false)) continue;

            boolean matchesFilter = currentFilter.equals("ALL") || r.status.equalsIgnoreCase(currentFilter);
            String safeReportId = r.reportId != null ? r.reportId.toLowerCase() : "";
            String safeLocation = r.location != null ? r.location.toLowerCase() : "";
            String safeItemName = r.itemName != null ? r.itemName.toLowerCase() : "";

            boolean matchesQuery = currentQuery.isEmpty() ||
                    safeReportId.contains(currentQuery.toLowerCase()) ||
                    safeLocation.contains(currentQuery.toLowerCase()) ||
                    safeItemName.contains(currentQuery.toLowerCase());

            if (matchesFilter && matchesQuery) filteredReports.add(r);
        }
        adapter.notifyDataSetChanged();
    }

    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}