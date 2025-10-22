package com.example.datadisplay;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.datadisplay.adapters.PhotoGridAdapter;
import com.example.datadisplay.models.PhotoCategory;
import com.example.datadisplay.models.PhotoData;
import com.example.datadisplay.models.PhotoFolder;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class PhotoListActivity extends AppCompatActivity implements PhotoGridAdapter.OnItemClickListener {

    private List<String> imageUrls;
    private String categoryName;
    private String folderName;
    private String jsonPath;

    private ViewPager2 fullscreenViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);

        RecyclerView recyclerView = findViewById(R.id.photoRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        fullscreenViewPager = findViewById(R.id.fullscreenViewPager);

        categoryName = getIntent().getStringExtra("category");
        folderName = getIntent().getStringExtra("folder");
        jsonPath = getIntent().getStringExtra("json_path");

        imageUrls = new ArrayList<>();

        if (jsonPath != null && categoryName != null && folderName != null) {
            try {
                String json = new String(Files.readAllBytes(new File(jsonPath).toPath()), StandardCharsets.UTF_8);
                PhotoData photoData = new Gson().fromJson(json, PhotoData.class);

                if (photoData != null && photoData.categories != null) {
                    for (PhotoCategory category : photoData.categories) {
                        if (category.name.equals(categoryName)) {
                            for (PhotoFolder folder : category.folders) {
                                if (folder.name.equals(folderName)) {
                                    imageUrls.addAll(folder.images);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Log.d("PhotoListActivity", "Total images: " + imageUrls.size());

        PhotoGridAdapter adapter = new PhotoGridAdapter(this, imageUrls, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(int position) {
        fullscreenViewPager.setVisibility(View.VISIBLE);
        FullscreenPagerAdapter pagerAdapter = new FullscreenPagerAdapter(
                imageUrls,
                () -> fullscreenViewPager.setVisibility(View.GONE) // double tap exit
        );
        fullscreenViewPager.setAdapter(pagerAdapter);
        fullscreenViewPager.setCurrentItem(position, false);
    }

    // Inner adapter for fullscreen pager
    private class FullscreenPagerAdapter extends RecyclerView.Adapter<FullscreenPagerAdapter.ViewHolder> {

        private List<String> images;
        private Runnable onDoubleTapExit;

        FullscreenPagerAdapter(List<String> images, Runnable onDoubleTapExit) {
            this.images = images;
            this.onDoubleTapExit = onDoubleTapExit;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            PhotoView photoView = new PhotoView(parent.getContext());
            photoView.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT
            ));
            photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new ViewHolder(photoView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String url = images.get(position);
            Picasso.get().load(url).into(holder.photoView);

            GestureDetector gestureDetector = new GestureDetector(holder.photoView.getContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            if (onDoubleTapExit != null) onDoubleTapExit.run();
                            return true;
                        }
                    });

            holder.photoView.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            PhotoView photoView;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                photoView = (PhotoView) itemView;
            }
        }
    }
}
