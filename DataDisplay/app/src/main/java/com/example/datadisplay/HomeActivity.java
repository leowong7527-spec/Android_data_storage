// File: HomeActivity.java
package com.example.datadisplay;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import java.io.InputStream;
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
    private String cachedPhotoJsonString;

    private boolean isMp3DownloadComplete = false;
    private boolean isBookDownloadComplete = false;
    private boolean isComicDownloadComplete = false;
    private boolean isPhotoDownloadComplete = false;

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

        // Book JSON from GitHub Release asset
        File bookFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "data.json");
        if (!bookFile.exists()) {
            downloadWithDownloadManager(
                    "https://github.com/leowong7527-spec/Android_data_storage/releases/download/v1.0.0/data.json",
                    "data.json"
            );
        } else {
            Log.d(TAG, "data.json already exists, skipping download");
            isBookDownloadComplete = true;
        }



        downloadJsonInBackground(
                "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/comic_data.json",
                "comic_data.json",
                false
        );

        // Preload photos
        preloadPhotoData();

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
                handlePhotoNavigation();
            } else if (id == R.id.nav_mp3) {
                handleMp3Navigation();
            } else if (id == R.id.nav_comics) {
                handleComicNavigation();
            }
            return true;
        });
    }

    // Helper: follow redirects until HTTP 200 OK
    private HttpURLConnection openConnectionFollowRedirects(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);

        int status = conn.getResponseCode();
        int redirectCount = 0;

        while (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER) {

            if (redirectCount++ > 5) {
                throw new Exception("Too many redirects");
            }
            String newUrl = conn.getHeaderField("Location");
            Log.d(TAG, "Redirecting to: " + newUrl);
            url = new URL(newUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            status = conn.getResponseCode();
        }

        if (status != HttpURLConnection.HTTP_OK) {
            throw new Exception("Failed to fetch, status=" + status);
        }

        return conn;
    }



    private void downloadJsonInBackground(String urlString, String filename, boolean isMp3) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = openConnectionFollowRedirects(urlString);

                if ("data.json".equals(filename)) {
                    // Stream large file directly to disk
                    InputStream in = conn.getInputStream();
                    File outFile = new File(getCacheDir(), filename);
                    FileOutputStream out = new FileOutputStream(outFile);

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long total = 0;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        total += bytesRead;
                        if (total % (5 * 1024 * 1024) < 8192) {
                            Log.d(TAG, filename + " downloaded " + (total / (1024 * 1024)) + " MB...");
                        }
                    }
                    out.close();
                    in.close();

                    Log.d(TAG, filename + " saved directly to " + outFile.getAbsolutePath());
                    isBookDownloadComplete = true;

                } else {
                    // For smaller JSONs, read into memory
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    reader.close();

                    String jsonString = builder.toString();

                    // Debug log: preview first 200 characters
                    Log.d(TAG, "Preview of " + filename + " response: " +
                            jsonString.substring(0, Math.min(200, jsonString.length())));

                    // Safety check: avoid saving HTML error pages
                    if (jsonString.startsWith("<!DOCTYPE") || jsonString.startsWith("<html")) {
                        throw new Exception("Received HTML instead of JSON for " + filename);
                    }

                    saveJsonToCache(filename, jsonString);

                    if ("mp3_data.json".equals(filename)) {
                        cachedMp3JsonString = jsonString;
                        isMp3DownloadComplete = true;
                    } else if ("comic_data.json".equals(filename)) {
                        cachedComicJsonString = jsonString;
                        isComicDownloadComplete = true;
                    } else if ("photo_data.json".equals(filename)) {
                        cachedPhotoJsonString = jsonString;
                        isPhotoDownloadComplete = true;
                    }
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
                } else if ("photo_data.json".equals(filename)) {
                    isPhotoDownloadComplete = true;
                }

                runOnUiThread(() ->
                        Toast.makeText(HomeActivity.this, "Error downloading " + filename, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private long dataJsonDownloadId;

    private void downloadWithDownloadManager(String url, String filename) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Downloading " + filename);
        request.setDescription("Please wait...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, filename);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        dataJsonDownloadId = manager.enqueue(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(onDownloadComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED);
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(onDownloadComplete);
    }


    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == dataJsonDownloadId) {
                isBookDownloadComplete = true;
                Toast.makeText(context, "data.json download finished", Toast.LENGTH_SHORT).show();
            }
        }
    };


    private void handleMp3Navigation() {
        File cacheFile = new File(getCacheDir(), "mp3_data.json");
        if (cacheFile.exists()) {
            Intent intent = new Intent(HomeActivity.this, RadioCategoryActivity.class);
            intent.putExtra("json_path", cacheFile.getAbsolutePath());
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
        // Since data.json is downloaded with DownloadManager, it’s saved in external files dir
        File cacheFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "data.json");
        if (cacheFile.exists()) {
            Intent intent = new Intent(HomeActivity.this, BookActivity.class);
            intent.putExtra("json_path", cacheFile.getAbsolutePath());
            startActivity(intent);
        } else {
            if (isBookDownloadComplete) {
                Toast.makeText(this, "No book data available.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Loading book data... Please wait.", Toast.LENGTH_LONG).show();
            }
        }
    }



    private void handleComicNavigation() {
        File cacheFile = new File(getCacheDir(), "comic_data.json");
        if (cacheFile.exists()) {
            Intent intent = new Intent(HomeActivity.this, ComicCategoryActivity.class);
            intent.putExtra("json_path", cacheFile.getAbsolutePath());
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

    private void preloadPhotoData() {
        downloadJsonInBackground(
                "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/photo_data.json",
                "photo_data.json",
                false
        );
    }

    private void handlePhotoNavigation() {
        File cacheFile = new File(getCacheDir(), "photo_data.json");
        if (cacheFile.exists()) {
            Intent intent = new Intent(HomeActivity.this, PhotoCategoryActivity.class);
            intent.putExtra("json_path", cacheFile.getAbsolutePath());
            startActivity(intent);
        } else {
            if (isPhotoDownloadComplete) {
                Toast.makeText(this, "No photo data available.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Loading photo data... Please wait.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
