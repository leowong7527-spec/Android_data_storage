package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced HomeActivity with Search, Statistics, and Quick Access features
 * 
 * New Features (v1.0):
 * 1. Global Search - Search across all media categories
 * 2. Statistics Dashboard - Show item counts for each category
 * 3. Quick Access Cards - GridLayout with 2x2 category shortcuts
 */
public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;

    private EditText searchBox;
    private LinearLayout searchResultsContainer;
    private LinearLayout dashboardContainer;
    private GridLayout quickAccessGrid;

    // Dashboard stats
    private TextView statsText;
    private int totalMp3s = 0;
    private int totalBooks = 0;
    private int totalComics = 0;
    private int totalPhotos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize views
        initializeViews();
        setupNavigation();
        loadStatistics();
        setupSearchFunctionality();
        setupQuickAccessCards();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        searchBox = findViewById(R.id.searchBox);
        searchResultsContainer = findViewById(R.id.searchResultsContainer);
        dashboardContainer = findViewById(R.id.dashboardContainer);
        quickAccessGrid = findViewById(R.id.quickAccessGrid);
        statsText = findViewById(R.id.statsText);
    }

    private void setupNavigation() {
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                drawerLayout.closeDrawers();
            } else if (id == R.id.nav_books) {
                navigateToCategory("data.json", BookActivity.class, "book");
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_photos) {
                navigateToCategory("photo_data.json", PhotoCategoryActivity.class, "photo");
            } else if (id == R.id.nav_mp3) {
                navigateToCategory("mp3_data.json", RadioCategoryActivity.class, "MP3");
            } else if (id == R.id.nav_comics) {
                navigateToCategory("comic_data.json", ComicCategoryActivity.class, "comic");
            }
            return true;
        });
    }

    /**
     * Load statistics from JSON files
     */
    private void loadStatistics() {
        new Thread(() -> {
            try {
                totalMp3s = countItemsInJson("mp3_data.json", "categories");
                totalBooks = countItemsInJson("data.json", "categories");
                totalComics = countItemsInJson("comic_data.json", "categories");
                totalPhotos = countItemsInJson("photo_data.json", "categories");

                runOnUiThread(this::updateDashboard);
            } catch (Exception e) {
                Log.e(TAG, "Error loading statistics", e);
            }
        }).start();
    }

    /**
     * Count items in JSON file
     */
    private int countItemsInJson(String filename, String key) {
        try {
            File jsonFile = new File(getExternalFilesDir(null), filename);
            if (!jsonFile.exists()) {
                jsonFile = new File(getExternalFilesDir("Downloads"), filename);
            }

            if (!jsonFile.exists()) return 0;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                JSONObject obj = new JSONObject(sb.toString());
                if (obj.has(key)) {
                    return obj.getJSONArray(key).length();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error counting items in " + filename, e);
        }
        return 0;
    }

    /**
     * Update dashboard display
     */
    private void updateDashboard() {
        int total = totalMp3s + totalBooks + totalComics + totalPhotos;
        String stats = String.format(
                " 統計\\n %d 首歌曲   %d 本書籍   %d 部漫畫   %d 張照片\\n總計：%d 項",
                totalMp3s, totalBooks, totalComics, totalPhotos, total
        );
        statsText.setText(stats);
    }

    /**
     * Setup search functionality
     */
    private void setupSearchFunctionality() {
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    performGlobalSearch(s.toString());
                } else {
                    searchResultsContainer.removeAllViews();
                    dashboardContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Perform global search
     */
    private void performGlobalSearch(String query) {
        new Thread(() -> {
            List<SearchResult> results = new ArrayList<>();
            results.addAll(searchInJson("mp3_data.json", query, ""));
            results.addAll(searchInJson("data.json", query, ""));
            results.addAll(searchInJson("comic_data.json", query, ""));
            results.addAll(searchInJson("photo_data.json", query, ""));

            runOnUiThread(() -> displaySearchResults(results));
        }).start();
    }

    /**
     * Search in JSON file
     */
    private List<SearchResult> searchInJson(String filename, String query, String icon) {
        List<SearchResult> results = new ArrayList<>();
        try {
            File jsonFile = new File(getExternalFilesDir("Downloads"), filename);
            if (!jsonFile.exists()) return results;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                JSONObject obj = new JSONObject(sb.toString());
                JSONArray categories = obj.getJSONArray("categories");

                for (int i = 0; i < categories.length(); i++) {
                    JSONObject cat = categories.getJSONObject(i);
                    String catName = cat.getString("name");

                    if (catName.toLowerCase().contains(query.toLowerCase())) {
                        results.add(new SearchResult(icon + " " + catName, filename, "category"));
                    }

                    JSONArray folders = cat.getJSONArray("folders");
                    for (int j = 0; j < folders.length(); j++) {
                        JSONObject folder = folders.getJSONObject(j);
                        String folderName = folder.getString("name");

                        if (folderName.toLowerCase().contains(query.toLowerCase())) {
                            results.add(new SearchResult(
                                    icon + " " + folderName + " (" + catName + ")",
                                    filename, "folder"
                            ));
                        }

                        if (folder.has("files")) {
                            JSONArray files = folder.getJSONArray("files");
                            for (int k = 0; k < files.length(); k++) {
                                JSONObject file = files.getJSONObject(k);
                                String title = file.getString("title");

                                if (title.toLowerCase().contains(query.toLowerCase())) {
                                    results.add(new SearchResult(
                                            icon + " " + title + " (" + folderName + ")",
                                            filename, "file"
                                    ));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching " + filename, e);
        }
        return results;
    }

    /**
     * Display search results
     */
    private void displaySearchResults(List<SearchResult> results) {
        searchResultsContainer.removeAllViews();
        dashboardContainer.setVisibility(View.GONE);

        if (results.isEmpty()) {
            TextView noResults = new TextView(this);
            noResults.setText("未找到結果");
            noResults.setPadding(16, 16, 16, 16);
            searchResultsContainer.addView(noResults);
            return;
        }

        for (SearchResult result : results) {
            MaterialCardView card = new MaterialCardView(this);
            card.setCardElevation(4);
            card.setCardBackgroundColor(getResources().getColor(android.R.color.white));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 8, 8, 8);
            card.setLayoutParams(params);

            TextView textView = new TextView(this);
            textView.setText(result.title);
            textView.setPadding(16, 12, 16, 12);
            textView.setTextSize(14);

            card.addView(textView);
            searchResultsContainer.addView(card);
        }
    }

    /**
     * Setup quick access cards
     */
    private void setupQuickAccessCards() {
        addQuickAccessCard(" MP3 / 音樂", "mp3_data.json", RadioCategoryActivity.class);
        addQuickAccessCard(" 書籍", "data.json", BookActivity.class);
        addQuickAccessCard(" 漫畫", "comic_data.json", ComicCategoryActivity.class);
        addQuickAccessCard(" 照片", "photo_data.json", PhotoCategoryActivity.class);
    }

    /**
     * Add quick access card
     */
    private void addQuickAccessCard(String title, String filename, Class<?> targetActivity) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardElevation(4);
        card.setClickable(true);
        card.setFocusable(true);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(8, 8, 8, 8);
        card.setLayoutParams(params);

        TextView textView = new TextView(this);
        textView.setText(title);
        textView.setPadding(16, 24, 16, 24);
        textView.setTextSize(16);
        textView.setTextStyle(android.graphics.Typeface.BOLD);
        textView.setGravity(android.view.Gravity.CENTER);

        card.addView(textView);
        card.setOnClickListener(v -> navigateToCategory(filename, targetActivity, title));

        quickAccessGrid.addView(card);
    }

    /**
     * Navigate to category
     */
    private void navigateToCategory(String filename, Class<?> targetActivity, String label) {
        File cacheFile = new File(getExternalFilesDir("Downloads"), filename);
        if (cacheFile.exists()) {
            Intent intent = new Intent(this, targetActivity);
            intent.putExtra("json_path", cacheFile.getAbsolutePath());
            startActivity(intent);
        } else {
            Toast.makeText(this, "正在加載 " + label + " 數據...", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Search result data class
     */
    public static class SearchResult {
        public String title;
        public String filename;
        public String type;

        public SearchResult(String title, String filename, String type) {
            this.title = title;
            this.filename = filename;
            this.type = type;
        }
    }
}
