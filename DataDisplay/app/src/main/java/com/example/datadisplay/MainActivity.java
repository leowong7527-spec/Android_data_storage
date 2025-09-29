package com.example.datadisplay;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.flexbox.FlexboxLayout;

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

public class MainActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        tagContainer = findViewById(R.id.tagContainer);
        collapsibleTagContainer = findViewById(R.id.collapsibleTagContainer);
        toggleTagsButton = findViewById(R.id.toggleTagsButton);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userList);
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

                    Intent intent = new Intent(MainActivity.this, BookDetailActivity.class);
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

                userList.add(name + " - Author: " + author);
            }

            adapter.notifyDataSetChanged();
            setupTags(uniqueTags);

        } catch (Exception e) {
            Log.e("JSON_PARSE", "Error parsing JSON", e);
        }
    }

    private void setupTags(Set<String> tags) {
        tagContainer.removeAllViews();

        for (String tag : tags) {
            Button tagButton = new Button(this);
            tagButton.setText(tag);
            tagButton.setAllCaps(false);

            // ✅ Add margins so spacing stays consistent
            FlexboxLayout.LayoutParams params =
                    new FlexboxLayout.LayoutParams(
                            FlexboxLayout.LayoutParams.WRAP_CONTENT,
                            FlexboxLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 8, 8, 8); // left, top, right, bottom
            tagButton.setLayoutParams(params);

            // Toggle selection
            tagButton.setOnClickListener(v -> {
                if (selectedTags.contains(tag)) {
                    selectedTags.remove(tag);
                    tagButton.setBackgroundColor(Color.LTGRAY); // deselected
                } else {
                    selectedTags.add(tag);
                    tagButton.setBackgroundColor(Color.CYAN); // selected
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

                    // ✅ Show book if it contains ALL selected tags
                    // New: requires ANY overlap between book tags and selected tags
                    if (selectedTags.isEmpty() || !disjoint(bookTags, selectedTags)) {
                        userList.add(obj.optString("name") + " - Author: " + obj.optString("author"));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private boolean disjoint(Set<String> a, Set<String> b) {
        for (String s : a) {
            if (b.contains(s)) return false;
        }
        return true;
    }

    private void showAllBooks() {
        userList.clear();
        for (int i = 0; i < booksArray.length(); i++) {
            JSONObject obj = booksArray.optJSONObject(i);
            if (obj != null) {
                userList.add(obj.optString("name") + " - Author: " + obj.optString("author"));
            }
        }
        adapter.notifyDataSetChanged();
    }
}