package com.radio.geminifm;

import android.app.Dialog;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SleepTimerDialog {

    public interface SleepTimerListener {
        void onTimerFinished();
    }

    Context context;
    CountDownTimer countDownTimer;
    SleepTimerListener listener;
    boolean isRunning = false;

    public SleepTimerDialog(Context context,
            SleepTimerListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void show() {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_sleep_timer);
        dialog.getWindow().setBackgroundDrawableResource(
            android.R.color.transparent);

        TextView timerText = dialog.findViewById(R.id.timerText);
        Button btn15 = dialog.findViewById(R.id.btn15);
        Button btn30 = dialog.findViewById(R.id.btn30);
        Button btn60 = dialog.findViewById(R.id.btn60);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        btn15.setOnClickListener(v -> {
            startTimer(15 * 60 * 1000, timerText);
            dialog.dismiss();
        });

        btn30.setOnClickListener(v -> {
            startTimer(30 * 60 * 1000, timerText);
            dialog.dismiss();
        });

        btn60.setOnClickListener(v -> {
            startTimer(60 * 60 * 1000, timerText);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            cancelTimer();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void startTimer(long millis, TextView timerText) {
        cancelTimer();
        isRunning = true;

        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                if (timerText != null) {
                    timerText.setText(
                        String.format("⏱ %02d:%02d", minutes, seconds));
                }
            }

            @Override
            public void onFinish() {
                isRunning = false;
                if (listener != null) {
                    listener.onTimerFinished();
                }
            }
        }.start();
    }

    public void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }
}