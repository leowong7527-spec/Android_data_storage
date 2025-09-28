package com.example.datadisplay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> userList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    String jsonUrl = "https://raw.githubusercontent.com/LEO7526/Android_data_storage/main/data.json";

    // Keep the parsed JSON array so we can access full details on click
    JSONArray booksArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userList);
        listView.setAdapter(adapter);

        fetchJson();

        // Handle clicks once, outside the loop
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (booksArray != null) {
                try {
                    JSONObject clickedObj = booksArray.getJSONObject(position);

                    String name = clickedObj.optString("name");
                    String author = clickedObj.optString("author");
                    String content = clickedObj.optString("content");

                    // ✅ Handle both single string and array for "tag"
                    String tagValue = "";
                    if (clickedObj.has("tag")) {
                        Object tagObj = clickedObj.get("tag");
                        if (tagObj instanceof JSONArray) {
                            JSONArray tagArray = (JSONArray) tagObj;
                            ArrayList<String> tags = new ArrayList<>();
                            for (int j = 0; j < tagArray.length(); j++) {
                                tags.add(tagArray.optString(j));
                            }
                            tagValue = String.join(", ", tags); // join multiple tags
                        } else {
                            tagValue = clickedObj.optString("tag", "General");
                        }
                    } else {
                        tagValue = "General";
                    }

                    // 🔍 Log what we’re passing
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
            for (int i = 0; i < booksArray.length(); i++) {
                JSONObject obj = booksArray.getJSONObject(i);
                String name = obj.optString("name", "Unknown");
                String author = obj.optString("author", "N/A");

                // Show only name + author in the list
                userList.add(name + " - Author: " + author);

                Log.d("JSON_PARSE", "Parsed item " + i + ": " + name + " by " + author);
            }

            adapter.notifyDataSetChanged();
            Log.d("JSON_PARSE", "Adapter updated with new data.");
        } catch (Exception e) {
            Log.e("JSON_PARSE", "Error parsing JSON", e);
        }
    }

}