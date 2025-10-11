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

        folderList = new ArrayList<>();

        if (jsonPath != null && categoryName != null) {
            try {
                String json = new String(Files.readAllBytes(new File(jsonPath).toPath()), StandardCharsets.UTF_8);
                PhotoData photoData = new Gson().fromJson(json, PhotoData.class);

                for (PhotoCategory category : photoData.categories) {
                    if (category.name.equals(categoryName)) {
                        folderList = category.folders;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        PhotoFolderAdapter adapter = new PhotoFolderAdapter(folderList, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onFolderClick(String folderName) {
        Intent intent = new Intent(this, PhotoListActivity.class);
        intent.putExtra("category", categoryName);
        intent.putExtra("folder", folderName);
        intent.putExtra("json_path", jsonPath); // ✅ pass path only
        startActivity(intent);
    }
}