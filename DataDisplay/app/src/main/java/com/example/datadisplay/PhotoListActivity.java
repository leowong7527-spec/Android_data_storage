package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datadisplay.adapters.PhotoGridAdapter;
import com.example.datadisplay.models.PhotoCategory;
import com.example.datadisplay.models.PhotoData;
import com.example.datadisplay.models.PhotoFolder;
import com.google.gson.Gson;


import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class PhotoListActivity extends AppCompatActivity implements PhotoGridAdapter.OnItemClickListener {

    private List<String> imageUrls;
    private String categoryName;
    private String folderName;
    private String jsonPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);

        RecyclerView recyclerView = findViewById(R.id.photoRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        categoryName = getIntent().getStringExtra("category");
        folderName = getIntent().getStringExtra("folder");
        jsonPath = getIntent().getStringExtra("json_path");

        imageUrls = new ArrayList<>();

        if (jsonPath != null && categoryName != null && folderName != null) {
            try {
                String json = new String(Files.readAllBytes(new File(jsonPath).toPath()), StandardCharsets.UTF_8);
                PhotoData photoData = new Gson().fromJson(json, PhotoData.class);

                if (photoData != null && photoData.categories != null) {
                    for (PhotoCategory category : photoData.categories) {
                        if (category.name.equals(categoryName)) {
                            for (PhotoFolder folder : category.folders) {
                                if (folder.name.equals(folderName)) {
                                    imageUrls.addAll(folder.images);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Log.d("PhotoListActivity", "Total images: " + imageUrls.size());

        PhotoGridAdapter adapter = new PhotoGridAdapter(this, imageUrls, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent(PhotoListActivity.this, FullScreenImageActivity.class);
        intent.putExtra("image_url", imageUrls.get(position));
        startActivity(intent);
    }
}
