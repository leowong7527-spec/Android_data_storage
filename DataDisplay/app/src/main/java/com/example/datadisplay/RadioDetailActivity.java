package com.example.datadisplay;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;
import java.util.Random;

public class RadioDetailActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private boolean isLooping = false;
    private String currentUrl;
    private List<String> allUrls;

    private TextView titleText;
    private TextView durationText;
    private TextView elapsedTimeText;
    private TextView totalTimeText;
    private ImageButton playPauseButton;
    private SeekBar progressBar;

    private boolean isShuffling = false;

    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio_detail);

        titleText = findViewById(R.id.titleText);
        durationText = findViewById(R.id.durationText);
        elapsedTimeText = findViewById(R.id.elapsedTimeText);
        totalTimeText = findViewById(R.id.totalTimeText);
        playPauseButton = findViewById(R.id.playPauseButton);
        Button loopButton = findViewById(R.id.loopButton);
        Button shuffleButton = findViewById(R.id.shuffleButton);
        progressBar = findViewById(R.id.progressBar);

        String title = getIntent().getStringExtra("title");
        currentUrl = getIntent().getStringExtra("url");
        allUrls = getIntent().getStringArrayListExtra("allUrls");

        titleText.setText(title);

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(currentUrl);
            mediaPlayer.setOnPreparedListener(mp -> {
                int durationMs = mp.getDuration();
                durationText.setText("Duration: " + formatTime(durationMs));
                totalTimeText.setText(formatTime(durationMs));
                progressBar.setMax(durationMs);
                mp.start();
                isPlaying = true;
                playPauseButton.setImageResource(R.drawable.ic_pause);
                startProgressUpdater();
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Toast.makeText(this, "Playback error", Toast.LENGTH_SHORT).show();
        }

        playPauseButton.setOnClickListener(v -> {
            if (isPlaying) {
                mediaPlayer.pause();
                isPlaying = false;
                playPauseButton.setImageResource(R.drawable.ic_play);
                stopProgressUpdater();
            } else {
                mediaPlayer.start();
                isPlaying = true;
                playPauseButton.setImageResource(R.drawable.ic_pause);
                startProgressUpdater();
            }
        });

        loopButton.setOnClickListener(v -> {
            isLooping = !isLooping;
            mediaPlayer.setLooping(isLooping);
            Toast.makeText(this, isLooping ? "Looping enabled" : "Looping disabled", Toast.LENGTH_SHORT).show();
        });

        shuffleButton.setOnClickListener(v -> {
            isShuffling = !isShuffling;
            Toast.makeText(this, isShuffling ? "Shuffle mode ON" : "Shuffle mode OFF", Toast.LENGTH_SHORT).show();

            if (isShuffling) {
                playNextRandomTrack();
            }
        });

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null && isPlaying) {
                    mediaPlayer.seekTo(progress);
                    elapsedTimeText.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopProgressUpdater();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                startProgressUpdater();
            }
        });
    }

    private void playNewTrack(String url) {
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            stopProgressUpdater();
            progressBar.setProgress(0);
            elapsedTimeText.setText("00:00");

            try {
                mediaPlayer.setDataSource(url);
                mediaPlayer.setOnPreparedListener(mp -> {
                    int durationMs = mp.getDuration();
                    durationText.setText("Duration: " + formatTime(durationMs));
                    totalTimeText.setText(formatTime(durationMs));
                    progressBar.setMax(durationMs);
                    mp.start();
                    isPlaying = true;
                    playPauseButton.setImageResource(R.drawable.ic_pause);
                    titleText.setText(extractTitleFromUrl(url));
                    startProgressUpdater();

                    // 🔁 Auto-play next random track if shuffle is active
                    mediaPlayer.setOnCompletionListener(completed -> {
                        if (isShuffling) {
                            playNextRandomTrack();
                        } else {
                            isPlaying = false;
                            playPauseButton.setImageResource(R.drawable.ic_play);
                            stopProgressUpdater();
                            progressBar.setProgress(progressBar.getMax());
                            elapsedTimeText.setText(totalTimeText.getText());
                        }
                    });
                });

                mediaPlayer.prepareAsync();
                currentUrl = url;

            } catch (Exception e) {
                Toast.makeText(this, "Shuffle error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startProgressUpdater() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    progressBar.setProgress(currentPosition);
                    elapsedTimeText.setText(formatTime(currentPosition));
                    progressHandler.postDelayed(this, 1000);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdater() {
        progressHandler.removeCallbacks(progressRunnable);
    }

    private String extractTitleFromUrl(String url) {
        String[] parts = url.split("/");
        String filename = parts[parts.length - 1];
        return filename.replace(".mp3", "").replace("_", " ");
    }

    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdater();
        if (mediaPlayer != null) mediaPlayer.release();
    }

    private void playNextRandomTrack() {
        if (allUrls.size() > 1) {
            String nextUrl;
            do {
                int randomIndex = new Random().nextInt(allUrls.size());
                nextUrl = allUrls.get(randomIndex);
            } while (nextUrl.equals(currentUrl));
            playNewTrack(nextUrl);
        }
    }

}