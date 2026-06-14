package com.radio.geminifm;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

public class RadioWidget extends AppWidgetProvider {

    static final String ACTION_WIDGET_PLAY =
        "com.radio.geminifm.WIDGET_PLAY";
    static final String ACTION_WIDGET_STOP =
        "com.radio.geminifm.WIDGET_STOP";

    public static boolean isPlaying = false;

    @Override
    public void onUpdate(Context context,
            AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id);
        }
    }

    static void updateWidget(Context context,
            AppWidgetManager manager, int widgetId) {

        RemoteViews views = new RemoteViews(
            context.getPackageName(),
            R.layout.widget_radio);

        // Klik logo — buka app
        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openPending = PendingIntent.getActivity(
            context, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT
                | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetLogo,
            openPending);

        // Button Play/Stop
        if (isPlaying) {
            views.setImageViewResource(R.id.widgetPlayBtn,
                android.R.drawable.ic_media_pause);
            views.setTextViewText(R.id.widgetStatus,
                "● LIVE");

            Intent stopIntent = new Intent(context,
                RadioWidget.class);
            stopIntent.setAction(ACTION_WIDGET_STOP);
            PendingIntent stopPending =
                PendingIntent.getBroadcast(
                    context, 1, stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(
                R.id.widgetPlayBtn, stopPending);

        } else {
            views.setImageViewResource(R.id.widgetPlayBtn,
                android.R.drawable.ic_media_play);
            views.setTextViewText(R.id.widgetStatus,
                "OFFLINE");

            Intent playIntent = new Intent(context,
                RadioWidget.class);
            playIntent.setAction(ACTION_WIDGET_PLAY);
            PendingIntent playPending =
                PendingIntent.getBroadcast(
                    context, 2, playIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(
                R.id.widgetPlayBtn, playPending);
        }

        manager.updateAppWidget(widgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        if (action == null) return;

        if (action.equals(ACTION_WIDGET_PLAY)) {
            // Start radio
            Intent service = new Intent(context,
                RadioService.class);
            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
            isPlaying = true;

        } else if (action.equals(ACTION_WIDGET_STOP)) {
            // Stop radio
            context.stopService(
                new Intent(context, RadioService.class));
            isPlaying = false;
        }

        // Update semua widget
        AppWidgetManager manager =
            AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(
            context, RadioWidget.class);
        int[] ids = manager.getAppWidgetIds(widget);
        onUpdate(context, manager, ids);
    }

    // Update widget dari luar (MainActivity/RadioService)
    public static void updateAll(Context context,
            boolean playing) {
        isPlaying = playing;
        AppWidgetManager manager =
            AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(
            context, RadioWidget.class);
        int[] ids = manager.getAppWidgetIds(widget);
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }
}