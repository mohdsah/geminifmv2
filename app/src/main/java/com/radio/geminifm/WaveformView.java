package com.radio.geminifm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class WaveformView extends View {

    Paint paint;
    Random random;
    float[] barHeights;
    int barCount = 12;
    boolean isAnimating = false;

    Runnable animRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAnimating) {
                updateBars();
                invalidate();
                postDelayed(this, 100);
            }
        }
    };

    public WaveformView(Context context) {
        super(context);
        init();
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.parseColor("#00FFD5"));
        paint.setAntiAlias(true);
        paint.setStrokeWidth(8f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        random = new Random();
        barHeights = new float[barCount];

        for (int i = 0; i < barCount; i++) {
            barHeights[i] = 10f;
        }
    }

    private void updateBars() {
        for (int i = 0; i < barCount; i++) {
            if (isAnimating) {
                barHeights[i] = 10f + random.nextFloat() * 60f;
            } else {
                barHeights[i] = 10f;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;

        float totalBarWidth = barCount * 16f;
        float startX = (width - totalBarWidth) / 2f;

        for (int i = 0; i < barCount; i++) {
            float x = startX + i * 16f;
            float barH = barHeights[i];

            paint.setAlpha(isAnimating ? 255 : 80);

            canvas.drawLine(
                x, centerY - barH / 2,
                x, centerY + barH / 2,
                paint
            );
        }
    }

    public void startAnimation() {
        isAnimating = true;
        post(animRunnable);
    }

    public void stopAnimation() {
        isAnimating = false;
        updateBars();
        invalidate();
    }
}