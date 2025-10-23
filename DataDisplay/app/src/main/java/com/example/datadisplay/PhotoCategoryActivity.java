package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datadisplay.adapters.PhotoCategoryAdapter;
import com.example.datadisplay.models.PhotoCategory;
import com.example.datadisplay.models.PhotoData;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class PhotoCategoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private File cacheFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_category);

        recyclerView = findViewById(R.id.categoryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        String jsonPath = getIntent().getStringExtra("json_path");
        if (jsonPath != null) {
            cacheFile = new File(jsonPath);
            loadCategoriesFromCache();
        } else {
            Snackbar.make(recyclerView, "No photo data available", Snackbar.LENGTH_LONG).show();
        }
    }

    private void loadCategoriesFromCache() {
        try {
            String json = new String(Files.readAllBytes(cacheFile.toPath()), StandardCharsets.UTF_8);
            setupRecyclerWithJson(json);
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(recyclerView, "Failed to load cached photo data", Snackbar.LENGTH_LONG).show();
        }
    }

    private void setupRecyclerWithJson(String json) {
        Gson gson = new Gson();
        PhotoData photoData = gson.fromJson(json, PhotoData.class);

        List<PhotoCategory> categories = new ArrayList<>();
        if (photoData != null && photoData.categories != null) {
            categories.addAll(photoData.categories);
        }

        PhotoCategoryAdapter adapter = new PhotoCategoryAdapter(categories, category -> {
            // ✅ Pass full object as JSON
            Intent intent = new Intent(PhotoCategoryActivity.this, PhotoFolderActivity.class);
            intent.putExtra("category_json", new Gson().toJson(category));
            intent.putExtra("json_path", cacheFile.getAbsolutePath());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }
}