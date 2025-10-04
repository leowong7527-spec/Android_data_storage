package com.example.datadisplay;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datadisplay.adapters.PhotoScrollAdapter;
import java.util.ArrayList;
import java.util.List;

public class PhotoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        RecyclerView recyclerView = findViewById(R.id.photoRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        List<String> imageUrls = getIntent().getStringArrayListExtra("images");
        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }

        PhotoScrollAdapter adapter = new PhotoScrollAdapter(imageUrls);
        recyclerView.setAdapter(adapter);
    }
}