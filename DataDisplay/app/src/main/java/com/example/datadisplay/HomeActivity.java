package com.example.datadisplay;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import android.util.JsonReader;

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

    // Download management
    private DownloadManager downloadManager;
    private BroadcastReceiver downloadCompleteReceiver;
    private final java.util.HashMap<String, Long> downloadQueue = new java.util.HashMap<>();
    private final java.util.HashSet<String> downloadingFiles = new java.util.HashSet<>();
    private Handler downloadProgressHandler = null;
    
    // Pending navigation after download
    private String pendingNavigationFile = null;
    private Class<?> pendingNavigationActivity = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        try {
            // Check and request storage permissions (non-blocking)
            Log.d(TAG, "🔐 Checking storage permissions...");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ uses READ_MEDIA_* permissions
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "📱 Requesting READ_MEDIA permissions...");
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.READ_MEDIA_AUDIO,
                                    Manifest.permission.READ_MEDIA_VIDEO,
                                    Manifest.permission.READ_MEDIA_IMAGES
                            }, 100);
                } else {
                    Log.d(TAG, "✅ All media permissions already granted");
                }
            } else {
                // Android 12 and below
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "📱 Requesting READ_EXTERNAL_STORAGE permission...");
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                } else {
                    Log.d(TAG, "✅ READ_EXTERNAL_STORAGE permission already granted");
                }
            }
            
            // Initialize download manager FIRST
            downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                Log.e(TAG, "ERROR: DownloadManager is NULL! Cannot initialize downloads.");
                Toast.makeText(this, "❌ 下載管理器初始化失敗", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "✓ DownloadManager initialized successfully");
            }
            
            // Register download complete receiver
            downloadCompleteReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "📥 Download complete broadcast received");
                    onDownloadComplete(intent);
                }
            };
            registerReceiver(downloadCompleteReceiver, 
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED);
            Log.d(TAG, "✓ BroadcastReceiver registered");

            // Download JSON files BEFORE initializing views
            Log.d(TAG, "🌐 Starting JSON file downloads...");
            ensureFile("mp3_data.json", 
                "https://raw.githubusercontent.com/leowong7527-spec/Data-Storage-/main/mp3_data.json");
            ensureFile("data.json", 
                "https://raw.githubusercontent.com/leowong7527-spec/Data-Storage-/main/data.json");
            ensureFile("comic_data.json", 
                "https://raw.githubusercontent.com/leowong7527-spec/Data-Storage-/main/comic_data.json");
            ensureFile("photo_data.json", 
                "https://raw.githubusercontent.com/leowong7527-spec/Data-Storage-/main/photo_data.json");

            Log.d(TAG, "📋 Initializing views...");
            // Initialize views
            initializeViews();
            setupNavigation();
            Log.d(TAG, "✓ Views initialized");
            
            // Load statistics after files are ensured
            loadStatistics();
            setupSearchFunctionality();
            setupQuickAccessCards();
            
            // Start periodic download progress checking
            startDownloadProgressCheck();
            
            // Refresh statistics every 2 seconds to catch file loads
            Handler refreshHandler = new Handler(Looper.getMainLooper());
            refreshHandler.postDelayed(() -> {
                Log.d(TAG, "🔄 Refreshing statistics...");
                loadStatistics();
            }, 2000);
            
            Log.d(TAG, "✓ HomeActivity onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "💥 FATAL ERROR in onCreate(): " + e.getMessage(), e);
            e.printStackTrace();
            Toast.makeText(this, "❌ 應用程式初始化失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 啟動定期檢查下載進度
     */
    private void startDownloadProgressCheck() {
        downloadProgressHandler = new Handler(Looper.getMainLooper());
        downloadProgressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkDownloadProgress();
                // Schedule next check after 3 seconds
                downloadProgressHandler.postDelayed(this, 3000);
            }
        }, 3000);
        Log.d(TAG, "📊 Download progress checking started");
    }
    
    /**
     * 檢查所有下載的進度和狀態
     */
    private void checkDownloadProgress() {
        if (downloadQueue.isEmpty()) {
            return;
        }
        
        try {
            DownloadManager.Query query = new DownloadManager.Query();
            Cursor cursor = downloadManager.query(query);
            
            if (cursor != null) {
                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
                int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                
                while (cursor.moveToNext()) {
                    long downloadId = cursor.getLong(idIndex);
                    int status = cursor.getInt(statusIndex);
                    
                    // Check if this is one of our downloads
                    for (java.util.Map.Entry<String, Long> entry : downloadQueue.entrySet()) {
                        if (entry.getValue() == downloadId) {
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                Log.d(TAG, "✅ Download completed: " + entry.getKey());
                                // Trigger onDownloadComplete manually
                                Intent intent = new Intent(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                                intent.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, downloadId);
                                onDownloadComplete(intent);
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                int reason = cursor.getInt(reasonIndex);
                                Log.e(TAG, "❌ Download failed for " + entry.getKey() + " (reason: " + reason + ")");
                                downloadingFiles.remove(entry.getKey());
                            }
                            break;
                        }
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking download progress: " + e.getMessage(), e);
        }
    }
    
    /**
     * 從 assets 複製文件到應用數據目錄
     */
    private void copyFileFromAssets(String filename, File destinationFile) throws Exception {
        // Create parent directory if it doesn't exist
        File parentDir = destinationFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // Check if file already exists and is valid
        if (destinationFile.exists() && destinationFile.length() > 0) {
            Log.d(TAG, "✅ File already exists: " + filename + " (" + (destinationFile.length() / 1024) + " KB)");
            return;
        }
        
        Log.d(TAG, "📋 Copying " + filename + " from assets...");
        
        try (java.io.InputStream inputStream = getAssets().open(filename);
             java.io.FileOutputStream outputStream = new java.io.FileOutputStream(destinationFile)) {
            
            byte[] buffer = new byte[8192]; // Larger buffer for better performance
            int length;
            long totalWritten = 0;
            
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
                totalWritten += length;
            }
            
            outputStream.flush();
            
            long copiedSize = destinationFile.length();
            Log.d(TAG, "✅ Successfully copied " + filename + " (" + (copiedSize / 1024) + " KB) to " + destinationFile.getAbsolutePath());
            
            if (copiedSize == 0) {
                Log.e(TAG, "⚠️ WARNING: " + filename + " copied but file is empty!");
                throw new IOException("Destination file is empty after copy");
            }
        } catch (Exception e) {
            // Clean up partial file on error
            if (destinationFile.exists()) {
                destinationFile.delete();
            }
            Log.e(TAG, "❌ Error copying " + filename + ": " + e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop download progress checking
        if (downloadProgressHandler != null) {
            downloadProgressHandler.removeCallbacksAndMessages(null);
            downloadProgressHandler = null;
        }
        if (downloadCompleteReceiver != null) {
            unregisterReceiver(downloadCompleteReceiver);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            boolean allGranted = true;
            StringBuilder grantLog = new StringBuilder();
            
            for (int i = 0; i < grantResults.length; i++) {
                boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                String status = granted ? "✅ GRANTED" : "❌ DENIED";
                grantLog.append(permissions[i]).append(" = ").append(status).append("\n");
                Log.d(TAG, "Permission " + permissions[i] + " = " + status);
                if (!granted) {
                    allGranted = false;
                }
            }
            
            Log.d(TAG, "Permission Summary:\n" + grantLog.toString());
            
            if (allGranted) {
                Log.d(TAG, "✅ All permissions granted!");
                Toast.makeText(this, "✅ 全部權限已授予，下載開始", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "⚠️ Some permissions were denied - app will use assets-based files and limited external storage");
                Toast.makeText(this, "⚠️ 部分權限被拒 - 將使用本機檔案", Toast.LENGTH_LONG).show();
                // 應用程式可以繼續使用 getExternalFilesDir()，這不需要特殊權限
                Log.d(TAG, "📁 Using app-specific external files directory: " + getExternalFilesDir("Downloads"));
            }
        }
    }
    
    /**
     * 確保 JSON 文件存在 - 優先從 assets 複製,然後嘗試下載
     */
    private void ensureFile(String filename, String url) {
        try {
            File destinationFile = new File(getExternalFilesDir("Downloads"), filename);
            
            // Check if file already exists and has content
            if (destinationFile.exists() && destinationFile.length() > 0) {
                Log.d(TAG, "✅ File already exists: " + filename + " (" + (destinationFile.length() / 1024) + " KB)");
                return;
            }
            
            // First, try to copy from assets
            try {
                Log.d(TAG, "🔄 Attempting to copy " + filename + " from assets...");
                copyFileFromAssets(filename, destinationFile);
                Log.d(TAG, "✅ File copied from assets: " + filename + " (" + (destinationFile.length() / 1024) + " KB)");
                return;
            } catch (Exception e) {
                Log.w(TAG, "⚠️ Could not copy from assets (" + filename + "): " + e.getMessage());
            }
            
            // If not in assets, try to download
            if (downloadManager == null) {
                Log.e(TAG, "❌ ensureFile(" + filename + "): DownloadManager is NULL - cannot download");
                return;
            }

            // Skip if already downloading
            if (downloadingFiles.contains(filename)) {
                Log.d(TAG, "⏳ Already downloading " + filename + ", skipping...");
                return;
            }
            
            // Download from GitHub
            Log.d(TAG, "📥 Downloading " + filename + " from " + url);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setDestinationInExternalFilesDir(this, "Downloads", filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setTitle("正在下載 " + filename);
            
            // 允許在任何网络下下载
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setAllowedOverRoaming(true);
            
            long downloadId = downloadManager.enqueue(request);
            Log.d(TAG, "✓ Download enqueued with ID: " + downloadId + " for " + filename);
            
            // Track download
            downloadQueue.put(filename, downloadId);
            downloadingFiles.add(filename);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in ensureFile(" + filename + "): " + e.getMessage(), e);
        }
    }
    
    /**
     * 下載完成處理
     */
    private void onDownloadComplete(Intent intent) {
        try {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId == -1) {
                Log.e(TAG, "❌ Invalid download ID received");
                return;
            }
            
            Log.d(TAG, "🔍 Querying download status for ID: " + downloadId);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            
            Cursor cursor = null;
            String completedFile = null;
            
            try {
                cursor = downloadManager.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = cursor.getInt(statusIndex);
                    
                    Log.d(TAG, "📊 Download status: " + status);
                    
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                        String localUri = cursor.getString(uriIndex);
                        Log.d(TAG, "✅ Download SUCCESSFUL: " + localUri);
                        
                        // Find which file completed
                        for (java.util.Map.Entry<String, Long> entry : downloadQueue.entrySet()) {
                            if (entry.getValue() == downloadId) {
                                completedFile = entry.getKey();
                                downloadingFiles.remove(completedFile);
                                Log.d(TAG, "✓ Matched to file: " + completedFile);
                                break;
                            }
                        }
                        
                        // Reload statistics after download
                        loadStatistics();
                        
                        // Auto-navigate if pending
                        if (completedFile != null && completedFile.equals(pendingNavigationFile) 
                            && pendingNavigationActivity != null) {
                            File file = new File(getExternalFilesDir("Downloads"), completedFile);
                            if (file.exists()) {
                                Log.d(TAG, "🚀 Auto-navigating to " + pendingNavigationActivity.getSimpleName());
                                Intent navIntent = new Intent(this, pendingNavigationActivity);
                                navIntent.putExtra("json_path", file.getAbsolutePath());
                                startActivity(navIntent);
                                
                                // Clear pending navigation
                                pendingNavigationFile = null;
                                pendingNavigationActivity = null;
                                
                                Toast.makeText(this, "數據已就緒,正在打開...", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        Log.e(TAG, "❌ Download FAILED with status code: " + status);
                        int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                        int reason = cursor.getInt(reasonIndex);
                        Log.e(TAG, "   Failure reason: " + reason);
                    } else {
                        Log.d(TAG, "⏳ Download still in progress, status: " + status);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in onDownloadComplete(): " + e.getMessage(), e);
        }
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
     * 加載統計信息從 JSON 文件
     */
    private void loadStatistics() {
        new Thread(() -> {
            try {
                Log.d(TAG, "📊 Loading statistics...");
                // Load MP3 stats
                totalMp3s = countItemsInJson("mp3_data.json", "categories");
                Log.d(TAG, "   📝 MP3s: " + totalMp3s);

                // Load Books stats
                totalBooks = countItemsInJson("data.json", "categories");
                Log.d(TAG, "   📝 Books: " + totalBooks);

                // Load Comics stats
                totalComics = countItemsInJson("comic_data.json", "categories");
                Log.d(TAG, "   📝 Comics: " + totalComics);

                // Load Photos stats
                totalPhotos = countItemsInJson("photo_data.json", "categories");
                Log.d(TAG, "   📝 Photos: " + totalPhotos);
                
                Log.d(TAG, "✅ Statistics loaded: MP3=" + totalMp3s + " Books=" + totalBooks + " Comics=" + totalComics + " Photos=" + totalPhotos);

                runOnUiThread(this::updateDashboard);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading statistics", e);
            }
        }).start();
    }

    /**
     * 統計 JSON 中所有檔案項目總數（支援多種格式）
     */
    private int countItemsInJson(String filename, String key) {
        int totalCount = 0;
        try {
            File jsonFile = new File(getExternalFilesDir("Downloads"), filename);

            if (!jsonFile.exists()) {
                Log.w(TAG, "⚠️ File not found for counting: " + filename);
                return 0;
            }
            
            long fileSize = jsonFile.length();
            if (fileSize == 0) {
                Log.w(TAG, "⚠️ File is empty: " + filename);
                return 0;
            }
            
            Log.d(TAG, "📋 Counting items in " + filename + " (size: " + (fileSize / 1024) + " KB)");

            try (JsonReader reader = new JsonReader(
                    new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8))) {
                
                if ("BEGIN_ARRAY".equals(reader.peek().toString())) {
                    // data.json 格式：根是 ARRAY，每個元素就是一項
                    Log.d(TAG, "  📊 JSON root is ARRAY format (data.json style)");
                    reader.beginArray();
                    while (reader.hasNext()) {
                        reader.beginObject();
                        totalCount++;  // 計算陣列中的每個對象
                        while (reader.hasNext()) {
                            reader.skipValue();
                        }
                        reader.endObject();
                    }
                    reader.endArray();
                } else {
                    // OBJECT 格式：包含 categories
                    Log.d(TAG, "  📊 JSON root is OBJECT format");
                    reader.beginObject();
                    
                    while (reader.hasNext()) {
                        String topKey = reader.nextName();
                        
                        if ("categories".equals(topKey)) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                reader.beginObject();
                                totalCount += countCategoryItems(reader);
                                reader.endObject();
                            }
                            reader.endArray();
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                }
            }
            
            Log.d(TAG, "✅ Counted " + totalCount + " items in " + filename);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error counting items in " + filename, e);
        }
        return totalCount;
    }

    /**
     * 計算單個分類中的項目數（支援 folders/files 和 images）
     */
    private int countCategoryItems(JsonReader reader) throws IOException {
        int count = 0;
        
        while (reader.hasNext()) {
            String key = reader.nextName();
            
            if ("folders".equals(key)) {
                // 有 folders 結構
                reader.beginArray();
                while (reader.hasNext()) {
                    reader.beginObject();
                    count += countFolderItems(reader);
                    reader.endObject();
                }
                reader.endArray();
            } else if ("images".equals(key)) {
                // 直接有 images（comic_data/photo_data 可能用此格式）
                reader.beginArray();
                while (reader.hasNext()) {
                    reader.skipValue();
                    count++;
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        
        return count;
    }

    /**
     * 計算單個資料夾中的項目數
     */
    private int countFolderItems(JsonReader reader) throws IOException {
        int count = 0;
        
        while (reader.hasNext()) {
            String key = reader.nextName();
            
            if ("files".equals(key)) {
                // MP3 和圖書用 files
                reader.beginArray();
                while (reader.hasNext()) {
                    reader.skipValue();
                    count++;
                }
                reader.endArray();
            } else if ("images".equals(key)) {
                // 漫畫和圖片可能用 images
                reader.beginArray();
                while (reader.hasNext()) {
                    reader.skipValue();
                    count++;
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        
        return count;
    }

    /**
     * 更新儀表板顯示
     */
    private void updateDashboard() {
        int total = totalMp3s + totalBooks + totalComics + totalPhotos;
        String stats = String.format(
                "📊 統計\n🎵 %d 首歌曲  📚 %d 本書籍  🎭 %d 部漫畫  📸 %d 張照片\n總計：%d 項",
                totalMp3s, totalBooks, totalComics, totalPhotos, total
        );
        statsText.setText(stats);
    }

    /**
     * 設置搜索功能（跨所有類別搜索）
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
     * 全局搜索 - 跨所有媒體類別搜索
     */
    private void performGlobalSearch(String query) {
        new Thread(() -> {
            List<SearchResult> results = new ArrayList<>();

            // 搜索 MP3
            results.addAll(searchInJson("mp3_data.json", query, "🎵"));

            // 搜索 Books
            results.addAll(searchInJson("data.json", query, "📚"));

            // 搜索 Comics
            results.addAll(searchInJson("comic_data.json", query, "🎭"));

            // 搜索 Photos
            results.addAll(searchInJson("photo_data.json", query, "📸"));

            runOnUiThread(() -> displaySearchResults(results));
        }).start();
    }

    /**
     * 在 JSON 文件中搜索（支援 ARRAY、folders/files、images 等多種格式）
     */
    private List<SearchResult> searchInJson(String filename, String query, String icon) {
        List<SearchResult> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        int fileCount = 0;
        
        try {
            File jsonFile = new File(getExternalFilesDir("Downloads"), filename);
            if (!jsonFile.exists()) {
                Log.w(TAG, "⚠️ Search: File not found: " + filename);
                return results;
            }

            try (JsonReader reader = new JsonReader(
                    new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8))) {
                
                if ("BEGIN_ARRAY".equals(reader.peek().toString())) {
                    // data.json：ARRAY 格式，每個元素就是一項
                    Log.d(TAG, "🔍 Search JSON root is ARRAY format in " + filename);
                    reader.beginArray();
                    
                    while (reader.hasNext()) {
                        reader.beginObject();
                        
                        while (reader.hasNext()) {
                            String key = reader.nextName();
                            
                            if ("name".equals(key) || "title".equals(key)) {
                                String name = reader.nextString();
                                if (name.toLowerCase().contains(lowerQuery)) {
                                    results.add(new SearchResult(icon + " " + name, filename, "item"));
                                    fileCount++;
                                }
                            } else {
                                reader.skipValue();
                            }
                        }
                        reader.endObject();
                    }
                    reader.endArray();
                } else {
                    // OBJECT 格式：包含 categories
                    Log.d(TAG, "🔍 Search JSON root is OBJECT format in " + filename);
                    reader.beginObject();
                    
                    while (reader.hasNext()) {
                        String key = reader.nextName();
                        
                        if ("categories".equals(key)) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                reader.beginObject();
                                String categoryName = "";
                                
                                while (reader.hasNext()) {
                                    String catKey = reader.nextName();
                                    
                                    if ("name".equals(catKey)) {
                                        categoryName = reader.nextString();
                                        if (categoryName.toLowerCase().contains(lowerQuery)) {
                                            results.add(new SearchResult(icon + " " + categoryName, filename, "category"));
                                        }
                                    } else if ("folders".equals(catKey)) {
                                        fileCount += searchFolders(reader, lowerQuery, icon, categoryName, results);
                                    } else if ("images".equals(catKey)) {
                                        // 分類下直接有 images（photo_data 可能用此格式）
                                        fileCount += searchImageArray(reader, lowerQuery, icon, categoryName, results);
                                    } else {
                                        reader.skipValue();
                                    }
                                }
                                reader.endObject();
                            }
                            reader.endArray();
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                }
            }
            
            Log.d(TAG, "🔍 Search in " + filename + " for \"" + query + "\": found " + results.size() + " results (" + fileCount + " files)");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error searching " + filename, e);
        }
        return results;
    }

    /**
     * 搜索資料夾結構（支援 files 和 images）
     */
    private int searchFolders(JsonReader reader, String lowerQuery, String icon, String categoryName, 
                             List<SearchResult> results) throws IOException {
        int count = 0;
        reader.beginArray();
        
        while (reader.hasNext()) {
            reader.beginObject();
            String folderName = "";
            
            while (reader.hasNext()) {
                String folderKey = reader.nextName();
                
                if ("name".equals(folderKey)) {
                    folderName = reader.nextString();
                    if (folderName.toLowerCase().contains(lowerQuery)) {
                        results.add(new SearchResult(
                                icon + " " + folderName + " (" + categoryName + ")",
                                "", "folder"
                        ));
                    }
                } else if ("files".equals(folderKey)) {
                    count += searchFileArray(reader, lowerQuery, icon, folderName, results);
                } else if ("images".equals(folderKey)) {
                    count += searchImageArray(reader, lowerQuery, icon, folderName, results);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        }
        reader.endArray();
        
        return count;
    }

    /**
     * 搜索 files 陣列
     */
    private int searchFileArray(JsonReader reader, String lowerQuery, String icon, String contextName,
                               List<SearchResult> results) throws IOException {
        int count = 0;
        reader.beginArray();
        
        while (reader.hasNext()) {
            reader.beginObject();
            
            while (reader.hasNext()) {
                String fileKey = reader.nextName();
                
                if ("title".equals(fileKey)) {
                    String title = reader.nextString();
                    if (title.toLowerCase().contains(lowerQuery)) {
                        results.add(new SearchResult(
                                icon + " " + title + " (" + contextName + ")",
                                "", "file"
                        ));
                        count++;
                    }
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        }
        reader.endArray();
        
        return count;
    }

    /**
     * 搜索 images 陣列（圖片/漫畫的數據格式）
     */
    private int searchImageArray(JsonReader reader, String lowerQuery, String icon, String contextName,
                                List<SearchResult> results) throws IOException {
        int count = 0;
        reader.beginArray();
        
        while (reader.hasNext()) {
            // images 通常是簡單字符串或 URL，直接跳過並計算
            reader.skipValue();
            count++;
        }
        reader.endArray();
        
        return count;
    }

    /**
     * 顯示搜索結果
     */
    private void displaySearchResults(List<SearchResult> results) {
        searchResultsContainer.removeAllViews();
        dashboardContainer.setVisibility(View.GONE);

        Log.d(TAG, "📊 Displaying " + results.size() + " search results");

        if (results.isEmpty()) {
            TextView noResults = new TextView(this);
            noResults.setText("❌ 未找到結果");
            noResults.setPadding(16, 16, 16, 16);
            noResults.setTextColor(getResources().getColor(android.R.color.darker_gray));
            searchResultsContainer.addView(noResults);
            return;
        }

        // Add result count header
        TextView resultCountHeader = new TextView(this);
        resultCountHeader.setText("🔍 找到 " + results.size() + " 個結果");
        resultCountHeader.setPadding(16, 16, 16, 8);
        resultCountHeader.setTextColor(getResources().getColor(android.R.color.black));
        resultCountHeader.setTextSize(14);
        searchResultsContainer.addView(resultCountHeader);

        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            MaterialCardView card = new MaterialCardView(this);
            card.setCardElevation(4);
            card.setCardBackgroundColor(getResources().getColor(android.R.color.white));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 8, 8, 8);
            card.setLayoutParams(params);

            LinearLayout cardContent = new LinearLayout(this);
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setPadding(16, 12, 16, 12);

            TextView textView = new TextView(this);
            textView.setText(result.title);
            textView.setTextSize(14);
            cardContent.addView(textView);

            TextView typeView = new TextView(this);
            typeView.setText("類型: " + result.type);
            typeView.setTextSize(12);
            typeView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            typeView.setPadding(0, 8, 0, 0);
            cardContent.addView(typeView);

            card.addView(cardContent);
            searchResultsContainer.addView(card);
        }
    }

    /**
     * 設置快速訪問卡片
     */
    private void setupQuickAccessCards() {
        addQuickAccessCard("🎵 MP3 / 音樂", "mp3_data.json", RadioCategoryActivity.class);
        addQuickAccessCard("📚 書籍", "data.json", BookActivity.class);
        addQuickAccessCard("🎭 漫畫", "comic_data.json", ComicCategoryActivity.class);
        addQuickAccessCard("📸 照片", "photo_data.json", PhotoCategoryActivity.class);
    }

    /**
     * 添加快速訪問卡片
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
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        textView.setGravity(android.view.Gravity.CENTER);

        card.addView(textView);
        card.setOnClickListener(v -> navigateToCategory(filename, targetActivity, title));

        quickAccessGrid.addView(card);
    }

    /**
     * 導航到特定類別
     */
    private void navigateToCategory(String filename, Class<?> targetActivity, String label) {
        File cacheFile = new File(getExternalFilesDir("Downloads"), filename);
        if (cacheFile.exists() && !downloadingFiles.contains(filename)) {
            // File exists and not currently downloading, navigate immediately
            Intent intent = new Intent(this, targetActivity);
            intent.putExtra("json_path", cacheFile.getAbsolutePath());
            startActivity(intent);
        } else if (downloadingFiles.contains(filename)) {
            // Already downloading, set up auto-navigation
            pendingNavigationFile = filename;
            pendingNavigationActivity = targetActivity;
            Toast.makeText(this, "正在下載 " + label + " 數據,下載完成後將自動打開...", Toast.LENGTH_LONG).show();
        } else {
            // File doesn't exist and not downloading, start download
            String url = getDownloadUrl(filename);
            if (url != null) {
                pendingNavigationFile = filename;
                pendingNavigationActivity = targetActivity;
                ensureFile(filename, url);
                Toast.makeText(this, "正在下載 " + label + " 數據,下載完成後將自動打開...", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * 獲取文件的下載 URL
     */
    private String getDownloadUrl(String filename) {
        String baseUrl = "https://raw.githubusercontent.com/leowong7527-spec/Data-Storage-/main/";
        switch (filename) {
            case "mp3_data.json":
            case "data.json":
            case "comic_data.json":
            case "photo_data.json":
                return baseUrl + filename;
            default:
                return null;
        }
    }

    /**
     * 搜索結果數據類
     */
    public static class SearchResult {
        public String title;
        public String filename;
        public String type; // "category", "folder", "file"

        public SearchResult(String title, String filename, String type) {
            this.title = title;
            this.filename = filename;
            this.type = type;
        }
    }
}
