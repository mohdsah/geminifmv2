package com.radio.geminifm;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class EqualizerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("🎚 Equalizer");

        LinearLayout eqContainer =
            findViewById(R.id.eqContainer);
        SeekBar bassSeek = findViewById(R.id.bassSeek);
        TextView bassLabel = findViewById(R.id.bassLabel);
        TextView eqTitle = findViewById(R.id.eqTitle);

        try {
            android.media.audiofx.Equalizer eq =
                new android.media.audiofx.Equalizer(0, 0);
            eq.setEnabled(true);

            short bands = eq.getNumberOfBands();
            if (bands == 0) throw new Exception("No bands");

            short[] range = eq.getBandLevelRange();
            int min = range[0];
            int max = range[1];

            for (short i = 0; i < bands; i++) {
                final short band = i;

                TextView label = new TextView(this);
                label.setText(eq.getCenterFreq(band) / 1000 + " Hz");
                label.setTextColor(0xFF00FF88);
                label.setTextSize(13f);

                SeekBar seek = new SeekBar(this);
                seek.setMax(max - min);
                seek.setProgress(eq.getBandLevel(band) - min);

                LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                p.setMargins(0, 4, 0, 16);
                seek.setLayoutParams(p);

                seek.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar sb,
                            int progress, boolean fromUser) {
                        if (fromUser)
                            eq.setBandLevel(band,
                                (short)(progress + min));
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar s){}
                    @Override
                    public void onStopTrackingTouch(SeekBar s){}
                });

                eqContainer.addView(label);
                eqContainer.addView(seek);
            }

            // Bass Boost
            android.media.audiofx.BassBoost bass =
                new android.media.audiofx.BassBoost(0, 0);
            bass.setEnabled(true);
            bassSeek.setMax(1000);
            bassSeek.setProgress(bass.getRoundedStrength());

            bassSeek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar sb,
                        int progress, boolean fromUser) {
                    if (fromUser) {
                        bass.setStrength((short) progress);
                        bassLabel.setText("Bass: " + progress);
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar s) {}
                @Override
                public void onStopTrackingTouch(SeekBar s) {}
            });

        } catch (Exception e) {
            // Peranti tidak support — tunjuk mesej cantik
            eqContainer.removeAllViews();
            TextView msg = new TextView(this);
            msg.setText("⚠ Equalizer tidak disokong pada peranti ini.\n\nCuba gunakan headphone atau speaker bluetooth.");
            msg.setTextColor(0xFF00FF88);
            msg.setTextSize(15f);
            msg.setPadding(0, 32, 0, 0);
            eqContainer.addView(msg);

            bassSeek.setEnabled(false);
            bassLabel.setText("Bass Boost: Tidak tersedia");
            eqTitle.setText("🎚 Equalizer — Tidak Tersedia");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}