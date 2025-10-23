package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datadisplay.adapters.PhotoFolderAdapter;
import com.example.datadisplay.models.PhotoCategory;
import com.example.datadisplay.models.PhotoData;
import com.example.datadisplay.models.PhotoFolder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class PhotoFolderActivity extends AppCompatActivity implements PhotoFolderAdapter.OnFolderClickListener {

    private List<PhotoFolder> folderList;
    private String categoryName;
    private String jsonPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_folder);

        RecyclerView recyclerView = findViewById(R.id.folderRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        categoryName = getIntent().getStringExtra("category");
        jsonPath = getIntent().getStringExtra("json_path");
        String folderJson = getIntent().getStringExtra("folder_json");

        folderList = new ArrayList<>();

        try {
            if (folderJson != null) {
                // ✅ Coming from a subfolder → just deserialize and use its children
                PhotoFolder currentFolder = new Gson().fromJson(folderJson, PhotoFolder.class);
                if (currentFolder != null && currentFolder.folders != null) {
                    folderList = currentFolder.folders;
                }
            } else if (jsonPath != null && categoryName != null) {
                // ✅ First entry point → load from root JSON
                String json = new String(Files.readAllBytes(new File(jsonPath).toPath()), StandardCharsets.UTF_8);
                PhotoData photoData = new Gson().fromJson(json, PhotoData.class);

                if (photoData != null && photoData.categories != null) {
                    for (PhotoCategory category : photoData.categories) {
                        if (category.name.equals(categoryName)) {
                            folderList = category.folders != null ? category.folders : new ArrayList<>();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(recyclerView, "Failed to load folders", Snackbar.LENGTH_LONG).show();
        }

        PhotoFolderAdapter adapter = new PhotoFolderAdapter(folderList, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onFolderClick(PhotoFolder clickedFolder) {
        Gson gson = new Gson();
        String folderJson = gson.toJson(clickedFolder);

        if (clickedFolder.folders != null && !clickedFolder.folders.isEmpty()) {
            // ✅ Has subfolders → open PhotoFolderActivity again
            Intent intent = new Intent(this, PhotoFolderActivity.class);
            intent.putExtra("category", categoryName);
            intent.putExtra("folder_json", folderJson);
            intent.putExtra("json_path", jsonPath);
            startActivity(intent);
        } else {
            // ✅ No subfolders → open PhotoListActivity
            Intent intent = new Intent(this, PhotoListActivity.class);
            intent.putExtra("category", categoryName);
            intent.putExtra("folder_json", folderJson);
            intent.putExtra("json_path", jsonPath);
            startActivity(intent);
        }
    }
}