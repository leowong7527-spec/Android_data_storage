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

    private static final String TAG = "PhotoListActivity";

    private List<String> imageUrls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);

        RecyclerView recyclerView = findViewById(R.id.photoRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        String folderName = getIntent().getStringExtra("folder_name");
        String jsonPath   = getIntent().getStringExtra("json_path");

        Log.d(TAG, "onCreate: folderName=" + folderName + ", jsonPath=" + jsonPath);

        imageUrls = new ArrayList<>();

        try {
            if (jsonPath != null && folderName != null) {
                String json = new String(Files.readAllBytes(new File(jsonPath).toPath()), StandardCharsets.UTF_8);
                PhotoData photoData = new Gson().fromJson(json, PhotoData.class);

                if (photoData != null && photoData.categories != null) {
                    for (PhotoCategory category : photoData.categories) {
                        PhotoFolder folder = findFolderByName(category.folders, folderName);
                        if (folder != null) {
                            if (folder.images != null) {
                                imageUrls.addAll(folder.images);
                                Log.d(TAG, "Loaded folder: " + folder.name + " with " + folder.images.size() + " images");
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading images from JSON", e);
        }

        Log.d(TAG, "Total images: " + imageUrls.size());

        PhotoGridAdapter adapter = new PhotoGridAdapter(this, imageUrls, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent(PhotoListActivity.this, ImagePagerActivity.class);
        intent.putStringArrayListExtra("image_urls", new ArrayList<>(imageUrls));
        intent.putExtra("start_position", position);
        startActivity(intent);
    }

    private PhotoFolder findFolderByName(List<PhotoFolder> folders, String targetName) {
        if (folders == null) return null;
        for (PhotoFolder f : folders) {
            if (targetName.equals(f.name)) return f;
            PhotoFolder found = findFolderByName(f.folders, targetName);
            if (found != null) return found;
        }
        return null;
    }
}