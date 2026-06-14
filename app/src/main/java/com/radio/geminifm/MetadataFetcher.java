package com.radio.geminifm;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MetadataFetcher {

    public interface MetadataListener {
        void onMetadataReceived(String title, String artist, Bitmap artwork);
    }

    Handler handler = new Handler(Looper.getMainLooper());
    MetadataListener listener;
    String statusUrl;

    public MetadataFetcher(String statusUrl, MetadataListener listener) {
        this.statusUrl = statusUrl;
        this.listener = listener;
    }

    public void fetch() {
        new Thread(() -> {
            try {
                // Fetch JSON status
                URL url = new URL(statusUrl);
                HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
                );

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                String json = sb.toString();
                String title = parseField(json, "title", "song",
                    "songtitle");
                String artist = parseField(json, "artist",
                    "songtitle", "dj");

                // Fetch artwork dari iTunes API
                Bitmap artwork = null;
                if (!title.isEmpty()) {
                    artwork = fetchArtwork(title, artist);
                }

                final String finalTitle = title;
                final String finalArtist = artist;
                final Bitmap finalArt = artwork;

                handler.post(() -> {
                    if (listener != null) {
                        listener.onMetadataReceived(
                            finalTitle, finalArtist, finalArt);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String parseField(String json, String... keys) {
        for (String key : keys) {
            String fullKey = "\"" + key + "\":\"";
            int start = json.indexOf(fullKey);
            if (start != -1) {
                start += fullKey.length();
                int end = json.indexOf("\"", start);
                if (end != -1) {
                    String val = json.substring(start, end).trim();
                    if (!val.isEmpty()) return val;
                }
            }
        }
        return "";
    }

    private Bitmap fetchArtwork(String title, String artist) {
        try {
            String query = (artist + " " + title)
                .replace(" ", "+")
                .replace("&", "");

            String itunesUrl =
                "https://itunes.apple.com/search?term="
                + query
                + "&media=music&limit=1";

            URL url = new URL(itunesUrl);
            HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            );

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            String json = sb.toString();
            String artKey = "\"artworkUrl100\":\"";
            int start = json.indexOf(artKey);

            if (start != -1) {
                start += artKey.length();
                int end = json.indexOf("\"", start);
                if (end != -1) {
                    String artUrl = json.substring(start, end)
                        .replace("\\/", "/");

                    // Download artwork
                    URL imgUrl = new URL(artUrl);
                    HttpURLConnection imgConn =
                        (HttpURLConnection) imgUrl.openConnection();
                    imgConn.setConnectTimeout(5000);

                    InputStream is = imgConn.getInputStream();
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();
                    return bmp;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}