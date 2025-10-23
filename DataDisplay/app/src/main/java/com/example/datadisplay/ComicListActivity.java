package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datadisplay.adapters.ComicGridAdapter;
import com.example.datadisplay.models.PhotoFolder;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class ComicListActivity extends AppCompatActivity implements ComicGridAdapter.OnItemClickListener {

    private List<String> imageUrls;
    private PhotoFolder currentFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_list);

        RecyclerView recyclerView = findViewById(R.id.photoRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        // ✅ Get folder JSON directly
        String folderJson = getIntent().getStringExtra("folder_json");
        currentFolder = new Gson().fromJson(folderJson, PhotoFolder.class);

        imageUrls = new ArrayList<>();
        if (currentFolder != null && currentFolder.images != null) {
            imageUrls.addAll(currentFolder.images);
        }

        Log.d("ComicListActivity", "Total images: " + imageUrls.size());

        ComicGridAdapter adapter = new ComicGridAdapter(this, imageUrls, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(String imageUrl, int position) {
        // ✅ We already have the clicked image URL
        Intent intent = new Intent(this, PhotoActivity.class);
        intent.putStringArrayListExtra("images", new ArrayList<>(imageUrls));
        intent.putExtra("position", position);
        startActivity(intent);
    }
}