package com.example.datadisplay;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.navigation.NavigationView;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class BookActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> userList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    String jsonUrl = "https://raw.githubusercontent.com/leowong7527-spec/Android_data_storage/main/data.json";

    JSONArray booksArray;
    FlexboxLayout tagContainer;
    LinearLayout collapsibleTagContainer;
    Button toggleTagsButton;

    Set<String> selectedTags = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);

        listView = findViewById(R.id.listView);
        tagContainer = findViewById(R.id.tagContainer);
        collapsibleTagContainer = findViewById(R.id.collapsibleTagContainer);
        toggleTagsButton = findViewById(R.id.toggleTagsButton);

        adapter = new ArrayAdapter<>(this, R.layout.list_item, userList);
        listView.setAdapter(adapter);

        fetchJson();

        // Toggle show/hide tags
        toggleTagsButton.setOnClickListener(v -> {
            if (collapsibleTagContainer.getVisibility() == View.GONE) {
                collapsibleTagContainer.setVisibility(View.VISIBLE);
                toggleTagsButton.setText("Hide Tags");
            } else {
                collapsibleTagContainer.setVisibility(View.GONE);
                toggleTagsButton.setText("Show Tags");
            }
        });

        // Handle book clicks
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (booksArray != null) {
                try {
                    JSONObject clickedObj = booksArray.getJSONObject(position);

                    String name = clickedObj.optString("name");
                    String author = clickedObj.optString("author");
                    String content = clickedObj.optString("content");

                    // ✅ Detect hash vs UTF-8
                    if (looksLikeHash(content)) {
                        content = "[HASH] " + content;
                    } else {
                        content = content.replace("\\n", "\n");
                    }

                    // Handle both single string and array for "tag"
                    String tagValue = "";
                    if (clickedObj.has("tag")) {
                        Object tagObj = clickedObj.get("tag");
                        if (tagObj instanceof JSONArray) {
                            JSONArray tagArray = (JSONArray) tagObj;
                            ArrayList<String> tags = new ArrayList<>();
                            for (int j = 0; j < tagArray.length(); j++) {
                                tags.add(tagArray.optString(j));
                            }
                            tagValue = String.join(", ", tags);
                        } else {
                            tagValue = clickedObj.optString("tag", "General");
                        }
                    } else {
                        tagValue = "General";
                    }

                    Log.d("BOOK_CLICK", "Name: " + name);
                    Log.d("BOOK_CLICK", "Author: " + author);
                    Log.d("BOOK_CLICK", "Tag(s): " + tagValue);
                    Log.d("BOOK_CLICK", "Content: " + content);

                    Intent intent = new Intent(BookActivity.this, BookDetailActivity.class);
                    intent.putExtra("name", name);
                    intent.putExtra("author", author);
                    intent.putExtra("content", content);
                    intent.putExtra("tag", tagValue);
                    startActivity(intent);

                } catch (Exception e) {
                    Log.e("BOOK_CLICK", "Error handling item click", e);
                }
            }
        });


        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
            } else if (id == R.id.nav_books) {
                drawerLayout.closeDrawers();
            } else if (id == R.id.nav_photos) {
                startActivity(new Intent(this, PhotoActivity.class));
            } else if (id == R.id.nav_settings) {

        }
            drawerLayout.closeDrawers();
            return true;
        });
    }


    private void fetchJson() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(jsonUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    runOnUiThread(() -> parseJson(jsonData));
                }
            }
        });
    }

    private void parseJson(String jsonData) {
        Log.d("JSON_PARSE", "Starting to parse JSON data...");
        try {
            booksArray = new JSONArray(jsonData);
            Log.d("JSON_PARSE", "JSONArray length: " + booksArray.length());

            userList.clear();
            Set<String> uniqueTags = new HashSet<>();

            for (int i = 0; i < booksArray.length(); i++) {
                JSONObject obj = booksArray.getJSONObject(i);
                String name = obj.optString("name", "Unknown");
                String author = obj.optString("author", "N/A");

                // Collect tags
                if (obj.has("tag")) {
                    Object tagObj = obj.get("tag");
                    if (tagObj instanceof JSONArray) {
                        JSONArray tagArray = (JSONArray) tagObj;
                        for (int j = 0; j < tagArray.length(); j++) {
                            uniqueTags.add(tagArray.optString(j));
                        }
                    } else {
                        uniqueTags.add(obj.optString("tag"));
                    }
                }

                String displayName = limitWords(name, 10);
                userList.add(displayName);
            }

            adapter.notifyDataSetChanged();
            setupTags(uniqueTags);

        } catch (Exception e) {
            Log.e("JSON_PARSE", "Error parsing JSON", e);
        }
    }


    private String limitWords(String text, int maxWords) {
        String[] words = text.split("\\s+");
        if (words.length <= maxWords) return text;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            sb.append(words[i]).append(" ");
        }
        return sb.toString().trim() + "...";
    }
    private void setupTags(Set<String> tags) {
        tagContainer.removeAllViews();

        for (String tag : tags) {
            Button tagButton = new Button(this);
            tagButton.setText(tag);
            tagButton.setAllCaps(false);

            // Initial background color
            tagButton.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));

            FlexboxLayout.LayoutParams params =
                    new FlexboxLayout.LayoutParams(
                            FlexboxLayout.LayoutParams.WRAP_CONTENT,
                            FlexboxLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 8, 8, 8);
            tagButton.setLayoutParams(params);

            tagButton.setOnClickListener(v -> {
                if (selectedTags.contains(tag)) {
                    selectedTags.remove(tag);
                    tagButton.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY)); // Unselected
                } else {
                    selectedTags.add(tag);
                    tagButton.setBackgroundTintList(ColorStateList.valueOf(Color.CYAN)); // Selected
                }
                filterBooksByTags();
            });

            tagContainer.addView(tagButton);
        }

        // "All" button
        Button allButton = new Button(this);
        allButton.setText("All");
        allButton.setAllCaps(false);

        FlexboxLayout.LayoutParams params =
                new FlexboxLayout.LayoutParams(
                        FlexboxLayout.LayoutParams.WRAP_CONTENT,
                        FlexboxLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(8, 8, 8, 8);
        allButton.setLayoutParams(params);

        allButton.setOnClickListener(v -> {
            selectedTags.clear();
            showAllBooks();
        });

        tagContainer.addView(allButton, 0);
    }

    private void filterBooksByTags() {
        userList.clear();

        for (int i = 0; i < booksArray.length(); i++) {
            JSONObject obj = booksArray.optJSONObject(i);
            if (obj != null) {
                try {
                    Set<String> bookTags = new HashSet<>();
                    Object tagObj = obj.get("tag");

                    if (tagObj instanceof JSONArray) {
                        JSONArray tagArray = (JSONArray) tagObj;
                        for (int j = 0; j < tagArray.length(); j++) {
                            bookTags.add(tagArray.optString(j));
                        }
                    } else {
                        bookTags.add(obj.optString("tag"));
                    }

                    if (selectedTags.isEmpty() || bookTags.containsAll(selectedTags)) {
                        userList.add(obj.optString("name") + " - Author: " + obj.optString("author"));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        adapter.notifyDataSetChanged();
    }


    private void showAllBooks() {
        userList.clear();
        for (int i = 0; i < booksArray.length(); i++) {
            JSONObject obj = booksArray.optJSONObject(i);
            if (obj != null) {
                userList.add(obj.optString("name"));
            }
        }
        adapter.notifyDataSetChanged();
    }

    // ✅ Helper: detect if string looks like a hash
    private boolean looksLikeHash(String s) {
        return s != null &&
                (
                        // Hexadecimal hashes (MD5 = 32 chars, SHA‑1 = 40 chars, SHA‑256 = 64 chars)
                        s.matches("^[a-fA-F0-9]{32,64}$")
                                ||
                                // Base64‑like strings (commonly 20+ chars, letters/numbers/+/=)
                                s.matches("^[A-Za-z0-9+/=]{20,}$")
                );
    }
}