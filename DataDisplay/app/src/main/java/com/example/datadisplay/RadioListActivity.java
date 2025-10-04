package com.example.datadisplay;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.datadisplay.adapters.RadioFileAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RadioListActivity extends AppCompatActivity {

    private static final String TAG = "RadioListActivity";

    private List<String> titles;
    private List<String> urls;
    private MediaPlayer mediaPlayer;
    private boolean isPreparing = false;
    private boolean isPlaying = false;
    private String categoryName;
    private String folderName;
    private String json;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio_list);

        ListView listView = findViewById(R.id.radioFileListView);

        categoryName = getIntent().getStringExtra("category");
        folderName = getIntent().getStringExtra("folder");
        json = getIntent().getStringExtra("json");

        // 🔹 Add this block immediately after retrieving the intent extras
        if (json == null) {
            JSONObject cached = loadJsonFromCache();
            if (cached != null) {
                json = cached.toString();
                Log.d(TAG, "Loaded JSON from cache");
            } else {
                Toast.makeText(this, "No cached JSON found", Toast.LENGTH_SHORT).show();
            }
        }



// Save it to cache so it’s available next time
        if (json != null) {
            saveJsonToCache(json);
        }

        titles = new ArrayList<>();
        urls = new ArrayList<>();

        try {
            JSONObject jsonData = new JSONObject(json);
            JSONArray categories = jsonData.getJSONArray("categories");

            for (int i = 0; i < categories.length(); i++) {
                JSONObject cat = categories.getJSONObject(i);
                if (cat.getString("name").equals(categoryName)) {
                    JSONArray folders = cat.getJSONArray("folders");
                    for (int j = 0; j < folders.length(); j++) {
                        JSONObject folder = folders.getJSONObject(j);
                        if (folder.getString("name").equals(folderName)) {
                            JSONArray files = folder.getJSONArray("files");
                            for (int k = 0; k < files.length(); k++) {
                                JSONObject fileObj = files.getJSONObject(k);
                                String title = fileObj.getString("title");
                                String path = fileObj.getString("path");
                                titles.add(title);
                                urls.add(path);
                                Log.d(TAG, "Loaded: " + title + " → " + path);
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading files", e);
            Toast.makeText(this, "Error loading files", Toast.LENGTH_SHORT).show();
        }

        RadioFileAdapter adapter = new RadioFileAdapter(this, titles);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, RadioDetailActivity.class);
            intent.putExtra("title", titles.get(position));
            intent.putExtra("url", urls.get(position));
            intent.putStringArrayListExtra("allUrls", new ArrayList<>(urls));
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            Log.d(TAG, "MediaPlayer released");
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
            Log.e(TAG, "No cached JSON found", e);
            return null;
        }
    }
}