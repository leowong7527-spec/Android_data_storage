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

        // Load from cache first
        cachedMp3JsonString = loadJsonFromCache("mp3_data.json");
        cachedBookJsonString = loadJsonFromCache("data.json");
        cachedComicJsonString = loadJsonFromCache("comic_data.json");
        cachedPhotoJsonString = loadJsonFromCache("photo_data.json");

        // Download in background
// MP3 JSON
        File mp3File = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "mp3_data.json");
        if (!mp3File.exists()) {
            mp3JsonDownloadId = downloadWithDownloadManager(
                    "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/mp3_data.json",
                    "mp3_data.json"
            );
        } else {
            isMp3DownloadComplete = true;
        }

// Book JSON
        File bookFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "data.json");
        if (!bookFile.exists()) {
            dataJsonDownloadId = downloadWithDownloadManager(
                    "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/data.json",
                    "data.json"
            );
        } else {
            isBookDownloadComplete = true;
        }

// Comic JSON
        File comicFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "comic_data.json");
        if (!comicFile.exists()) {
            comicJsonDownloadId = downloadWithDownloadManager(
                    "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/comic_data.json",
                    "comic_data.json"
            );
        } else {
            isComicDownloadComplete = true;
        }

// Photo JSON
        File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "photo_data.json");
        if (!photoFile.exists()) {
            photoJsonDownloadId = downloadWithDownloadManager(
                    "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/photo_data.json",
                    "photo_data.json"
            );
        } else {
            isPhotoDownloadComplete = true;
        }

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

    private long downloadWithDownloadManager(String url, String filename) {
        Log.d(TAG, "Starting download: " + filename + " from " + url);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Downloading " + filename);
        request.setDescription("Please wait...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, filename);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        long id = manager.enqueue(request);

        Log.d(TAG, "Enqueued download for " + filename + " with ID=" + id);
        return id;
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
            Log.d(TAG, "Download complete broadcast received for ID=" + id);

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(id);
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor cursor = manager.query(query);

            if (cursor != null && cursor.moveToFirst()) {
                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);

                if (statusIndex != -1 && reasonIndex != -1) {
                    int status = cursor.getInt(statusIndex);
                    int reason = cursor.getInt(reasonIndex);
                    Log.d(TAG, "Download ID=" + id
                            + " status=" + getStatusMessage(status) + "(" + status + ")"
                            + " reason=" + getReasonMessage(reason) + "(" + reason + ")");
                }
                cursor.close();
            }

            if (id == dataJsonDownloadId) {
                isBookDownloadComplete = true;
                Log.d(TAG, "data.json download finished successfully");
                Toast.makeText(context, "data.json download finished", Toast.LENGTH_SHORT).show();
            } else if (id == mp3JsonDownloadId) {
                isMp3DownloadComplete = true;
                Log.d(TAG, "mp3_data.json download finished successfully");
                Toast.makeText(context, "mp3_data.json download finished", Toast.LENGTH_SHORT).show();
            } else if (id == comicJsonDownloadId) {
                isComicDownloadComplete = true;
                Log.d(TAG, "comic_data.json download finished successfully");
                Toast.makeText(context, "comic_data.json download finished", Toast.LENGTH_SHORT).show();
            } else if (id == photoJsonDownloadId) {
                isPhotoDownloadComplete = true;
                Log.d(TAG, "photo_data.json download finished successfully");
                Toast.makeText(context, "photo_data.json download finished", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "Unknown download ID=" + id);
            }
        }
    };



    private String getStatusMessage(int status) {
        switch (status) {
            case DownloadManager.STATUS_PENDING: return "PENDING";
            case DownloadManager.STATUS_RUNNING: return "RUNNING";
            case DownloadManager.STATUS_PAUSED:  return "PAUSED";
            case DownloadManager.STATUS_SUCCESSFUL: return "SUCCESSFUL";
            case DownloadManager.STATUS_FAILED:  return "FAILED";
            default: return "UNKNOWN";
        }
    }

    private String getReasonMessage(int reason) {
        switch (reason) {
            case DownloadManager.ERROR_CANNOT_RESUME: return "ERROR_CANNOT_RESUME";
            case DownloadManager.ERROR_DEVICE_NOT_FOUND: return "ERROR_DEVICE_NOT_FOUND";
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS: return "ERROR_FILE_ALREADY_EXISTS";
            case DownloadManager.ERROR_FILE_ERROR: return "ERROR_FILE_ERROR";
            case DownloadManager.ERROR_HTTP_DATA_ERROR: return "ERROR_HTTP_DATA_ERROR";
            case DownloadManager.ERROR_INSUFFICIENT_SPACE: return "ERROR_INSUFFICIENT_SPACE";
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS: return "ERROR_TOO_MANY_REDIRECTS";
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE: return "ERROR_UNHANDLED_HTTP_CODE";
            case DownloadManager.ERROR_UNKNOWN: return "ERROR_UNKNOWN";
            default: return "REASON_OTHER(" + reason + ")";
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
        File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "photo_data.json");
        if (!photoFile.exists()) {
            photoJsonDownloadId = downloadWithDownloadManager(
                    "https://github.com/leowong7527-spec/Android_data_storage/releases/download/v1.0.1/photo_data.json",
                    "photo_data.json"
            );
        } else {
            isPhotoDownloadComplete = true;
        }
    }


    private void handleMp3Navigation() {
        File cacheFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "mp3_data.json");
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

    private void handleComicNavigation() {
        File cacheFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "comic_data.json");
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

    private void handlePhotoNavigation() {
        File cacheFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "photo_data.json");
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
