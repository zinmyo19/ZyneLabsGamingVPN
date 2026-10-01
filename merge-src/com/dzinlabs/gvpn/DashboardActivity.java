package com.dzinlabs.gvpn;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.booster.shizuku.ShizukuBooster;

import java.util.Locale;

/** Device readiness dashboard: RAM, storage, temp, refresh rate + score. */
public class DashboardActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF151918);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("\uD83D\uDCCA DEVICE READINESS");
        title.setTextSize(20);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        title.setTextColor(0xFF68B6A5);
        root.addView(title);
        root.addView(spacer(12));

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(lp);
        root.addView(scroll);

        // --- collect stats ---
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        double freeGb = mi.availMem / 1e9;
        double totalGb = mi.totalMem / 1e9;

        StatFs sf = new StatFs(Environment.getDataDirectory().getPath());
        double freeSt = sf.getAvailableBytes() / 1e9;
        double totalSt = sf.getTotalBytes() / 1e9;

        android.content.Intent bi = registerReceiver(null,
                new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
        double tempC = (bi != null ? bi.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) : 0) / 10.0;

        float hz;
        try { hz = getWindowManager().getDefaultDisplay().getRefreshRate(); }
        catch (Exception e) { hz = 60f; }

        boolean shizuku = ShizukuBooster.INSTANCE.isShizukuAvailable()
                && ShizukuBooster.INSTANCE.hasShizukuPermission();

        row(body, "Device", Build.MANUFACTURER + " " + Build.MODEL);
        row(body, "Android", Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
        row(body, "Free RAM", String.format(Locale.US, "%.1f / %.1f GB", freeGb, totalGb));
        row(body, "Free storage", String.format(Locale.US, "%.1f / %.1f GB", freeSt, totalSt));
        row(body, "Battery temp", String.format(Locale.US, "%.1f\u00B0C", tempC));
        row(body, "Display", Math.round(hz) + " Hz");
        row(body, "Shizuku", shizuku ? "READY \uD83D\uDFE2" : "NOT READY \uD83D\uDD34");

        // --- score ---
        int score = 0;
        if (freeGb >= 2) score += 30; else if (freeGb >= 1) score += 15;
        if (tempC < 40) score += 25; else if (tempC < 45) score += 12;
        if (freeSt >= 5) score += 20; else if (freeSt >= 2) score += 10;
        if (shizuku) score += 15;
        if (hz >= 90) score += 10;

        root.addView(spacer(12));
        TextView scoreV = new TextView(this);
        scoreV.setText("READINESS: " + score + " / 100");
        scoreV.setTextSize(22);
        scoreV.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        scoreV.setGravity(Gravity.CENTER);
        scoreV.setTextColor(score >= 80 ? 0xFF68B6A5 : (score >= 50 ? 0xFFDDB461 : 0xFFE16A5E));
        root.addView(scoreV);

        TextView hint = new TextView(this);
        hint.setText(score >= 80 ? "Your phone is game-ready. \uD83D\uDE0E"
                : (score >= 50 ? "Decent \u2014 run a boost before ranked matches."
                : "Run OPTIMIZE before playing!"));
        hint.setTextSize(13);
        hint.setTypeface(Typeface.MONOSPACE);
        hint.setTextColor(0xFFADB5AF);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(6), 0, dp(10));
        root.addView(hint);

        TextView optBtn = new TextView(this);
        optBtn.setText("\u26A1 OPTIMIZE NOW");
        optBtn.setTextSize(16);
        optBtn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        optBtn.setTextColor(0xFF151918);
        optBtn.setBackgroundColor(0xFF68B6A5);
        optBtn.setGravity(Gravity.CENTER);
        optBtn.setPadding(0, dp(12), 0, dp(12));
        optBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!shizuku) {
                    Toast.makeText(DashboardActivity.this,
                            "Shizuku not ready", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(DashboardActivity.this,
                        "Optimizing...", Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override public void run() {
                        try {
                            ShizukuBooster.INSTANCE.boostDevice(
                                    DashboardActivity.this,
                                    new kotlin.jvm.functions.Function1<String, kotlin.Unit>() {
                                        @Override public kotlin.Unit invoke(String s) {
                                            return kotlin.Unit.INSTANCE;
                                        }
                                    }, false);
                        } catch (Exception ignored) {}
                    }
                }).start();
            }
        });
        root.addView(optBtn);

        setContentView(root);
    }

    private void row(LinearLayout parent, String k, String v) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, dp(5), 0, dp(5));
        TextView kk = new TextView(this);
        kk.setText(k);
        kk.setTextSize(13);
        kk.setTypeface(Typeface.MONOSPACE);
        kk.setTextColor(0xFFADB5AF);
        LinearLayout.LayoutParams lpp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        kk.setLayoutParams(lpp);
        TextView vv = new TextView(this);
        vv.setText(v);
        vv.setTextSize(13);
        vv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        vv.setTextColor(0xFFF2EEE4);
        vv.setGravity(Gravity.END);
        LinearLayout.LayoutParams lpv = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f);
        vv.setLayoutParams(lpv);
        r.addView(kk);
        r.addView(vv);
        parent.addView(r);
        View div = new View(this);
        div.setBackgroundColor(0xFF202624);
        div.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        parent.addView(div);
    }

    private View spacer(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(h)));
        return v;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
