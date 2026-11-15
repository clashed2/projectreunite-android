package com.example.projectreunite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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
    private TextView filterAll, filterLost, filterFound, tvEmptyState, tvSearchTitle;
    private String currentQuery = "";

    private boolean myItemsOnly = false;
    private boolean savedItemsOnly = false;
    private String currentUserName = "";
    private int currentUserId = 0;
    private SharedPreferences savedPrefs;

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
        setContentView(R.layout.activity_search);

        myItemsOnly = getIntent().getBooleanExtra("FILTER_MY_ITEMS", false);
        savedItemsOnly = getIntent().getBooleanExtra("FILTER_SAVED_ITEMS", false);

        SharedPreferences mainPrefs = getSharedPreferences("reunite", MODE_PRIVATE);
        currentUserName = mainPrefs.getString("user_name", "");
        currentUserId = mainPrefs.getInt("user_id", 0);
        savedPrefs = getUserSavedPrefs();

        tvSearchTitle = findViewById(R.id.tvSearchTitle);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        filterAll = findViewById(R.id.filterAll);
        filterLost = findViewById(R.id.filterLost);
        filterFound = findViewById(R.id.filterFound);
        EditText etSearch = findViewById(R.id.etSearchBox);

        if (myItemsOnly) {
            if (tvSearchTitle != null) tvSearchTitle.setText("My Reported Items");
            etSearch.setHint("Search my reported items...");
        } else if (savedItemsOnly) {
            if (tvSearchTitle != null) tvSearchTitle.setText("Saved Items");
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
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                        if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = obj.optString("image_url", "");
                        if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = obj.optString("photo", "");
                        if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = obj.optString("image", "");
                        if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = obj.optString("file_name", "");
                        if (photo.isEmpty() || photo.equalsIgnoreCase("null")) photo = obj.optString("picture", "");

                        int userIdWhoFiled = obj.optInt("user_id_who_filed", obj.optInt("user_id", 0));
                        String filedBy = obj.optString("filed_by_user", obj.optString("username", "Anonymous"));
                        String category = obj.optString("category_name", obj.optString("category", "General"));

                        allReports.add(new Report(
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
            if (myItemsOnly) {
                boolean isMyItem = (r.userIdWhoFiled > 0 && currentUserId > 0 && r.userIdWhoFiled == currentUserId)
                        || (r.filedBy != null && !r.filedBy.isEmpty() && r.filedBy.equalsIgnoreCase(currentUserName));
                if (!isMyItem) continue;
            }

            if (savedItemsOnly) {
                boolean isSaved = savedPrefs.getBoolean(String.valueOf(r.id), false);
                if (!isSaved) continue;
            }

            boolean matchesFilter = currentFilter.equals("ALL") || r.status.equalsIgnoreCase(currentFilter);
            String safeReportId = r.reportId != null ? r.reportId.toLowerCase() : "";
            String safeLocation = r.location != null ? r.location.toLowerCase() : "";
            String safeItemName = r.itemName != null ? r.itemName.toLowerCase() : "";
            String safeDesc = r.description != null ? r.description.toLowerCase() : "";

            boolean matchesQuery = currentQuery.isEmpty() ||
                    safeReportId.contains(currentQuery.toLowerCase()) ||
                    safeLocation.contains(currentQuery.toLowerCase()) ||
                    safeItemName.contains(currentQuery.toLowerCase()) ||
                    safeDesc.contains(currentQuery.toLowerCase());

            if (matchesFilter && matchesQuery) filteredReports.add(r);
        }

        adapter.notifyDataSetChanged();

        if (tvEmptyState != null) {
            if (filteredReports.isEmpty()) {
                if (myItemsOnly) {
                    tvEmptyState.setText("You have not reported any missing items yet.");
                } else if (savedItemsOnly) {
                    tvEmptyState.setText("No saved items in your profile.");
                } else {
                    tvEmptyState.setText("No matching items found.");
                }
                tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                tvEmptyState.setVisibility(View.GONE);
            }
        }
    }

    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}