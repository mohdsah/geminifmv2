package com.radio.geminifm;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    public static Bitmap currentArtwork = null;

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    ImageButton playBtn;
    ImageView btnGlow;
    ImageView logoImg;
    TextView statusText;
    TextView songText;
    TextView artistText;
    WaveformView waveformView;
    AnimatorSet glowAnim;
    String radioState = "STOPPED";
    SleepTimerDialog sleepTimerDialog;
    UpdateChecker updateChecker;

    ActivityResultLauncher<String> notifPermission =
        registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {}
        );

    BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");
            String song = intent.getStringExtra("song");
            String artist = intent.getStringExtra("artist");
            if (status == null) return;

            radioState = status;

            switch (status) {
                case "PLAYING":
                    playBtn.setImageResource(
                        android.R.drawable.ic_media_pause);
                    statusText.setText("● LIVE");
                    waveformView.startAnimation();
                    startGlowAnim();

                    if (song != null && !song.isEmpty()) {
                        songText.setText("🎵 " + song);
                    }
                    if (artist != null && !artist.isEmpty()) {
                        artistText.setText(artist);
                    }
                    if (currentArtwork != null) {
                        logoImg.setImageBitmap(currentArtwork);
                        logoImg.setScaleType(
                            ImageView.ScaleType.CENTER_CROP);
                    }
                    // Update widget
                    RadioWidget.updateAll(context, true);
                    break;

                case "PAUSED":
                    playBtn.setImageResource(
                        android.R.drawable.ic_media_play);
                    statusText.setText("⏸ Paused");
                    waveformView.stopAnimation();
                    stopGlowAnim();
                    RadioWidget.updateAll(context, false);
                    break;

                case "BUFFERING":
                    statusText.setText("⏳ Buffering...");
                    break;

                case "ERROR":
                case "OFFLINE":
                    playBtn.setImageResource(
                        android.R.drawable.ic_media_play);
                    statusText.setText("⚠ Reconnecting...");
                    waveformView.stopAnimation();
                    stopGlowAnim();
                    RadioWidget.updateAll(context, false);
                    break;

                case "STOPPED":
                    playBtn.setImageResource(
                        android.R.drawable.ic_media_play);
                    statusText.setText("OFFLINE");
                    songText.setText("");
                    artistText.setText("");
                    waveformView.stopAnimation();
                    stopGlowAnim();
                    logoImg.setImageResource(
                        R.mipmap.ic_launcher);
                    currentArtwork = null;
                    RadioWidget.updateAll(context, false);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        playBtn = findViewById(R.id.playBtn);
        btnGlow = findViewById(R.id.btnGlow);
        logoImg = findViewById(R.id.logoImg);
        statusText = findViewById(R.id.statusText);
        songText = findViewById(R.id.songText);
        artistText = findViewById(R.id.artistText);
        waveformView = findViewById(R.id.waveformView);

        // Init Sleep Timer
        sleepTimerDialog = new SleepTimerDialog(this, () -> {
            stopService(new Intent(this, RadioService.class));
            waveformView.stopAnimation();
            stopGlowAnim();
            RadioWidget.updateAll(this, false);
            Toast.makeText(this,
                "⏰ Radio dihentikan oleh Sleep Timer",
                Toast.LENGTH_LONG).show();
        });

        // Auto Update check
        updateChecker = new UpdateChecker(this);
        updateChecker.check();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.open, R.string.close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        requestNotifPermission();

        IntentFilter filter = new IntentFilter("RADIO_STATUS");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter,
                Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                drawerLayout.closeDrawer(GravityCompat.START);

            } else if (id == R.id.nav_about) {
                startActivity(
                    new Intent(this, AboutActivity.class));

            } else if (id == R.id.nav_ai) {
                startActivity(
                    new Intent(this, AiChatActivity.class));

            } else if (id == R.id.nav_equalizer) {
                startActivity(
                    new Intent(this, EqualizerActivity.class));

            } else if (id == R.id.nav_sleep) {
                sleepTimerDialog.show();

            } else if (id == R.id.nav_call) {
                Toast.makeText(this, "Call Us",
                    Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_email) {
                Toast.makeText(this, "E-mail",
                    Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_website) {
                Toast.makeText(this, "Website",
                    Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_facebook) {
                Toast.makeText(this, "Facebook",
                    Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_instagram) {
                Toast.makeText(this, "Instagram",
                    Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_share) {
                shareApp();

            } else if (id == R.id.nav_rate) {
                Toast.makeText(this, "Rate App",
                    Toast.LENGTH_SHORT).show();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Play button — single player
        playBtn.setOnClickListener(v -> {
            try {
                switch (radioState) {
                    case "STOPPED":
                    case "OFFLINE":
                    case "ERROR":
                        startRadioService();
                        statusText.setText("Connecting...");
                        songText.setText("");
                        artistText.setText("");
                        radioState = "BUFFERING";
                        playBtn.setImageResource(
                            android.R.drawable.ic_media_pause);
                        break;

                    case "PLAYING":
                        sendBroadcast(new Intent(
                            RadioService.ACTION_PAUSE));
                        break;

                    case "PAUSED":
                        sendBroadcast(new Intent(
                            RadioService.ACTION_PLAY));
                        break;

                    case "BUFFERING":
                        Toast.makeText(this,
                            "⏳ Sedang connecting...",
                            Toast.LENGTH_SHORT).show();
                        break;
                }
            } catch (Exception e) {
                statusText.setText("Error!");
            }
        });
    }

    private void requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPermission.launch(
                    Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void startRadioService() {
        stopService(new Intent(this, RadioService.class));
        Intent i = new Intent(this, RadioService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
    }

    private void startGlowAnim() {
        btnGlow.setVisibility(View.VISIBLE);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(
            btnGlow, "scaleX", 1f, 1.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(
            btnGlow, "scaleY", 1f, 1.3f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(
            btnGlow, "alpha", 0.8f, 0.2f, 0.8f);

        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setRepeatMode(ObjectAnimator.REVERSE);
        scaleY.setRepeatMode(ObjectAnimator.REVERSE);
        alpha.setRepeatMode(ObjectAnimator.REVERSE);
        scaleX.setDuration(1500);
        scaleY.setDuration(1500);
        alpha.setDuration(1500);

        glowAnim = new AnimatorSet();
        glowAnim.playTogether(scaleX, scaleY, alpha);
        glowAnim.setInterpolator(
            new AccelerateDecelerateInterpolator());
        glowAnim.start();

        ObjectAnimator logoPulse = ObjectAnimator.ofFloat(
            logoImg, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator logoPulseY = ObjectAnimator.ofFloat(
            logoImg, "scaleY", 1f, 1.05f, 1f);
        logoPulse.setRepeatCount(ObjectAnimator.INFINITE);
        logoPulseY.setRepeatCount(ObjectAnimator.INFINITE);
        logoPulse.setRepeatMode(ObjectAnimator.REVERSE);
        logoPulseY.setRepeatMode(ObjectAnimator.REVERSE);
        logoPulse.setDuration(1000);
        logoPulseY.setDuration(1000);
        logoPulse.start();
        logoPulseY.start();
    }

    private void stopGlowAnim() {
        btnGlow.setVisibility(View.GONE);
        if (glowAnim != null) {
            glowAnim.cancel();
            glowAnim = null;
        }
        logoImg.setScaleX(1f);
        logoImg.setScaleY(1f);
    }

    private void shareApp() {
        String songInfo = songText.getText().toString();
        String shareText;

        if (!songInfo.isEmpty()) {
            shareText = "Saya sedang mendengar "
                + songInfo
                + " di GeMiniFm! 🎵\n"
                + "Download: https://play.google.com";
        } else {
            shareText =
                "Dengar GeMiniFm - Radio Online Terbaik! 📻\n"
                + "Download: https://play.google.com";
        }

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, shareText);
        share.setPackage("com.whatsapp");

        try {
            startActivity(share);
        } catch (Exception e) {
            share.setPackage(null);
            startActivity(
                Intent.createChooser(share, "Share via"));
        }
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(statusReceiver);
            stopGlowAnim();
            if (sleepTimerDialog != null) {
                sleepTimerDialog.cancelTimer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}