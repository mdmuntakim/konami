package com.konami.konamiid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Handle incoming deep links from Konami ID / MyKonami URLs
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            String scheme = intent.getData().getScheme();
            String host = intent.getData().getHost();
            Log.d("DeepLink", "Received deep link: scheme=" + scheme + ", host=" + host);

            // Display which Konami link triggered the app launch
            Toast.makeText(this, "Konami link received: " + host, Toast.LENGTH_LONG).show();

            // TODO: Extract session token from deep link data and handle login handoff
            // The session ID can be passed via query parameters, e.g.:
            // konamiid://mykonami-login?session_token=XYZ123
        }
    }

    /**
     * Handle the Konami ID login flow.
     * Prompts for email/password, authenticates with Konami ID API,
     * saves the session ID locally, and sends it to eFootball (jp.konami.pesam).
     */
    public void handleKonamiIdLogin(String email, String password) {
        // TODO: Implement actual Konami ID authentication
        // This would involve:
        // 1. Making HTTPS request to Konami ID auth endpoint with email/password
        // 2. Receiving session ID / auth token in response
        // 3. Saving session ID to SharedPreferences or external storage
        // 4. Sending session ID to eFootapp (jp.konami.pesam) via intent

        // For now, simulate a successful auth with a generated session ID
        String sessionId = generateSessionId();
        saveSessionIdLocally(sessionId);

        // Launch eFootball intent with the session ID handoff
        handoffSessionToEfootball(sessionId);
    }

    private String generateSessionId() {
        // Generate a unique session identifier
        return "KONAMI_SESSION_" + System.currentTimeMillis();
    }

    private void saveSessionIdLocally(String sessionId) {
        // Save to internal storage so it persists even if app is reinstalled
        // Also save to external shared prefs for account switch support
        android.content.SharedPreferences prefs = android.content.SharedPreferences
                .getDefaultSharedPreferences(this);
        prefs.edit().putString("konami_session_id", sessionId).apply();

        // Also write to external storage for persistence across installs
        // (as requested: save session id for later usable even if APK is deleted)
        try {
            java.io.File extFile = new java.io.File(
                    android.os.Environment.getExternalStorageDirectory(),
                    "konami_session.txt"
            );
            extFile.getParentFile().mkdirs();
            java.io.FileWriter writer = new java.io.FileWriter(extFile);
            writer.write(sessionId);
            writer.close();
        } catch (Exception e) {
            Log.w("SessionSave", "Could not write session to external storage", e);
        }
    }

    private void handoffSessionToEfootball(String sessionId) {
        // Send the session ID to eFootball (jp.konami.pesam) to complete login handoff
        Intent intent = new Intent();
        intent.setPackage("jp.konami.pesam");
        intent.setAction("android.intent.action.MAIN");
        intent.putExtra("konami_session_id", sessionId);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e("EfootballHandoff", "eFootball app not found, cannot handoff session", e);
            Toast.makeText(this, "Session saved. eFootball app not installed.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Account switch support: load saved session IDs and let user select one.
     * Returns list of saved session IDs.
     */
    public String[] loadSavedSessionIds() {
        android.content.SharedPreferences prefs = android.content.SharedPreferences
                .getDefaultSharedPreferences(this);
        String savedSession = prefs.getString("konami_session_id", null);

        // Also check external storage
        String externalSession = null;
        try {
            java.io.File extFile = new java.io.File(
                    android.os.Environment.getExternalStorageDirectory(),
                    "konami_session.txt"
            );
            if (extFile.exists()) {
                externalSession = java.io.FileUtils.readFileToString(extFile);
            }
        } catch (Exception e) {
            // ignore
        }

        if (savedSession != null && externalSession != null) {
            return new String[]{savedSession, externalSession};
        } else if (savedSession != null) {
            return new String[]{savedSession};
        } else if (externalSession != null) {
            return new String[]{externalSession};
        }
        return new String[0];
    }
}