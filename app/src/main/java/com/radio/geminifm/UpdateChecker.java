package com.radio.geminifm;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

public class UpdateChecker {

    // Tukar kepada URL raw GitHub kamu
    static final String VERSION_URL =
        "https://raw.githubusercontent.com/USERNAME/REPO/main/version.json";

    Activity activity;
    Handler handler = new Handler(Looper.getMainLooper());

    public UpdateChecker(Activity activity) {
        this.activity = activity;
    }

    public void check() {
        new Thread(() -> {
            try {
                URL url = new URL(VERSION_URL);
                HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                int latestCode = json.getInt("versionCode");
                String latestName = json.getString("versionName");
                String apkUrl = json.getString("apkUrl");
                String changelog = json.getString("changelog");

                int currentCode = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0)
                    .versionCode;

                if (latestCode > currentCode) {
                    handler.post(() ->
                        showUpdateDialog(latestName,
                            apkUrl, changelog));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showUpdateDialog(String version,
            String apkUrl, String changelog) {

        if (activity.isFinishing()) return;

        new AlertDialog.Builder(activity)
            .setTitle("🆕 Update Tersedia — v" + version)
            .setMessage("Yang baru:\n" + changelog)
            .setCancelable(false)
            .setPositiveButton("Download Sekarang", (d, w) -> {
                downloadApk(apkUrl, version);
            })
            .setNegativeButton("Nanti", (d, w) -> {
                d.dismiss();
            })
            .show();
    }

    private void downloadApk(String apkUrl, String version) {
        try {
            String fileName = "GeMiniFm-v" + version + ".apk";

            DownloadManager.Request request =
                new DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle("GeMiniFm Update");
            request.setDescription("Downloading v" + version);
            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, fileName);
            request.allowScanningByMediaScanner();

            DownloadManager dm = (DownloadManager)
                activity.getSystemService(
                    Context.DOWNLOAD_SERVICE);
            long downloadId = dm.enqueue(request);

            // Tunggu download siap
            BroadcastReceiver receiver =
                new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent i) {
                    long id = i.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (id == downloadId) {
                        installApk(fileName);
                        ctx.unregisterReceiver(this);
                    }
                }
            };

            activity.registerReceiver(receiver,
                new IntentFilter(
                    DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void installApk(String fileName) {
        try {
            File file = new File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), fileName);

            Intent intent = new Intent(Intent.ACTION_VIEW);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri uri = FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName()
                        + ".provider",
                    file);
                intent.setDataAndType(uri,
                    "application/vnd.android.package-archive");
                intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(Uri.fromFile(file),
                    "application/vnd.android.package-archive");
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}