package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datadisplay.adapters.PhotoCategoryAdapter; // reuse
import com.example.datadisplay.models.PhotoCategory;
import com.example.datadisplay.models.PhotoData;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.*;

public class ComicCategoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private final String jsonUrl = "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/comic_data.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_category); // reuse same layout

        recyclerView = findViewById(R.id.categoryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchCategories();
    }

    private void fetchCategories() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(jsonUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Snackbar.make(recyclerView,
                        "Failed to load comic categories",
                        Snackbar.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    runOnUiThread(() -> setupRecyclerWithJson(json));
                }
            }
        });
    }

    private void setupRecyclerWithJson(String json) {
        Gson gson = new Gson();
        PhotoData comicData = gson.fromJson(json, PhotoData.class);

        List<String> categoryNames = new ArrayList<>();
        for (PhotoCategory category : comicData.categories) {
            categoryNames.add(category.name);
        }

        PhotoCategoryAdapter adapter = new PhotoCategoryAdapter(categoryNames, categoryName -> {
            Intent intent = new Intent(ComicCategoryActivity.this, ComicFolderActivity.class);
            intent.putExtra("category", categoryName);
            intent.putExtra("json", json);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }
}