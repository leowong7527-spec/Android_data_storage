// File: HomeActivity.java
package com.example.datadisplay;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
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
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;

    private boolean isMp3DownloadComplete = false;
    private boolean isBookDownloadComplete = false;
    private boolean isComicDownloadComplete = false;
    private boolean isPhotoDownloadComplete = false;

    private long dataJsonDownloadId;
    private long mp3JsonDownloadId;
    private long comicJsonDownloadId;
    private long photoJsonDownloadId;

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

        // Trigger downloads if missing
        ensureFile("mp3_data.json",
                "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/mp3_data.json");
        ensureFile("data.json",
                "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/data.json");
        ensureFile("comic_data.json",
                "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/comic_data.json");
        ensureFile("photo_data.json",
                "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/photo_data.json");

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

    private void ensureFile(String filename, String url) {
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename);
        if (!file.exists()) {
            long id = downloadWithDownloadManager(url, filename);
            if (filename.contains("mp3")) mp3JsonDownloadId = id;
            else if (filename.equals("data.json")) dataJsonDownloadId = id;
            else if (filename.contains("comic")) comicJsonDownloadId = id;
            else if (filename.contains("photo")) photoJsonDownloadId = id;
        } else {
            if (filename.contains("mp3")) isMp3DownloadComplete = true;
            else if (filename.equals("data.json")) isBookDownloadComplete = true;
            else if (filename.contains("comic")) isComicDownloadComplete = true;
            else if (filename.contains("photo")) isPhotoDownloadComplete = true;
        }
    }

    private long downloadWithDownloadManager(String url, String filename) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Downloading " + filename);
        request.setDescription("Please wait...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, filename);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        return manager.enqueue(request);
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
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(id));

            if (cursor != null && cursor.moveToFirst()) {
                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                if (statusIndex != -1 && reasonIndex != -1) {
                    int status = cursor.getInt(statusIndex);
                    int reason = cursor.getInt(reasonIndex);
                    Log.d(TAG, "Download ID=" + id + " status=" + status + " reason=" + reason);
                }
                cursor.close();
            }

            if (id == dataJsonDownloadId) isBookDownloadComplete = true;
            else if (id == mp3JsonDownloadId) isMp3DownloadComplete = true;
            else if (id == comicJsonDownloadId) isComicDownloadComplete = true;
            else if (id == photoJsonDownloadId) isPhotoDownloadComplete = true;
        }
    };

    // --- Navigation handlers ---
    private void handleBookNavigation() {
        openIfExists("data.json", BookActivity.class, isBookDownloadComplete, "book");
    }

    private void handleMp3Navigation() {
        openIfExists("mp3_data.json", RadioCategoryActivity.class, isMp3DownloadComplete, "MP3");
    }

    private void handleComicNavigation() {
        openIfExists("comic_data.json", ComicCategoryActivity.class, isComicDownloadComplete, "comic");
    }

    private void handlePhotoNavigation() {
        openIfExists("photo_data.json", PhotoCategoryActivity.class, isPhotoDownloadComplete, "photo");
    }

    private void openIfExists(String filename, Class<?> target, boolean isComplete, String label) {
        File cacheFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename);
        if (cacheFile.exists()) {
            Intent intent = new Intent(HomeActivity.this, target);
            intent.putExtra("json_path", cacheFile.getAbsolutePath());
            startActivity(intent);
        } else {
            if (isComplete) {
                Toast.makeText(this, "No " + label + " data available.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Loading " + label + " data... Please wait.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- Stream JSON safely (no OOM) ---
    private <T> T loadJsonFromCache(String filename, Class<T> clazz) {
        try {
            File jsonFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename);
            if (!jsonFile.exists()) return null;

            try (FileInputStream fis = new FileInputStream(jsonFile);
                 InputStreamReader isr = new InputStreamReader(fis);
                 BufferedReader reader = new BufferedReader(isr)) {
                return new Gson().fromJson(reader, clazz);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading JSON from cache", e);
            return null;
        }
    }
}