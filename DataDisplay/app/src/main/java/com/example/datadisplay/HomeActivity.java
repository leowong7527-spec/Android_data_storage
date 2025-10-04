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
    private String cachedMp3JsonString;
    private String cachedBookJsonString;
    private String cachedComicJsonString;

    private boolean isMp3DownloadComplete = false;
    private boolean isBookDownloadComplete = false;

    private boolean isComicDownloadComplete = false;

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

        // Load from cache first
        cachedMp3JsonString = loadJsonFromCache("mp3_data.json");
        cachedBookJsonString = loadJsonFromCache("data.json");
        cachedComicJsonString = loadJsonFromCache("comic_data.json");

        // Download in background
        downloadJsonInBackground(
                "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/mp3_data.json",
                "mp3_data.json",
                true
        );

        //book
        downloadJsonInBackground(
                "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/data.json",
                "data.json",
                false
        );

        downloadJsonInBackground(
                "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/comic_data.json",
                "comic_data.json",
                false
        );

        // Navigation drawer clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                drawerLayout.closeDrawers();
            } else if (id == R.id.nav_books) {
                handleBookNavigation();
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_photos) {
                startActivity(new Intent(this, PhotoCategoryActivity.class));
            } else if (id == R.id.nav_mp3) {
                handleMp3Navigation();
            }else if (id == R.id.nav_comics) {
                handleComicNavigation();
            }

            return true;
        });
    }

    private void downloadJsonInBackground(String urlString, String filename, boolean isMp3) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                reader.close();

                String jsonString = builder.toString();
                saveJsonToCache(filename, jsonString);

                // ✅ Distinguish between MP3, Book, and Comic
                if ("mp3_data.json".equals(filename)) {
                    cachedMp3JsonString = jsonString;
                    isMp3DownloadComplete = true;
                } else if ("data.json".equals(filename)) {
                    cachedBookJsonString = jsonString;
                    isBookDownloadComplete = true;
                } else if ("comic_data.json".equals(filename)) {
                    cachedComicJsonString = jsonString;
                    isComicDownloadComplete = true;
                }

                Log.d(TAG, filename + " download complete and cached.");

            } catch (Exception e) {
                Log.e(TAG, "Error downloading " + filename, e);

                if ("mp3_data.json".equals(filename)) {
                    isMp3DownloadComplete = true;
                } else if ("data.json".equals(filename)) {
                    isBookDownloadComplete = true;
                } else if ("comic_data.json".equals(filename)) {
                    isComicDownloadComplete = true;
                }

                runOnUiThread(() ->
                        Toast.makeText(HomeActivity.this, "Error downloading " + filename, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void handleMp3Navigation() {
        if (cachedMp3JsonString != null && !cachedMp3JsonString.isEmpty()) {
            Intent intent = new Intent(HomeActivity.this, RadioCategoryActivity.class);
            intent.putExtra("json", cachedMp3JsonString);
            startActivity(intent);
        } else {
            if (isMp3DownloadComplete) {
                Toast.makeText(this, "No MP3 data available.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Loading MP3 data... Please wait.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handleBookNavigation() {
        Intent intent = new Intent(HomeActivity.this, BookActivity.class);
        startActivity(intent); // no extras needed
    }

    private void handleComicNavigation() {
        if (cachedComicJsonString != null && !cachedComicJsonString.isEmpty()) {
            Intent intent = new Intent(HomeActivity.this, ComicCategoryActivity.class);
            intent.putExtra("json", cachedComicJsonString);
            startActivity(intent);
        } else {
            if (isComicDownloadComplete) {
                Toast.makeText(this, "No comic data available.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Loading comic data... Please wait.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveJsonToCache(String filename, String json) {
        try {
            File jsonFile = new File(getCacheDir(), filename);
            FileOutputStream fos = new FileOutputStream(jsonFile);
            fos.write(json.getBytes());
            fos.close();
            Log.d(TAG, "JSON cached locally to " + jsonFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error saving JSON to cache", e);
        }
    }

    private String loadJsonFromCache(String filename) {
        try {
            File jsonFile = new File(getCacheDir(), filename);
            if (!jsonFile.exists()) {
                Log.d(TAG, "Cache file " + filename + " does not exist");
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
            return builder.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error loading JSON from cache", e);
            return null;
        }
    }
}