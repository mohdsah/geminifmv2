package com.radio.geminifm;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AiChatActivity extends AppCompatActivity {

    TextView chatHistory;
    EditText inputMsg;
    ImageButton sendBtn;
    ScrollView scrollView;
    StringBuilder chatLog = new StringBuilder();
    boolean isLoading = false;

    // Guna Anthropic Claude API
    // Daftar percuma: https://console.anthropic.com
    String API_KEY = "YOUR_API_KEY_HERE";
    String API_URL = "https://api.anthropic.com/v1/messages";
    String MODEL = "claude-haiku-4-5-20251001";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("🤖 AI Assistant");

        chatHistory = findViewById(R.id.chatHistory);
        inputMsg = findViewById(R.id.inputMsg);
        sendBtn = findViewById(R.id.sendBtn);
        scrollView = findViewById(R.id.scrollView);

        // Greeting awal
        appendChat("AI", "Helo! Saya AI Assistant GeMiniFm. Boleh tanya saya apa-apa tentang lagu, radio, atau apa sahaja! 🎵");

        sendBtn.setOnClickListener(v -> {
            String msg = inputMsg.getText().toString().trim();
            if (msg.isEmpty() || isLoading) return;

            inputMsg.setText("");
            appendChat("Anda", msg);
            sendToAI(msg);
        });
    }

    private void sendToAI(String userMessage) {
        isLoading = true;
        sendBtn.setEnabled(false);
        appendChat("AI", "⏳ Sedang berfikir...");

        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type",
                    "application/json");
                conn.setRequestProperty("x-api-key", API_KEY);
                conn.setRequestProperty("anthropic-version",
                    "2023-06-01");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                // Build JSON request
                JSONObject body = new JSONObject();
                body.put("model", MODEL);
                body.put("max_tokens", 500);

                JSONArray messages = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content",
                    "Kamu adalah AI Assistant untuk app radio GeMiniFm. "
                    + "Bantu pendengar dengan soalan tentang muzik, lagu, "
                    + "artis, dan radio. Jawab dalam Bahasa Malaysia. "
                    + "Soalan: " + userMessage);
                messages.put(msg);
                body.put("messages", messages);

                // Send request
                OutputStream os = conn.getOutputStream();
                os.write(body.toString()
                    .getBytes(StandardCharsets.UTF_8));
                os.close();

                // Read response
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                // Parse response
                JSONObject response =
                    new JSONObject(sb.toString());
                JSONArray content =
                    response.getJSONArray("content");
                String aiReply =
                    content.getJSONObject(0).getString("text");

                // Update UI
                runOnUiThread(() -> {
                    // Remove "sedang berfikir" message
                    removeLast();
                    appendChat("AI", aiReply);
                    isLoading = false;
                    sendBtn.setEnabled(true);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    removeLast();
                    appendChat("AI",
                        "⚠ Maaf, gagal sambung. Semak internet.");
                    isLoading = false;
                    sendBtn.setEnabled(true);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void appendChat(String sender, String message) {
        String prefix = sender.equals("AI") ? "🤖 AI: " : "👤 Anda: ";
        chatLog.append(prefix).append(message).append("\n\n");
        chatHistory.setText(chatLog.toString());

        scrollView.post(() ->
            scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void removeLast() {
        String text = chatLog.toString();
        int lastIndex = text.lastIndexOf("🤖 AI: ");
        if (lastIndex != -1) {
            chatLog = new StringBuilder(text.substring(0, lastIndex));
            chatHistory.setText(chatLog.toString());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}