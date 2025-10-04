package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datadisplay.adapters.RadioFolderAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RadioFolderActivity extends AppCompatActivity implements RadioFolderAdapter.OnFolderClickListener {

    private List<String> folderNames;
    private String categoryName;
    private String json;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio_folder);

        RecyclerView recyclerView = findViewById(R.id.radioFolderRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        categoryName = getIntent().getStringExtra("category");
        json = getIntent().getStringExtra("json");

        if (json == null) {
            // Try to load from cache
            // Show error message if still null
            Toast.makeText(this, "No data available", Toast.LENGTH_SHORT).show();
            return;
        }




        folderNames = new ArrayList<>();
        try {
            JSONObject jsonData = new JSONObject(json);
            JSONArray categories = jsonData.getJSONArray("categories");

            for (int i = 0; i < categories.length(); i++) {
                JSONObject cat = categories.getJSONObject(i);
                if (cat.getString("name").equals(categoryName)) {
                    JSONArray folders = cat.getJSONArray("folders");
                    for (int j = 0; j < folders.length(); j++) {
                        folderNames.add(folders.getJSONObject(j).getString("name"));
                    }
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading folders", Toast.LENGTH_SHORT).show();
        }

        RadioFolderAdapter adapter = new RadioFolderAdapter(folderNames, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onFolderClick(String folderName) {
        Intent intent = new Intent(this, RadioListActivity.class);
        intent.putExtra("category", categoryName);
        intent.putExtra("folder", folderName);
        intent.putExtra("json", json);
        startActivity(intent);
    }
}