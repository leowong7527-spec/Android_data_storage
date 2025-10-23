package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

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

    private static final String TAG = "PhotoFolderActivity";

    private List<PhotoFolder> folderList;
    private String categoryName;
    private String jsonPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_folder);

        RecyclerView recyclerView = findViewById(R.id.folderRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // ✅ Use category_name consistently
        categoryName = getIntent().getStringExtra("category_name");
        jsonPath = getIntent().getStringExtra("json_path");
        String folderName = getIntent().getStringExtra("folder_name");

        folderList = new ArrayList<>();

        try {
            if (jsonPath != null) {
                String json = new String(Files.readAllBytes(new File(jsonPath).toPath()), StandardCharsets.UTF_8);
                PhotoData photoData = new Gson().fromJson(json, PhotoData.class);

                if (photoData != null && photoData.categories != null) {
                    for (PhotoCategory category : photoData.categories) {
                        if (category.name.equals(categoryName) && category.folders != null) {
                            if (folderName == null) {
                                // First entry point → load top-level folders
                                folderList = category.folders != null ? category.folders : new ArrayList<>();
                            } else {
                                // Use recursive search for nested folders
                                PhotoFolder current = findFolderByName(category.folders, folderName);
                                folderList = (current != null && current.folders != null) ? current.folders : new ArrayList<>();
                                if (current != null) {
                                    Log.d(TAG, "Loaded subfolder: " + current.name + " with " + folderList.size() + " children");
                                }
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading folders", e);
            Snackbar.make(recyclerView, "Failed to load folders", Snackbar.LENGTH_LONG).show();
        }

        PhotoFolderAdapter adapter = new PhotoFolderAdapter(folderList, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onFolderClick(String folderName) {
        Log.d(TAG, "Clicked folder: " + folderName);

        try {
            if (jsonPath != null) {
                String json = new String(Files.readAllBytes(new File(jsonPath).toPath()), StandardCharsets.UTF_8);
                PhotoData photoData = new Gson().fromJson(json, PhotoData.class);

                if (photoData != null && photoData.categories != null) {
                    for (PhotoCategory category : photoData.categories) {
                        if (categoryName.equals(category.name)) {
                            PhotoFolder clicked = findFolderByName(category.folders, folderName);
                            if (clicked == null) {
                                Log.w(TAG, "Folder not found in JSON: " + folderName);
                                Snackbar.make(findViewById(R.id.folderRecyclerView),
                                        "Folder not found", Snackbar.LENGTH_LONG).show();
                                return;
                            }

                            if (clicked.folders != null && !clicked.folders.isEmpty()) {
                                Intent intent = new Intent(this, PhotoFolderActivity.class);
                                intent.putExtra("category_name", categoryName);
                                intent.putExtra("folder_name", clicked.name);
                                intent.putExtra("json_path", jsonPath);
                                startActivity(intent);
                            } else if (clicked.images != null && !clicked.images.isEmpty()) {
                                Intent intent = new Intent(this, PhotoListActivity.class);
                                intent.putExtra("category_name", categoryName);
                                intent.putExtra("folder_name", clicked.name);
                                intent.putExtra("json_path", jsonPath);
                                startActivity(intent);
                            } else {
                                Log.w(TAG, "Folder is empty: " + clicked.name);
                                Snackbar.make(findViewById(R.id.folderRecyclerView),
                                        "This folder is empty", Snackbar.LENGTH_LONG).show();
                            }
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling folder click", e);
        }
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