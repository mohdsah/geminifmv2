package com.radio.geminifm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

public class RadioService extends Service {

    ExoPlayer exoPlayer;
    Handler handler;
    Handler songHandler;
    boolean isPlaying = false;
    String currentSong = "";
    String currentArtist = "";
    Bitmap currentArtwork = null;
    int retryCount = 0;
    int maxRetry = 10;

    String RADIO_URL = "http://eu8.fastcast4u.com:23024/stream/1/";
    String STATUS_URL = "http://eu8.fastcast4u.com:23024/status-json.xsl";
    String CHANNEL_ID = "radio_channel";

    static final String ACTION_PLAY = "ACTION_PLAY";
    static final String ACTION_PAUSE = "ACTION_PAUSE";
    static final String ACTION_STOP = "ACTION_STOP";

    public class RadioBinder extends Binder {
        RadioService getService() {
            return RadioService.this;
        }
    }

    IBinder binder = new RadioBinder();

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case ACTION_PLAY:
                    resumeRadio();
                    break;
                case ACTION_PAUSE:
                    pauseRadio();
                    break;
                case ACTION_STOP:
                    stopSelf();
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        songHandler = new Handler(Looper.getMainLooper());
        createChannel();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_STOP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter,
                Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification(
            "Connecting...", false, null));
        initExoPlayer();
        startRadio();
        return START_STICKY;
    }

    private void initExoPlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }

        exoPlayer = new ExoPlayer.Builder(this).build();

        exoPlayer.addListener(new Player.Listener() {

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    isPlaying = true;
                    retryCount = 0;
                    updateNotification("▶ LIVE", true);
                    startSongUpdater();
                    sendStatus("PLAYING", currentSong,
                        currentArtist, null);

                } else if (state == Player.STATE_BUFFERING) {
                    updateNotification("Buffering...", false);
                    sendStatus("BUFFERING", "", "", null);

                } else if (state == Player.STATE_ENDED) {
                    retryConnection();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                isPlaying = false;
                stopSongUpdater();
                updateNotification("⚠ Reconnecting...", false);
                sendStatus("ERROR", "", "", null);
                retryConnection();
            }

            @Override
            public void onIsPlayingChanged(boolean playing) {
                isPlaying = playing;
                if (playing) {
                    updateNotification("▶ LIVE", true);
                    sendStatus("PLAYING", currentSong,
                        currentArtist, currentArtwork);
                } else {
                    updateNotification("⏸ Paused", false);
                    sendStatus("PAUSED", "", "", null);
                }
            }
        });
    }

    private void startRadio() {
        try {
            MediaItem mediaItem = MediaItem.fromUri(RADIO_URL);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.setPlayWhenReady(true);
        } catch (Exception e) {
            updateNotification("⚠ Connection Error", false);
            e.printStackTrace();
        }
    }

    public void pauseRadio() {
        if (exoPlayer != null && exoPlayer.isPlaying()) {
            exoPlayer.pause();
            stopSongUpdater();
        }
    }

    public void resumeRadio() {
        if (exoPlayer != null && !exoPlayer.isPlaying()) {
            exoPlayer.play();
            startSongUpdater();
        }
    }

    private void retryConnection() {
        if (retryCount >= maxRetry) {
            updateNotification("⚠ Stream Offline", false);
            sendStatus("OFFLINE", "", "", null);
            return;
        }
        retryCount++;
        int delay = Math.min(3000 * retryCount, 30000);
        updateNotification("⚠ Retry " + retryCount
            + "/" + maxRetry + "...", false);

        handler.postDelayed(() -> {
            initExoPlayer();
            startRadio();
        }, delay);
    }

    private void startSongUpdater() {
        songHandler.post(songRunnable);
    }

    private void stopSongUpdater() {
        songHandler.removeCallbacks(songRunnable);
    }

    Runnable songRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                fetchMetadata();
                songHandler.postDelayed(this, 10000);
            }
        }
    };

    private void fetchMetadata() {
        MetadataFetcher fetcher = new MetadataFetcher(
            STATUS_URL,
            (title, artist, artwork) -> {
                boolean changed = !title.equals(currentSong)
                    || !artist.equals(currentArtist);

                currentSong = title;
                currentArtist = artist;
                if (artwork != null) currentArtwork = artwork;

                if (changed) {
                    String notifText = artist.isEmpty()
                        ? "▶ LIVE | " + title
                        : "▶ " + artist + " - " + title;

                    updateNotification(notifText, true);
                    sendStatus("PLAYING", currentSong,
                        currentArtist, currentArtwork);
                }
            }
        );
        fetcher.fetch();
    }

    private void sendStatus(String status, String song,
            String artist, Bitmap artwork) {
        Intent i = new Intent("RADIO_STATUS");
        i.putExtra("status", status);
        i.putExtra("song", song);
        i.putExtra("artist", artist);
        sendBroadcast(i);

        MainActivity.currentArtwork = artwork;
    }

    private Notification buildNotification(String text,
            boolean playing, Bitmap artwork) {

        Intent openApp = new Intent(this, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPending = PendingIntent.getActivity(
            this, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT
                | PendingIntent.FLAG_IMMUTABLE);

        Intent playIntent = new Intent(ACTION_PLAY);
        PendingIntent playPending = PendingIntent.getBroadcast(
            this, 1, playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
                | PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent = new Intent(ACTION_PAUSE);
        PendingIntent pausePending = PendingIntent.getBroadcast(
            this, 2, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
                | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getBroadcast(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
                | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
            new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("📻 GeMiniFm")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(openPending)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        if (artwork != null) {
            builder.setLargeIcon(artwork);
        }

        if (playing) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Pause", pausePending);
        } else {
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Play", playPending);
        }

        builder.addAction(
            android.R.drawable.ic_delete,
            "Stop", stopPending);

        return builder.build();
    }

    private void updateNotification(String text, boolean playing) {
        NotificationManager manager =
            (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        manager.notify(1, buildNotification(
            text, playing, currentArtwork));
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Radio Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("GeMiniFm Radio");
            NotificationManager manager =
                getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        try {
            handler.removeCallbacksAndMessages(null);
            stopSongUpdater();
            unregisterReceiver(receiver);

            if (exoPlayer != null) {
                exoPlayer.stop();
                exoPlayer.release();
                exoPlayer = null;
            }

            sendStatus("STOPPED", "", "", null);

        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}