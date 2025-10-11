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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class RadioListActivity extends AppCompatActivity {

    private static final String TAG = "RadioListActivity";

    private List<String> titles;
    private List<String> urls;
    private MediaPlayer mediaPlayer;
    private String categoryName;
    private String folderName;
    private String jsonPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio_list);

        ListView listView = findViewById(R.id.radioFileListView);

        categoryName = getIntent().getStringExtra("category");
        folderName = getIntent().getStringExtra("folder");
        jsonPath = getIntent().getStringExtra("json_path");

        titles = new ArrayList<>();
        urls = new ArrayList<>();

        if (jsonPath != null) {
            try {
                String json = new String(Files.readAllBytes(new File(jsonPath).toPath()), StandardCharsets.UTF_8);
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
        }

        RadioFileAdapter adapter = new RadioFileAdapter(this, titles);
        listView.setAdapter(adapter);

        // ✅ Completed onItemClick block
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
}