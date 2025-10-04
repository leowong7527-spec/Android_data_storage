// File: RadioCategoryActivity.java
package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datadisplay.adapters.RadioCategoryAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File; // Added for File
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RadioCategoryActivity extends AppCompatActivity implements RadioCategoryAdapter.OnCategoryClickListener {

    private static final String TAG = "RadioCategoryActivity"; // Added TAG for logging
    private RecyclerView recyclerView;
    private JSONObject jsonData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio_category);

        recyclerView = findViewById(R.id.radioCategoryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // First, try to get JSON from the intent (passed from HomeActivity)
        String jsonFromIntent = getIntent().getStringExtra("json");
        String json = null;

        if (jsonFromIntent != null) {
            json = jsonFromIntent;
            Log.d(TAG, "Loaded JSON from intent.");
        } else {
            // If not in intent, try to load from cache
            json = loadMp3JsonFromCache();
            if (json != null) {
                Log.d(TAG, "Loaded JSON from cache.");
            }
        }


        if (json != null) {
            try {
                jsonData = new JSONObject(json);
                setupRecycler(jsonData);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing mp3_data.json", e); // Use Log.e
                Toast.makeText(this, "Error parsing mp3_data.json", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No mp3 data found.", Toast.LENGTH_LONG).show();
            finish(); // Close activity if no data is available
        }
    }

    private String loadMp3JsonFromCache() {
        try {
            // 🔹 FIX: Use getCacheDir() to match where HomeActivity saves the file
            File jsonFile = new File(getCacheDir(), "mp3_data.json");
            if (!jsonFile.exists()) {
                Log.d(TAG, "Cache file does not exist for mp3_data.json in " + getCacheDir());
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
            Log.e(TAG, "Error loading mp3_data.json from cache", e); // Use Log.e
            return null;
        }
    }

    private void setupRecycler(JSONObject jsonData) {
        try {
            JSONArray categories = jsonData.getJSONArray("categories");
            List<String> categoryNames = new ArrayList<>();
            for (int i = 0; i < categories.length(); i++) {
                categoryNames.add(categories.getJSONObject(i).getString("name"));
            }

            RadioCategoryAdapter adapter = new RadioCategoryAdapter(categoryNames, this);
            recyclerView.setAdapter(adapter);
        } catch (JSONException e) {
            Log.e(TAG, "Error loading categories", e); // Use Log.e
            Toast.makeText(this, "Error loading categories", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCategoryClick(String categoryName) {
        Intent intent = new Intent(this, RadioFolderActivity.class);
        intent.putExtra("category", categoryName);
        intent.putExtra("json", jsonData.toString());
        startActivity(intent);
    }
}