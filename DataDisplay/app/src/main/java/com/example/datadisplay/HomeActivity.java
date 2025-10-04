// File: HomeActivity.java
package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;

    // Keep JSON in memory for reuse
    private String cachedJsonString;
    private boolean isDownloadComplete = false; // Indicates if the download attempt has finished

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // 🔹 Try to load from cache first
        loadCachedJson();

        // 🔹 Download JSON in background (will update cache)
        // This runs in a separate thread and does not block the UI.
        downloadJsonInBackground();

        // 🔹 Navigation drawer clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                drawerLayout.closeDrawers();
            } else if (id == R.id.nav_books) {
                startActivity(new Intent(this, BookActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_photos) {
                startActivity(new Intent(this, PhotoCategoryActivity.class));
            } else if (id == R.id.nav_mp3) {
                handleMp3Navigation();
            }
            return true;
        });
    }

    private void loadCachedJson() {
        try {
            JSONObject cached = loadJsonFromCache();
            if (cached != null) {
                cachedJsonString = cached.toString();
                Log.d(TAG, "Loaded JSON from cache");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading cached JSON", e);
        }
    }

    private void downloadJsonInBackground() {
        new Thread(() -> {
            try {
                URL url = new URL("https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/mp3_data.json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                reader.close();
                cachedJsonString = builder.toString();

                // Save to cache
                saveJsonToCache(cachedJsonString);
                isDownloadComplete = true; // Mark download as complete

                // Removed auto-navigation logic here.
                // The JSON is now only downloaded and cached.
                Log.d(TAG, "JSON download complete and cached.");

            } catch (Exception e) {
                Log.e(TAG, "Error downloading JSON", e);
                isDownloadComplete = true; // Mark download as complete even if failed
                runOnUiThread(() ->
                        Toast.makeText(HomeActivity.this, "Error downloading JSON", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void handleMp3Navigation() {
        // Check if we have JSON data
        if (cachedJsonString != null && !cachedJsonString.isEmpty()) {
            // If JSON is available, navigate directly to RadioCategoryActivity
            Intent intent = new Intent(HomeActivity.this, RadioCategoryActivity.class);
            intent.putExtra("json", cachedJsonString);
            startActivity(intent);
        } else {
            // No JSON available yet or download failed
            if (isDownloadComplete) {
                // Download completed but failed or resulted in empty data
                Toast.makeText(this, "No data available. Please check your connection.", Toast.LENGTH_LONG).show();
            } else {
                // Download still in progress
                Toast.makeText(this, "Loading data... Please wait a moment and try again.", Toast.LENGTH_LONG).show();

                // Optional: Retry loading from cache immediately in case it just finished
                loadCachedJson();
                if (cachedJsonString != null) {
                    // If cache load succeeded, then navigate
                    Intent intent = new Intent(this, RadioCategoryActivity.class);
                    intent.putExtra("json", cachedJsonString);
                    startActivity(intent);
                }
            }
        }
    }

    private void saveJsonToCache(String json) {
        try {
            File jsonFile = new File(getCacheDir(), "mp3_data.json");
            FileOutputStream fos = new FileOutputStream(jsonFile);
            fos.write(json.getBytes());
            fos.close();
            Log.d(TAG, "JSON cached locally to " + jsonFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error saving JSON to cache", e);
        }
    }

    private JSONObject loadJsonFromCache() {
        try {
            File jsonFile = new File(getCacheDir(), "mp3_data.json");
            if (!jsonFile.exists()) {
                Log.d(TAG, "Cache file does not exist");
                return null;
            }

            FileInputStream fis = new FileInputStream(jsonFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            return new JSONObject(builder.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error loading JSON from cache", e);
            return null;
        }
    }
}