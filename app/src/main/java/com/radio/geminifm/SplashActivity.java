package com.radio.geminifm;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Wajib panggil SEBELUM super.onCreate
        SplashScreen splashScreen =
            SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // Tahan splash screen selama 2 saat
        long startTime = System.currentTimeMillis();
        splashScreen.setKeepOnScreenCondition(() ->
            System.currentTimeMillis() - startTime < 2000
        );

        // Lepas 2 saat, pergi MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(
                new Intent(this, MainActivity.class));
            finish();
        }, 2000);
    }
}