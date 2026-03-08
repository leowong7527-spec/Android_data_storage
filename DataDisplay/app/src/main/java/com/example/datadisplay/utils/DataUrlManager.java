package com.example.datadisplay.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

public class DataUrlManager {
    private static final String PREF_NAME = "DataSourceSettings";
    private static final String KEY_MP3_URL = "mp3_data_url";
    private static final String KEY_COMIC_URL = "comic_data_url";
    private static final String KEY_PHOTO_URL = "photo_data_url";
    private static final String KEY_BOOK_URL = "book_data_url";

    // Default Google Drive share links.
    private static final String DEFAULT_MP3_URL = "https://drive.google.com/file/d/1MCuzBSSmeVzsPP9IBy4LdUj02VEeA2FU/view?usp=sharing";
    private static final String DEFAULT_COMIC_URL = "https://drive.google.com/file/d/1JKKpAczBJ2jxl7yhAY7AVxyYfRembgFo/view?usp=sharing";
    private static final String DEFAULT_PHOTO_URL = "https://drive.google.com/file/d/1p0slCvo3j543GWzG85J21Twy7nT49Y2Y/view?usp=sharing";
    private static final String DEFAULT_BOOK_URL = "https://drive.google.com/file/d/1tjla5WD0elmmpYQkYcIY1ApCoYPcUxQ-/view?usp=sharing";

    private final SharedPreferences sharedPreferences;

    public DataUrlManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getMp3DataUrl() {
        return sharedPreferences.getString(KEY_MP3_URL, DEFAULT_MP3_URL);
    }

    public String getComicDataUrl() {
        return sharedPreferences.getString(KEY_COMIC_URL, DEFAULT_COMIC_URL);
    }

    public String getPhotoDataUrl() {
        return sharedPreferences.getString(KEY_PHOTO_URL, DEFAULT_PHOTO_URL);
    }

    public String getBookDataUrl() {
        return sharedPreferences.getString(KEY_BOOK_URL, DEFAULT_BOOK_URL);
    }

    public String getMp3DownloadUrl() {
        return toDownloadUrl(getMp3DataUrl());
    }

    public String getComicDownloadUrl() {
        return toDownloadUrl(getComicDataUrl());
    }

    public String getPhotoDownloadUrl() {
        return toDownloadUrl(getPhotoDataUrl());
    }

    public String getBookDownloadUrl() {
        return toDownloadUrl(getBookDataUrl());
    }

    public void setMp3DataUrl(String url) {
        sharedPreferences.edit().putString(KEY_MP3_URL, url).apply();
    }

    public void setComicDataUrl(String url) {
        sharedPreferences.edit().putString(KEY_COMIC_URL, url).apply();
    }

    public void setPhotoDataUrl(String url) {
        sharedPreferences.edit().putString(KEY_PHOTO_URL, url).apply();
    }

    public void setBookDataUrl(String url) {
        sharedPreferences.edit().putString(KEY_BOOK_URL, url).apply();
    }

    public void clearCache() {
        sharedPreferences.edit().putLong("last_update_time", System.currentTimeMillis()).apply();
    }

    private String toDownloadUrl(String url) {
        if (url == null) {
            return "";
        }

        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        if (!trimmed.contains("drive.google.com")) {
            return trimmed;
        }

        if (trimmed.contains("uc?export=download")) {
            return trimmed;
        }

        String fileId = extractGoogleDriveFileId(trimmed);
        if (fileId == null || fileId.isEmpty()) {
            return trimmed;
        }

        return "https://drive.google.com/uc?export=download&id=" + fileId;
    }

    private String extractGoogleDriveFileId(String url) {
        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath();

            if (path != null) {
                String marker = "/file/d/";
                int markerIndex = path.indexOf(marker);
                if (markerIndex >= 0) {
                    int start = markerIndex + marker.length();
                    int end = path.indexOf('/', start);
                    return end > start ? path.substring(start, end) : path.substring(start);
                }
            }

            String queryId = uri.getQueryParameter("id");
            if (queryId != null && !queryId.isEmpty()) {
                return queryId;
            }
        } catch (Exception ignored) {
            // Keep original URL when parsing fails.
        }

        return null;
    }
}
