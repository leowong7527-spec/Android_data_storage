package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datadisplay.adapters.PhotoGridAdapter;
import com.example.datadisplay.models.PhotoCategory;
import com.example.datadisplay.models.PhotoData;
import com.example.datadisplay.models.PhotoFolder;
import com.example.datadisplay.utils.PhotoCacheHelper;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class PhotoListActivity extends AppCompatActivity implements PhotoGridAdapter.OnItemClickListener {

    private List<String> imageUrls;
    private String categoryName;
    private String folderName;
    private String json;

    private String encodePathSegment(String segment) {
        try {
            return URLEncoder.encode(segment, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return segment;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);

        RecyclerView recyclerView = findViewById(R.id.photoRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        // Get data from intent
        categoryName = getIntent().getStringExtra("category");
        folderName = getIntent().getStringExtra("folder");
        json = getIntent().getStringExtra("json");

        // 🔑 Offline fallback: if no JSON passed, try cached JSON
        if (json == null) {
            json = PhotoCacheHelper.loadJsonFromCache(this);
            if (json != null) {
                Snackbar.make(findViewById(R.id.photoRecyclerView),
                                "Showing cached photos (offline mode)",
                                Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry", v -> {
                            // 🔁 Attempt to reload fresh JSON from network
                            fetchFreshJsonFromNetwork();
                        })
                        .show();
            }
        }

        imageUrls = new ArrayList<>();

        if (json != null && categoryName != null && folderName != null) {
            Gson gson = new Gson();
            PhotoData photoData = gson.fromJson(json, PhotoData.class);

            for (PhotoCategory category : photoData.categories) {
                if (category.name.equals(categoryName)) {
                    for (PhotoFolder folder : category.folders) {
                        if (folder.name.equals(folderName)) {
                            String baseUrl = "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/photo_image/";
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
        Intent intent = new Intent(this, PhotoActivity.class);
        intent.putStringArrayListExtra("images", new ArrayList<>(imageUrls));
        intent.putExtra("position", position);
        startActivity(intent);
    }


    private void fetchFreshJsonFromNetwork() {
        String url = "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/photo_data/photo_data.json";

        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(() -> {
                    Snackbar.make(findViewById(R.id.photoRecyclerView),
                            "Failed to fetch fresh data",
                            Snackbar.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    String freshJson = response.body().string();
                    PhotoCacheHelper.saveJsonToCache(PhotoListActivity.this, freshJson);

                    // Restart activity with fresh JSON
                    Intent intent = getIntent();
                    intent.putExtra("json", freshJson);
                    finish(); // close current
                    startActivity(intent); // restart with fresh data
                }
            }
        });
    }
}