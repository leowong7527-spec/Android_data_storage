package com.example.datadisplay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.List;
import java.util.Random;

public class RadioPlaybackService extends Service {

    private static final String TAG = "RadioPlaybackService";

    public static final String CHANNEL_ID = "RadioPlaybackChannel";
    private MediaPlayer mediaPlayer;
    private List<String> allUrls;
    private String currentUrl;
    private boolean isShuffling = false;
    private boolean isLooping = false;

    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_SHUFFLE = "ACTION_SHUFFLE";
    public static final String ACTION_LOOP = "ACTION_LOOP";
    public static final String ACTION_STATUS = "ACTION_STATUS";
    public static final String ACTION_PROGRESS = "ACTION_PROGRESS";
    public static final String ACTION_SEEK = "ACTION_SEEK";
    public static final String ACTION_TRACK_CHANGED = "ACTION_TRACK_CHANGED";
    public static final String ACTION_REQUEST_STATUS = "ACTION_REQUEST_STATUS";

    public static final String ACTION_NEXT = "ACTION_NEXT";

    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";

    private final Handler progressHandler = new Handler();
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                int pos = mediaPlayer.getCurrentPosition();
                int dur = mediaPlayer.getDuration();
                Intent progressIntent = new Intent(ACTION_PROGRESS);
                progressIntent.setPackage(getPackageName());
                progressIntent.putExtra("position", pos);
                progressIntent.putExtra("duration", dur);
                sendBroadcast(progressIntent);
                progressHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Radio Playing")
                .setContentText("Streaming audio...")
                .setSmallIcon(R.drawable.ic_radio)
                .build();

        startForeground(1, notification);

        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_PLAY:
                    if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                        mediaPlayer.start();
                        broadcastStatus(true);
                        progressHandler.post(progressRunnable);
                    }
                    return START_STICKY;

                case ACTION_PAUSE:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        broadcastStatus(false);
                        progressHandler.removeCallbacks(progressRunnable);
                    }
                    return START_STICKY;

                case ACTION_LOOP:
                    isLooping = intent.getBooleanExtra("loop", false);
                    return START_STICKY;

                case ACTION_SHUFFLE:
                    isShuffling = intent.getBooleanExtra("shuffle", false);
                    return START_STICKY;

                case ACTION_SEEK:
                    int newPos = intent.getIntExtra("position", 0);
                    if (mediaPlayer != null) {
                        mediaPlayer.seekTo(newPos);
                    }
                    return START_STICKY;

                case ACTION_REQUEST_STATUS:
                    broadcastStatus(mediaPlayer != null && mediaPlayer.isPlaying());
                    if (currentUrl != null) {
                        broadcastTrackChanged(currentUrl);
                    }
                    return START_STICKY;

                case ACTION_NEXT:
                    playNextTrack();
                    return START_STICKY;

                case ACTION_PREVIOUS:
                    playPreviousTrack();
                    return START_STICKY;
            }
        }

        // Initial playback setup
        currentUrl = intent.getStringExtra("url");
        allUrls = intent.getStringArrayListExtra("allUrls");
        isShuffling = intent.getBooleanExtra("shuffle", false);
        isLooping = intent.getBooleanExtra("loop", false);

        playTrack(currentUrl);
        return START_STICKY;
    }


    private void playPreviousTrack() {
        if (isShuffling) {
            playNextRandomTrack(); // or implement playPreviousRandomTrack() if needed
            return;
        }

        if (allUrls == null || allUrls.isEmpty()) return;

        int currentIndex = allUrls.indexOf(currentUrl);
        if (currentIndex == -1) currentIndex = 0;

        int prevIndex = (currentIndex - 1 + allUrls.size()) % allUrls.size();
        String prevUrl = allUrls.get(prevIndex);
        currentUrl = prevUrl;

        playTrack(prevUrl);
    }

    private void playNextTrack() {
        if (isShuffling) {
            playNextRandomTrack();
            return;
        }

        if (allUrls == null || allUrls.isEmpty()) return;

        int currentIndex = allUrls.indexOf(currentUrl);
        if (currentIndex == -1) currentIndex = 0;

        int nextIndex = (currentIndex + 1) % allUrls.size();
        String nextUrl = allUrls.get(nextIndex);
        currentUrl = nextUrl;

        playTrack(nextUrl);
    }
    private void playTrack(String url) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.reset();
            } else {
                mediaPlayer = new MediaPlayer();
            }

            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                broadcastStatus(true);
                broadcastTrackChanged(url);
                progressHandler.removeCallbacks(progressRunnable);
                progressHandler.post(progressRunnable);
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                broadcastStatus(false);
                progressHandler.removeCallbacks(progressRunnable);

                if (isShuffling && allUrls != null && allUrls.size() > 1) {
                    playNextRandomTrack();
                } else if (isLooping) {
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();
                    broadcastStatus(true);
                    progressHandler.post(progressRunnable);
                }
            });
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "Error playing track: " + e.getMessage(), e);
            broadcastStatus(false);
            if (isShuffling && allUrls != null && allUrls.size() > 1) {
                playNextRandomTrack();
            }
        }
    }

    private void playNextRandomTrack() {
        if (allUrls == null || allUrls.isEmpty()) return;
        String nextUrl;
        do {
            int randomIndex = new Random().nextInt(allUrls.size());
            nextUrl = allUrls.get(randomIndex);
        } while (nextUrl.equals(currentUrl));
        currentUrl = nextUrl;
        playTrack(nextUrl);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Radio Playback Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void broadcastStatus(boolean isPlaying) {
        Intent statusIntent = new Intent(ACTION_STATUS);
        statusIntent.setPackage(getPackageName());
        statusIntent.putExtra("isPlaying", isPlaying);
        sendBroadcast(statusIntent);
    }

    private void broadcastTrackChanged(String url) {
        Intent intent = new Intent(ACTION_TRACK_CHANGED);
        intent.setPackage(getPackageName());
        intent.putExtra("url", url);
        intent.putExtra("title", extractTitleFromUrl(url));
        sendBroadcast(intent);
    }

    private String extractTitleFromUrl(String url) {
        if (url == null) return "Unknown";
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        return fileName.replace("%20", " ");
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        progressHandler.removeCallbacks(progressRunnable);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}