package com.dzinlabs.gvpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.booster.shizuku.ShizukuBooster;

/**
 * Floating game HUD: FPS (via Shizuku dumpsys gfxinfo), ping, battery temp.
 * Draggable overlay, tap the X to close.
 */
public class HudService extends Service {
    public static volatile boolean isRunning = false;

    private WindowManager wm;
    private LinearLayout hud;
    private WindowManager.LayoutParams params;
    private TextView fpsView, pingView, tempView;
    private volatile boolean workerOn = false;
    private Thread worker;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private String gamePkg;
    private long lastFrames = -1;
    private long lastSampleNs = 0;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        startFg();
        buildOverlay();
        gamePkg = getSharedPreferences("gvpn", MODE_PRIVATE).getString("last_game_pkg", null);
        startWorker();
    }

    private void startFg() {
        String ch = "hud_channel";
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(ch, "Game HUD", NotificationManager.IMPORTANCE_LOW));
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, ch) : new Notification.Builder(this);
        b.setContentTitle("Game HUD active")
                .setContentText("Drag to move \u2022 tap \u2715 to close")
                .setSmallIcon(android.R.drawable.ic_dialog_info);
        Notification n = b.build();
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(2, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(2, n);
    }

    private TextView monoRow(LinearLayout parent, String label, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView l = new TextView(this);
        l.setText(label + " ");
        l.setTextSize(8);
        l.setTextColor(0xFFADB5AF);
        l.setTypeface(Typeface.MONOSPACE);
        TextView v = new TextView(this);
        v.setText("--");
        v.setTextSize(10);
        v.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        v.setTextColor(color);
        row.addView(l);
        row.addView(v);
        parent.addView(row);
        return v;
    }

    private void buildOverlay() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        hud = new LinearLayout(this);
        hud.setOrientation(LinearLayout.VERTICAL);
        hud.setBackgroundColor(0x59151918);
        int pad = dp(5);
        hud.setPadding(pad, pad / 2, pad, pad);

        TextView close = new TextView(this);
        close.setText("\u2715  HUD");
        close.setTextSize(8);
        close.setTextColor(0xFFE16A5E);
        close.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        close.setGravity(Gravity.END);
        close.setPadding(0, 0, dp(2), dp(2));
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { stopSelf(); }
        });
        hud.addView(close);

        fpsView = monoRow(hud, "FPS ", 0xFF68B6A5);
        pingView = monoRow(hud, "PING", 0xFF68B6A5);
        tempView = monoRow(hud, "TEMP", 0xFFDDB461);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= 26
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 24;
        params.y = 240;

        final int[] d = new int[4];
        hud.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        d[0] = params.x; d[1] = params.y;
                        d[2] = (int) e.getRawX(); d[3] = (int) e.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = d[0] + (int) e.getRawX() - d[2];
                        params.y = d[1] + (int) e.getRawY() - d[3];
                        wm.updateViewLayout(hud, params);
                        return true;
                }
                return false;
            }
        });
        wm.addView(hud, params);
    }

    private void startWorker() {
        workerOn = true;
        worker = new Thread(new Runnable() {
            @Override public void run() {
                while (workerOn) {
                    final String fps = readFps();
                    final String ping = readPing();
                    final String temp = readTemp();
                    ui.post(new Runnable() {
                        @Override public void run() {
                            if (fpsView != null) fpsView.setText(fps);
                            if (pingView != null) pingView.setText(ping);
                            if (tempView != null) tempView.setText(temp);
                        }
                    });
                    try { Thread.sleep(2000); } catch (InterruptedException e) { break; }
                }
            }
        });
        worker.start();
    }

    private String readFps() {
        try {
            if (gamePkg == null || !ShizukuBooster.INSTANCE.isShizukuAvailable()
                    || !ShizukuBooster.INSTANCE.hasShizukuPermission()) return "--";
            String out = ShizukuBooster.INSTANCE.runCommand("dumpsys gfxinfo " + gamePkg);
            long frames = -1;
            for (String line : out.split("\n")) {
                line = line.trim();
                if (line.startsWith("Total frames rendered:")) {
                    String num = line.substring(line.indexOf(':') + 1).trim().split(" ")[0];
                    frames = Long.parseLong(num);
                    break;
                }
            }
            long now = System.nanoTime();
            String res = "--";
            if (frames >= 0 && lastFrames >= 0 && now > lastSampleNs) {
                double dt = (now - lastSampleNs) / 1e9;
                if (dt >= 1.0) res = String.valueOf(Math.max(0, Math.round((frames - lastFrames) / dt)));
            }
            if (frames >= 0) { lastFrames = frames; lastSampleNs = now; }
            return res;
        } catch (Exception e) { return "--"; }
    }

    private String readPing() {
        try {
            Process pr = Runtime.getRuntime().exec(new String[]{"ping", "-c", "1", "-W", "2", "1.1.1.1"});
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(pr.getInputStream()));
            String line, res = "--";
            while ((line = br.readLine()) != null) {
                int ti = line.indexOf("time=");
                if (ti >= 0) {
                    String t = line.substring(ti + 5).split(" ")[0];
                    res = Math.round(Double.parseDouble(t)) + "ms";
                }
            }
            br.close();
            pr.waitFor();
            return res;
        } catch (Exception e) { return "--"; }
    }

    private String readTemp() {
        try {
            android.content.Intent bi = registerReceiver(null,
                    new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
            int t = bi != null ? bi.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) : 0;
            return String.format(java.util.Locale.US, "%.1f\u00B0", t / 10.0);
        } catch (Exception e) { return "--"; }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        workerOn = false;
        if (worker != null) worker.interrupt();
        try { if (hud != null) wm.removeView(hud); } catch (Exception ignored) {}
        hud = null;
        isRunning = false;
        super.onDestroy();
    }
}
