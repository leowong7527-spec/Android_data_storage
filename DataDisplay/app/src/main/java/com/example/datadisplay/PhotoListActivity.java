package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datadisplay.adapters.PhotoGridAdapter;
import com.example.datadisplay.models.PhotoFolder;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class PhotoListActivity extends AppCompatActivity implements PhotoGridAdapter.OnItemClickListener {

    private List<String> imageUrls;
    private PhotoFolder currentFolder;  // ✅ store the folder object directly

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);

        RecyclerView recyclerView = findViewById(R.id.photoRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        // ✅ Get the folder JSON directly from intent
        String folderJson = getIntent().getStringExtra("folder_json");
        currentFolder = new Gson().fromJson(folderJson, PhotoFolder.class);

        imageUrls = new ArrayList<>();

        if (currentFolder != null && currentFolder.images != null) {
            imageUrls.addAll(currentFolder.images);
        }

        Log.d("PhotoListActivity", "Total images: " + imageUrls.size());

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
}