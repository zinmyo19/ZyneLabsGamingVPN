package com.dzinlabs.gvpn;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Ping test against well-known DNS endpoints. No root needed. */
public class NetworkCheckActivity extends Activity {
    private static final String[] HOSTS = {"1.1.1.1", "8.8.8.8", "208.67.222.222"};
    private static final String[] NAMES = {"Cloudflare", "Google", "OpenDNS"};

    private TextView log;
    private TextView testBtn;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean testing = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF151918);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("\uD83D\uDCCA NETWORK CHECK");
        title.setTextSize(20);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        title.setTextColor(0xFF68B6A5);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Ping test to major DNS endpoints.\nLower = better for gaming.");
        sub.setTextSize(13);
        sub.setTypeface(Typeface.MONOSPACE);
        sub.setTextColor(0xFFADB5AF);
        sub.setPadding(0, dp(6), 0, dp(14));
        root.addView(sub);

        testBtn = new TextView(this);
        testBtn.setText("\u25B6 RUN TEST");
        testBtn.setTextSize(16);
        testBtn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        testBtn.setTextColor(0xFF151918);
        testBtn.setBackgroundColor(0xFF68B6A5);
        testBtn.setGravity(Gravity.CENTER);
        testBtn.setPadding(0, dp(12), 0, dp(12));
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { runTest(); }
        });
        root.addView(testBtn);

        log = new TextView(this);
        log.setText("Tap RUN TEST to measure latency.\n");
        log.setTextSize(13);
        log.setTypeface(Typeface.MONOSPACE);
        log.setTextColor(0xFFF2EEE4);
        log.setPadding(0, dp(14), 0, 0);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(log);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(lp);
        root.addView(scroll);

        setContentView(root);
    }

    private void append(final String s) {
        ui.post(new Runnable() {
            @Override public void run() { log.append(s); }
        });
    }

    private void runTest() {
        if (testing) return;
        testing = true;
        ui.post(new Runnable() {
            @Override public void run() {
                log.setText("");
                testBtn.setText("\u23F3 TESTING...");
            }
        });
        new Thread(new Runnable() {
            @Override public void run() {
                double best = Double.MAX_VALUE;
                String bestName = "";
                for (int i = 0; i < HOSTS.length; i++) {
                    append("\n[" + NAMES[i] + " " + HOSTS[i] + "]\n");
                    double avg = ping(HOSTS[i]);
                    if (avg >= 0) {
                        append("  avg: " + Math.round(avg) + " ms  " + verdict(avg) + "\n");
                        if (avg < best) { best = avg; bestName = NAMES[i]; }
                    } else {
                        append("  unreachable\n");
                    }
                }
                if (best < Double.MAX_VALUE) {
                    append("\n\u2713 Best route: " + bestName + " (" + Math.round(best) + " ms)\n");
                } else {
                    append("\n\u2715 No route reachable \u2014 check connection / VPN.\n");
                }
                testing = false;
                ui.post(new Runnable() {
                    @Override public void run() { testBtn.setText("\u25B6 RUN TEST"); }
                });
            }
        }).start();
    }

    /** Returns avg ms, or -1 if unreachable. */
    private double ping(String host) {
        try {
            Process pr = Runtime.getRuntime().exec(new String[]{"ping", "-c", "4", "-W", "2", host});
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(pr.getInputStream()));
            String line, rtt = null;
            while ((line = br.readLine()) != null) {
                if (line.contains("rtt min/avg/max")) rtt = line;
                append("  " + line.trim() + "\n");
            }
            br.close();
            pr.waitFor();
            if (rtt != null) {
                // rtt min/avg/max/mdev = 10.1/12.3/15.2/1.8 ms
                String vals = rtt.substring(rtt.indexOf('=') + 1).trim().split(" ")[0];
                return Double.parseDouble(vals.split("/")[1]);
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private String verdict(double avg) {
        if (avg < 30) return "\u2014 EXCELLENT \uD83D\uDFE2";
        if (avg < 80) return "\u2014 GOOD \uD83D\uDFE2";
        if (avg < 150) return "\u2014 FAIR \uD83D\uDFE1";
        return "\u2014 POOR \uD83D\uDD34";
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
