package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datadisplay.adapters.ComicGridAdapter;
import com.example.datadisplay.models.PhotoCategory;
import com.example.datadisplay.models.PhotoData;
import com.example.datadisplay.models.PhotoFolder;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class ComicListActivity extends AppCompatActivity implements ComicGridAdapter.OnItemClickListener {

    private List<String> imageUrls;
    private String categoryName;
    private String folderName;
    private String json;

    private String encodePathSegment(String segment) {
        try {
            return URLEncoder.encode(segment, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return segment;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_list);

        RecyclerView recyclerView = findViewById(R.id.photoRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        categoryName = getIntent().getStringExtra("category");
        folderName = getIntent().getStringExtra("folder");
        json = getIntent().getStringExtra("json");

        Log.d("ComicListActivity", "Category: " + categoryName + ", Folder: " + folderName);
        Log.d("ComicListActivity", "JSON: " + json);

        imageUrls = new ArrayList<>();

        if (json != null && categoryName != null && folderName != null) {
            Gson gson = new Gson();
            PhotoData comicData = gson.fromJson(json, PhotoData.class);

            if (comicData != null && comicData.categories != null) {
                for (PhotoCategory category : comicData.categories) {
                    Log.d("ComicListActivity", "Checking category: " + category.name);
                    if (category.name.equals(categoryName)) {
                        for (PhotoFolder folder : category.folders) {
                            Log.d("ComicListActivity", "Checking folder: " + folder.name);
                            if (folder.name.equals(folderName)) {
                                String baseUrl = "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/comic_image/";
                                for (String imageName : folder.images) {
                                    String fullUrl = baseUrl
                                            + encodePathSegment(category.name) + "/"
                                            + encodePathSegment(folder.name) + "/"
                                            + encodePathSegment(imageName);
                                    Log.d("ComicListActivity", "Adding image URL: " + fullUrl);
                                    imageUrls.add(fullUrl);
                                }
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }

        Log.d("ComicListActivity", "Total images: " + imageUrls.size());

        ComicGridAdapter adapter = new ComicGridAdapter(this, imageUrls, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent(this, PhotoActivity.class);
        intent.putStringArrayListExtra("images", new ArrayList<>(imageUrls));
        intent.putExtra("position", position);
        startActivity(intent);
    }
}