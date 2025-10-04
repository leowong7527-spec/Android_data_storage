package com.example.datadisplay;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
        elapsedTimeText = findViewById(R.id.elapsedTimeText);
        totalTimeText = findViewById(R.id.totalTimeText);
        playPauseButton = findViewById(R.id.playPauseButton);
        ImageButton loopButton = findViewById(R.id.loopButton);
        ImageButton shuffleButton = findViewById(R.id.shuffleButton);
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
                totalTimeText.setText(formatTime(durationMs));
                progressBar.setMax(durationMs);
                mp.start();
                isPlaying = true;
                playPauseButton.setImageResource(R.drawable.ic_pause);
                startProgressUpdater();

                // Completion listener for first track
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

        loopButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        shuffleButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        loopButton.setOnClickListener(v -> {
            isLooping = !isLooping;

            if (isLooping) {
                // Turn off shuffle if it was on
                isShuffling = false;
                shuffleButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                mediaPlayer.setLooping(true);
                loopButton.setBackgroundResource(R.drawable.bg_oval_highlight);
            } else {
                mediaPlayer.setLooping(false);
                loopButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            }
        });

        shuffleButton.setOnClickListener(v -> {
            isShuffling = !isShuffling;

            if (isShuffling) {
                // Turn off loop if it was on
                isLooping = false;
                mediaPlayer.setLooping(false);
                loopButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                shuffleButton.setBackgroundResource(R.drawable.bg_oval_highlight);
            } else {
                shuffleButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
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
        try {
            filename = URLDecoder.decode(filename, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            // ignore decoding errors
        }
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
        if (allUrls != null && allUrls.size() > 1) {
            String nextUrl;
            do {
                int randomIndex = new Random().nextInt(allUrls.size());
                nextUrl = allUrls.get(randomIndex);
            } while (nextUrl.equals(currentUrl)); // avoid repeating the same track

            playNewTrack(nextUrl);
        } else if (allUrls != null && allUrls.size() == 1) {
            playNewTrack(allUrls.get(0));
        }
    }
}