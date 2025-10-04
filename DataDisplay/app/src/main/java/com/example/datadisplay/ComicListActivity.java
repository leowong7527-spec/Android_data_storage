package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datadisplay.adapters.PhotoGridAdapter; // reuse
import com.example.datadisplay.models.PhotoCategory;
import com.example.datadisplay.models.PhotoData;
import com.example.datadisplay.models.PhotoFolder;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class ComicListActivity extends AppCompatActivity implements PhotoGridAdapter.OnItemClickListener {

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
        setContentView(R.layout.activity_photo_list);

        RecyclerView recyclerView = findViewById(R.id.photoRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        categoryName = getIntent().getStringExtra("category");
        folderName = getIntent().getStringExtra("folder");
        json = getIntent().getStringExtra("json");

        imageUrls = new ArrayList<>();

        if (json != null && categoryName != null && folderName != null) {
            Gson gson = new Gson();
            PhotoData comicData = gson.fromJson(json, PhotoData.class);

            for (PhotoCategory category : comicData.categories) {
                if (category.name.equals(categoryName)) {
                    for (PhotoFolder folder : category.folders) {
                        if (folder.name.equals(folderName)) {
                            String baseUrl = "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/comic_image/";
                            for (String imageName : folder.images) {
                                String encodedCategory = encodePathSegment(category.name);
                                String encodedFolder = encodePathSegment(folder.name);
                                String encodedImage = encodePathSegment(imageName);
                                String fullUrl = baseUrl + encodedCategory + "/" + encodedFolder + "/" + encodedImage;
                                imageUrls.add(fullUrl);
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        }

        PhotoGridAdapter adapter = new PhotoGridAdapter(this, imageUrls, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent(this, PhotoActivity.class); // reuse fullscreen viewer
        intent.putStringArrayListExtra("images", new ArrayList<>(imageUrls));
        intent.putExtra("position", position);
        startActivity(intent);
    }
}