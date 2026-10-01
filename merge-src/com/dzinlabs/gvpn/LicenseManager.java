package com.dzinlabs.gvpn;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * PRO license handling. Keys are verified against the Cloudflare Worker at
 * LICENSE_HOST, bound to ANDROID_ID (max 2 devices per key). A 7-day offline
 * grace period keeps PRO working without network.
 */
public class LicenseManager {

    public static final String LICENSE_HOST = "https://license.dzinlabs.dpdns.org";
    static final long GRACE_MS = 7L * 24 * 3600 * 1000;

    public interface Callback {
        void onResult(boolean pro, String msg);
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("gvpn", Context.MODE_PRIVATE);
    }

    public static String getKey(Context c) {
        return prefs(c).getString("license_key", "");
    }

    public static void setKey(Context c, String k) {
        prefs(c).edit().putString("license_key",
                k == null ? "" : k.trim().toUpperCase()).apply();
    }

    public static void clearKey(Context c) {
        prefs(c).edit().remove("license_key")
                .remove("pro_cached").remove("pro_ts").apply();
    }

    public static String deviceId(Context c) {
        String id = Settings.Secure.getString(c.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return id == null ? "unknown" : id;
    }

    /** Fast offline check: a key + cached PRO within the grace period. */
    public static boolean isProCached(Context c) {
        if (getKey(c).isEmpty()) return false;
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("pro_cached", false)) return false;
        return System.currentTimeMillis() - p.getLong("pro_ts", 0) < GRACE_MS;
    }

    /** Server verification on a background thread; callback on UI thread. */
    public static void verify(final Context c, final Callback cb) {
        final String key = getKey(c);
        if (key.isEmpty()) {
            cb.onResult(false, "No license key");
            return;
        }
        new Thread(() -> {
            boolean pro = false;
            String msg;
            try {
                URL url = new URL(LICENSE_HOST + "/verify");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setConnectTimeout(9000);
                con.setReadTimeout(9000);
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json");
                String body = "{\"key\":\"" + key + "\",\"device_id\":\"" + deviceId(c) + "\"}";
                try (OutputStream os = con.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
                int code = con.getResponseCode();
                InputStream in = code < 400 ? con.getInputStream() : con.getErrorStream();
                String resp = new String(readAll(in), StandardCharsets.UTF_8);
                if (resp.contains("\"ok\":true")) {
                    pro = true;
                    msg = "PRO verified";
                } else if (resp.contains("device_limit")) {
                    msg = "Device limit reached (max 2)";
                } else if (resp.contains("revoked")) {
                    msg = "Key revoked";
                } else {
                    msg = "Invalid key";
                }
            } catch (Exception e) {
                if (isProCached(c)) {
                    pro = true;
                    msg = "Offline \u2014 cached PRO";
                } else {
                    msg = "Network error";
                }
            }
            prefs(c).edit().putBoolean("pro_cached", pro)
                    .putLong("pro_ts", System.currentTimeMillis()).apply();
            final boolean fPro = pro;
            final String fMsg = msg;
            new Handler(Looper.getMainLooper()).post(() -> cb.onResult(fPro, fMsg));
        }).start();
    }

    static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        return b.toByteArray();
    }
}
