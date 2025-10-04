package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datadisplay.adapters.PhotoFolderAdapter; // reuse
import com.example.datadisplay.models.PhotoCategory;
import com.example.datadisplay.models.PhotoData;
import com.example.datadisplay.models.PhotoFolder;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class ComicFolderActivity extends AppCompatActivity implements PhotoFolderAdapter.OnFolderClickListener {

    private List<PhotoFolder> folderList;
    private String categoryName;
    private String json;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_folder);

        RecyclerView recyclerView = findViewById(R.id.folderRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        categoryName = getIntent().getStringExtra("category");
        json = getIntent().getStringExtra("json");

        folderList = new ArrayList<>();

        if (json != null && categoryName != null) {
            Gson gson = new Gson();
            PhotoData comicData = gson.fromJson(json, PhotoData.class);

            for (PhotoCategory category : comicData.categories) {
                if (category.name.equals(categoryName)) {
                    folderList = category.folders;
                    break;
                }
            }
        }

        PhotoFolderAdapter adapter = new PhotoFolderAdapter(folderList, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onFolderClick(String folderName) {
        Intent intent = new Intent(this, ComicListActivity.class);
        intent.putExtra("category", categoryName);
        intent.putExtra("folder", folderName);
        intent.putExtra("json", json);
        startActivity(intent);
    }
}