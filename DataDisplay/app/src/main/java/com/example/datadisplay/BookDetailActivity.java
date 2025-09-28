package com.example.datadisplay;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class BookDetailActivity extends AppCompatActivity {

    TextView bookDetailText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        bookDetailText = findViewById(R.id.bookDetailText);

        String name = getIntent().getStringExtra("name");
        String author = getIntent().getStringExtra("author");
        String content = getIntent().getStringExtra("content");
        String tag = getIntent().getStringExtra("tag");

        // 🔍 Log each value for debugging
        Log.d("BOOK_DETAIL", "Received name: " + name);
        Log.d("BOOK_DETAIL", "Received author: " + author);
        Log.d("BOOK_DETAIL", "Received tag: " + tag);
        Log.d("BOOK_DETAIL", "Received content: " + content);

        String details = "Book name: " + name +
                "\n Author: " + author +
                "\n Tag: " + tag +
                "\n\n" + content;

        bookDetailText.setText(details);

        Log.d("BOOK_DETAIL", "Displayed details successfully.");
    }
}