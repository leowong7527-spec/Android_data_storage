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

                    // 🔍 Log which item was clicked
                    Log.d("BOOK_CLICK", "Item clicked at position: " + position);
                    Log.d("BOOK_CLICK", "Name: " + clickedObj.optString("name"));
                    Log.d("BOOK_CLICK", "Author: " + clickedObj.optString("author"));
                    Log.d("BOOK_CLICK", "Tag: " + clickedObj.optString("tag"));
                    Log.d("BOOK_CLICK", "Content: " + clickedObj.optString("content"));

                    Intent intent = new Intent(MainActivity.this, BookDetailActivity.class);
                    intent.putExtra("name", clickedObj.optString("name"));
                    intent.putExtra("author", clickedObj.optString("author"));
                    intent.putExtra("content", clickedObj.optString("content"));
                    intent.putExtra("tag", clickedObj.optString("tag"));
                    startActivity(intent);

                    Log.d("BOOK_CLICK", "Intent started for BookDetailActivity");

                } catch (Exception e) {
                    Log.e("BOOK_CLICK", "Error handling item click", e);
                }
            } else {
                Log.w("BOOK_CLICK", "booksArray is null, cannot open details");
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