package com.dzinlabs.gvpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.booster.shizuku.BoostBubbleService;
import com.booster.shizuku.Prefs;
import com.booster.shizuku.ScriptEntry;
import com.booster.shizuku.ScriptStore;
import com.booster.shizuku.ShizukuBooster;

import java.util.List;

import rikka.shizuku.Shizuku;

/**
 * Launcher hub — calm dark theme (Chinese Learn palette). Game tiles (1-tap boost + launch),
 * quick boost, floating bubble, HUD, scripts, VPN, network info,
 * settings and about.
 */
public class GameHubActivity extends Activity {

    // Calm palette (matches Chinese Learn app)
    static final int BG = 0xFF151918;
    static final int CARD = 0xFF202624;
    static final int YELLOW = 0xFFDDB461;
    static final int CYAN = 0xFF68B6A5;
    static final int MAGENTA = 0xFFE16A5E;
    static final int ACCENT = CYAN;
    static final int TEXT = 0xFFF2EEE4;
    static final int DIM = 0xFFADB5AF;
    static final int AMBER = 0xFFDDB461;

    static final int REQ_SHIZUKU = 1001;
    static final int REQ_OVERLAY = 1002;

    // Display name + candidate package names (first installed wins).
    static final String[][] GAMES = {
            {"eFootball", "jp.konami.pesam"},
            {"PUBG Mobile", "com.tencent.ig"},
            {"MLBB", "com.mobile.legends"},
            {"Free Fire", "com.dts.freefireth", "com.dts.freefiremax"},
            {"COD Mobile", "com.activision.callofduty.shooter"},
            {"Genshin Impact", "com.miHoYo.GenshinImpact"},
            {"FC Mobile", "com.ea.gp.fifamobile"},
            {"Roblox", "com.roblox.client"},
    };

    // Game tile icons (emoji glyphs — original artwork, no trademarked logos).
    static final String[] GAME_ICONS = {
            "⚽", "\uD83E\uDE82", "⚔️", "\uD83D\uDD25",
            "\uD83C\uDFAF", "✨", "\uD83C\uDFC6", "\uD83D\uDFE5",
    };

    // Game package -> auto-run script on launch.
    static final String[][] GAME_SCRIPTS = {
            {"jp.konami.pesam", "beast_mode.sh"},
            {"com.tencent.ig", "network_turbo.sh"},
            {"com.mobile.legends", "network_turbo.sh"},
            {"com.dts.freefireth", "touch_wifi_turbo.sh"},
            {"com.dts.freefiremax", "touch_wifi_turbo.sh"},
            {"com.activision.callofduty.shooter", "touch_wifi_turbo.sh"},
            {"com.miHoYo.GenshinImpact", "beast_mode.sh"},
            {"com.ea.gp.fifamobile", "beast_mode.sh"},
            {"com.roblox.client", "battery_chill.sh"},
    };

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private TextView shizukuStatus;
    private TextView bubbleBtn;
    private TextView hudBtn;
    private TextView logView;
    private AlertDialog logDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Seed bundled .sh scripts on first run (same as standalone booster).
        try {
            ScriptStore.INSTANCE.seedFromAssets(this);
        } catch (Exception ignored) {}

        // Shizuku binder listeners (sticky: fires immediately if already bound).
        try {
            Shizuku.addBinderReceivedListenerSticky(() -> uiHandler.post(this::updateShizukuStatus));
            Shizuku.addBinderDeadListener(() -> uiHandler.post(this::updateShizukuStatus));
            Shizuku.addRequestPermissionResultListener((rc, gr) -> {
                if (rc == REQ_SHIZUKU) uiHandler.post(this::updateShizukuStatus);
            });
        } catch (Exception ignored) {}

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(BG);
        scroll.addView(root);

        // Frame: solid themed background (mascot layer removed for a clean look).
        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        frame.setBackgroundColor(BG);
        frame.addView(scroll, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(frame);

        // ---- header ----
        TextView title = label("DZINLABS", 26, YELLOW, true);
        root.addView(title);
        TextView sub = label("GAMING VPN", 13, CYAN, true);
        sub.setLetterSpacing(0.3f);
        root.addView(sub);

        root.addView(spacer(10));
        shizukuStatus = label("\u25CC SHIZUKU: checking...", 12, DIM, true);
        shizukuStatus.setPadding(dp(2), dp(6), dp(2), dp(6));
        shizukuStatus.setClickable(true);
        shizukuStatus.setFocusable(true);
        shizukuStatus.setOnClickListener(v -> onShizukuStatusTap());
        root.addView(shizukuStatus);


        // ---- VPN ----
        root.addView(spacer(8));
        TextView vpnBtn = solidBtn("OPEN GAMING VPN", YELLOW, 0xFF151918);
        vpnBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName("com.zeus.warpwg", "com.zeus.warpwg.MainActivity");
                try {
                    startActivity(i);
                } catch (Exception e) {
                    Toast.makeText(GameHubActivity.this, "VPN screen unavailable", Toast.LENGTH_SHORT).show();
                }
            }
        });
        root.addView(vpnBtn);

        // ---- BOOST (2x2 compact grid) ----
        root.addView(spacer(12));
        root.addView(label("BOOST", 12, CYAN, true));
        root.addView(spacer(6));
        LinearLayout bRow1 = hRow();
        TextView quickBtn = ghostBtn("QUICK BOOST", CYAN);
        quickBtn.setOnClickListener(v -> doQuickBoost());
        bubbleBtn = ghostBtn("BUBBLE: OFF", CYAN);
        bubbleBtn.setOnClickListener(v -> toggleBubble());
        bRow1.addView(quickBtn);
        bRow1.addView(bubbleBtn);
        root.addView(bRow1);
        root.addView(spacer(6));
        LinearLayout bRow2 = hRow();
        TextView scriptsBtn = ghostBtn("SCRIPTS", CYAN);
        scriptsBtn.setOnClickListener(v -> showScriptsDialog());
        hudBtn = ghostBtn(HudService.isRunning ? "HUD: ON" : "HUD: OFF", MAGENTA);
        hudBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleHud(); }
        });
        bRow2.addView(scriptsBtn);
        bRow2.addView(hudBtn);
        root.addView(bRow2);

        // ---- games (2-col tiles with icons) ----
        root.addView(spacer(12));
        root.addView(label("TAP A GAME TO LAUNCH", 12, CYAN, true));
        root.addView(spacer(6));
        PackageManager pm = getPackageManager();
        LinearLayout row = null;
        for (int i = 0; i < GAMES.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                if (i > 0) root.addView(spacer(8));
                root.addView(row);
            }
            final String name = GAMES[i][0];
            String installedPkg = null;
            for (int k = 1; k < GAMES[i].length; k++) {
                try {
                    pm.getPackageInfo(GAMES[i][k], 0);
                    installedPkg = GAMES[i][k];
                    break;
                } catch (Exception ignored) {}
            }
            final String pkg = installedPkg;
            LinearLayout tile = tile(GAME_ICONS[i], name, pkg != null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(106), 1f);
            if (i % 2 == 0) lp.rightMargin = dp(4);
            else lp.leftMargin = dp(4);
            tile.setLayoutParams(lp);
            if (pkg != null) {
                tile.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        getSharedPreferences("gvpn", MODE_PRIVATE).edit()
                                .putString("last_game_pkg", pkg).apply();
                        Toast.makeText(GameHubActivity.this,
                                "Boosting, then launching " + name + "...",
                                Toast.LENGTH_SHORT).show();
                        new Thread(() -> {
                            try {
                                if (ShizukuBooster.INSTANCE.isShizukuAvailable()
                                        && ShizukuBooster.INSTANCE.hasShizukuPermission()) {
                                    ShizukuBooster.INSTANCE.boostDevice(
                                            GameHubActivity.this,
                                            s -> kotlin.Unit.INSTANCE, false);
                                }
                            } catch (Exception ignored) {}
                            try {
                                runGameProfile(GameHubActivity.this, pkg);
                            } catch (Exception ignored) {}
                            uiHandler.post(() -> {
                                Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
                                if (launch != null) startActivity(launch);
                                else Toast.makeText(GameHubActivity.this,
                                        name + " cannot be launched", Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    }
                });
            }
            row.addView(tile);
        }

        // ---- game tools (2x2 grid) ----
        root.addView(spacer(12));
        root.addView(label("GAME TOOLS", 12, CYAN, true));
        root.addView(spacer(6));
        LinearLayout tRow1 = hRow();
        TextView netCheckBtn = ghostBtn("NET", CYAN);
        netCheckBtn.setOnClickListener(v ->
                startActivity(new Intent(GameHubActivity.this, NetworkCheckActivity.class)));
        TextView reflexBtn = ghostBtn("REFLEX", CYAN);
        reflexBtn.setOnClickListener(v ->
                startActivity(new Intent(GameHubActivity.this, ReflexActivity.class)));
        tRow1.addView(netCheckBtn);
        tRow1.addView(reflexBtn);
        root.addView(tRow1);
        root.addView(spacer(6));
        LinearLayout tRow2 = hRow();
        TextView dashBtn = ghostBtn("DEVICE", CYAN);
        dashBtn.setOnClickListener(v ->
                startActivity(new Intent(GameHubActivity.this, DashboardActivity.class)));
        TextView domainBtn = ghostBtn("DOMAIN", CYAN);
        domainBtn.setOnClickListener(v ->
                startActivity(new Intent(GameHubActivity.this, NetworkInfoActivity.class)));
        tRow2.addView(dashBtn);
        tRow2.addView(domainBtn);
        root.addView(tRow2);

        // ---- app ----
        root.addView(spacer(12));
        root.addView(label("APP", 12, CYAN, true));
        root.addView(spacer(6));
        LinearLayout aRow = hRow();
        TextView settingsBtn = ghostBtn("SETTINGS", MAGENTA);
        settingsBtn.setOnClickListener(v ->
                startActivity(new Intent(GameHubActivity.this, SettingsActivity.class)));
        TextView aboutBtn = ghostBtn("ABOUT", MAGENTA);
        aboutBtn.setOnClickListener(v ->
                startActivity(new Intent(GameHubActivity.this, AboutActivity.class)));
        aRow.addView(settingsBtn);
        aRow.addView(aboutBtn);
        root.addView(aRow);

        root.addView(spacer(12));
        TextView tip = label("Tip: for lowest ping, play with the VPN OFF — " +
                "game tiles auto-boost (Shizuku) before launching. " +
                "Connect the VPN only if your ISP route is unstable.", 11, DIM, false);
        root.addView(tip);

        updateShizukuStatus();
        updateBubbleBtn();
    }

    private LinearLayout hRow() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        return r;
    }

    /** Visible animated mascot strip under the header (decorative). */
    private LinearLayout mascotStrip() {
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER);
        String[] em = {"🎮", "⚽", "🏆", "👾", "✨"};
        for (int i = 0; i < em.length; i++) {
            TextView tv = new TextView(this);
            tv.setText(em[i]);
            tv.setTextSize(24);
            int p = dp(8);
            tv.setPadding(p, 0, p, 0);
            strip.addView(tv);
            android.view.animation.TranslateAnimation a =
                    new android.view.animation.TranslateAnimation(0, 0, 0, dp(8));
            a.setDuration(1800 + i * 220);
            a.setRepeatMode(android.view.animation.Animation.REVERSE);
            a.setRepeatCount(android.view.animation.Animation.INFINITE);
            a.setStartOffset(i * 150);
            tv.startAnimation(a);
        }
        return strip;
    }

    /** Floating game mascots drifting behind the content (decorative). */
    private void addMascots(android.widget.FrameLayout frame) {
        String[] em = {"🎮", "⚽", "🏆", "🪂", "👾", "✨", "🎯", "🏁"};
        // side: 0 = left, 1 = right ; then xDp, yDp
        int[][] pos = {
                {0, 18, 130}, {1, 26, 250}, {0, 55, 430}, {1, 60, 560},
                {0, 24, 700}, {1, 36, 830}, {0, 70, 950}, {1, 80, 1080},
        };
        for (int i = 0; i < pos.length; i++) {
            TextView tv = new TextView(this);
            tv.setText(em[i % em.length]);
            tv.setTextSize(34);
            tv.setAlpha(0.22f);
            int grav = pos[i][0] == 0
                    ? (Gravity.TOP | Gravity.LEFT)
                    : (Gravity.TOP | Gravity.RIGHT);
            android.widget.FrameLayout.LayoutParams lp =
                    new android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT, grav);
            if (pos[i][0] == 0) lp.leftMargin = dp(pos[i][1]);
            else lp.rightMargin = dp(pos[i][1]);
            lp.topMargin = dp(pos[i][2]);
            tv.setLayoutParams(lp);
            frame.addView(tv);
            android.view.animation.TranslateAnimation a =
                    new android.view.animation.TranslateAnimation(0, 0, 0, dp(16));
            a.setDuration(2400 + i * 300);
            a.setRepeatMode(android.view.animation.Animation.REVERSE);
            a.setRepeatCount(android.view.animation.Animation.INFINITE);
            tv.startAnimation(a);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateShizukuStatus();
        updateBubbleBtn();
        if (hudBtn != null)
            hudBtn.setText(HudService.isRunning ? "HUD: ON" : "HUD: OFF");
    }

    // ---------- HUD ----------

    private void toggleHud() {
        if (HudService.isRunning) {
            stopService(new Intent(this, HudService.class));
            hudBtn.setText("HUD: OFF");
        } else {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Grant overlay permission for the HUD first",
                        Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
                return;
            }
            Intent i = new Intent(this, HudService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
            hudBtn.setText("HUD: ON");
            Toast.makeText(this, "HUD on \u2014 drag to move, tap \u2715 to close",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ---------- Shizuku ----------

    private void updateShizukuStatus() {
        if (shizukuStatus == null) return;
        boolean available;
        boolean granted = false;
        try {
            available = ShizukuBooster.INSTANCE.isShizukuAvailable();
            if (available) granted = ShizukuBooster.INSTANCE.hasShizukuPermission();
        } catch (Exception e) {
            available = false;
        }
        if (!available) {
            shizukuStatus.setText("\u25CC SHIZUKU: NOT RUNNING \u2014 tap for help");
            shizukuStatus.setTextColor(AMBER);
        } else if (granted) {
            shizukuStatus.setText("\u25CF SHIZUKU: ONLINE // AUTHORIZED");
            shizukuStatus.setTextColor(ACCENT);
        } else {
            shizukuStatus.setText("\u25CC SHIZUKU: TAP TO AUTHORIZE");
            shizukuStatus.setTextColor(AMBER);
        }
    }

    private void onShizukuStatusTap() {
        boolean available;
        try {
            available = ShizukuBooster.INSTANCE.isShizukuAvailable();
        } catch (Exception e) {
            available = false;
        }
        if (!available) {
            Toast.makeText(this, "Start the Shizuku server first (via Shizuku app / wireless debugging), then reopen this app.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        try {
            if (!ShizukuBooster.INSTANCE.hasShizukuPermission()) requestShizukuPermission();
            else Toast.makeText(this, "Shizuku is ready \u2713", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Shizuku error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void requestShizukuPermission() {
        try {
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                Toast.makeText(this, "Grant Shizuku permission to apply game tweaks", Toast.LENGTH_SHORT).show();
            }
            Shizuku.requestPermission(REQ_SHIZUKU);
        } catch (Exception e) {
            Toast.makeText(this, "Permission request failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean ensureShizuku() {
        boolean available;
        try {
            available = ShizukuBooster.INSTANCE.isShizukuAvailable();
        } catch (Exception e) {
            available = false;
        }
        if (!available) {
            Toast.makeText(this, "Shizuku server not running \u2014 start Shizuku first", Toast.LENGTH_LONG).show();
            return false;
        }
        try {
            if (!ShizukuBooster.INSTANCE.hasShizukuPermission()) {
                requestShizukuPermission();
                return false;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Shizuku error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // ---------- quick boost ----------

    private void doQuickBoost() {
        if (!ensureShizuku()) return;
        showLogDialog("Quick Boost");
        appendLog("Starting boost...");
        new Thread(() -> {
            try {
                ShizukuBooster.INSTANCE.boostDevice(GameHubActivity.this,
                        s -> { appendLog(s); return kotlin.Unit.INSTANCE; }, false);
                appendLog("\u2713 Boost complete \u2014 good luck!");
            } catch (Exception e) {
                appendLog("\u2717 " + e);
            }
        }).start();
    }

    // ---------- bubble ----------

    private void updateBubbleBtn() {
        if (bubbleBtn == null) return;
        boolean on;
        try {
            on = Prefs.INSTANCE.getBoolean(this, Prefs.KEY_BUBBLE, false);
        } catch (Exception e) {
            on = false;
        }
        bubbleBtn.setText(on ? "BUBBLE: ON" : "BUBBLE: OFF");
    }

    private void toggleBubble() {
        boolean on;
        try {
            on = Prefs.INSTANCE.getBoolean(this, Prefs.KEY_BUBBLE, false);
        } catch (Exception e) {
            on = false;
        }
        if (!on) {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Allow 'display over other apps' for the bubble", Toast.LENGTH_LONG).show();
                try {
                    startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
                } catch (Exception e) {
                    Toast.makeText(this, "Cannot open overlay settings", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            try {
                Intent i = new Intent(this, BoostBubbleService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
                else startService(i);
                Prefs.INSTANCE.setBoolean(this, Prefs.KEY_BUBBLE, true);
                Toast.makeText(this, "Bubble ON \u2014 tiny dot on screen", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Bubble failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            try {
                stopService(new Intent(this, BoostBubbleService.class));
            } catch (Exception ignored) {}
            try {
                Prefs.INSTANCE.setBoolean(this, Prefs.KEY_BUBBLE, false);
            } catch (Exception ignored) {}
        }
        updateBubbleBtn();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)) {
                toggleBubble();
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---------- scripts ----------

    private void showScriptsDialog() {
        List<ScriptEntry> scripts;
        try {
            scripts = ScriptStore.INSTANCE.getAll(this);
        } catch (Exception e) {
            Toast.makeText(this, "Script store error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        if (scripts.isEmpty()) {
            Toast.makeText(this, "No scripts found \u2014 reopen the app to re-seed", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[scripts.size()];
        for (int i = 0; i < scripts.size(); i++) names[i] = scripts.get(i).getName();
        new AlertDialog.Builder(this)
                .setTitle("Boost scripts")
                .setItems(names, (d, which) -> runScript(scripts.get(which)))
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private void runScript(ScriptEntry entry) {
        if (!ensureShizuku()) return;
        showLogDialog("\u25B8 " + entry.getName());
        new Thread(() -> {
            try {
                String content = ScriptStore.INSTANCE.readContent(GameHubActivity.this, entry);
                if (content == null || content.trim().isEmpty()) {
                    appendLog("Could not read script \u2014 reopen the app to re-seed.");
                    return;
                }
                appendLog("Running " + entry.getName() + "...");
                ShizukuBooster.INSTANCE.runScript(content,
                        s -> { appendLog(s); return kotlin.Unit.INSTANCE; });
                appendLog("\u2713 Done.");
            } catch (Exception e) {
                appendLog("\u2717 " + e);
            }
        }).start();
    }

    // ---------- log dialog ----------

    private void showLogDialog(String title) {
        if (logDialog != null && logDialog.isShowing()) {
            logView.setText("");
            logDialog.setTitle(title);
            return;
        }
        logView = new TextView(this);
        logView.setTextSize(12);
        logView.setTypeface(android.graphics.Typeface.MONOSPACE);
        logView.setTextColor(TEXT);
        int p = dp(12);
        logView.setPadding(p, p, p, p);
        ScrollView sv = new ScrollView(this);
        sv.addView(logView);
        logDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(sv)
                .setPositiveButton("CLOSE", null)
                .create();
        logDialog.show();
        if (logDialog.getWindow() != null) {
            logDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92), dp(430));
        }
    }

    private void appendLog(final String s) {
        uiHandler.post(() -> {
            if (logView != null) {
                logView.append(s + "\n");
                final ScrollView sv = (ScrollView) logView.getParent();
                if (sv != null) sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    /** Run the mapped tuning script for a game package (silent). */
    static void runGameProfile(Activity ctx, String pkg) {
        String file = null;
        for (String[] m : GAME_SCRIPTS) {
            if (m[0].equals(pkg)) { file = m[1]; break; }
        }
        if (file == null) return;
        try {
            List<ScriptEntry> scripts = ScriptStore.INSTANCE.getAll(ctx);
            for (ScriptEntry e : scripts) {
                if (file.equals(e.getName())) {
                    String content = ScriptStore.INSTANCE.readContent(ctx, e);
                    if (content != null && !content.trim().isEmpty()) {
                        ShizukuBooster.INSTANCE.runScript(content,
                                s -> kotlin.Unit.INSTANCE);
                    }
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    // ---------- views ----------

    private LinearLayout tile(String icon, String name, boolean installed) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable d = new GradientDrawable();
        d.setColor(installed ? 0xFF242B28 : CARD);
        d.setCornerRadius(dp(12));
        d.setStroke(dp(1), installed ? CYAN : 0xFF2F7569);
        t.setBackground(d);
        // Icon badge: soft circle behind the glyph, prettier than a bare emoji.
        android.widget.FrameLayout badge = new android.widget.FrameLayout(this);
        int badgeSize = dp(44);
        android.widget.FrameLayout.LayoutParams badgeLp =
                new android.widget.FrameLayout.LayoutParams(badgeSize, badgeSize);
        badgeLp.gravity = Gravity.CENTER;
        badge.setLayoutParams(badgeLp);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(installed ? 0xFF2F7569 : 0xFF242B28);
        if (installed) circle.setStroke(dp(1), CYAN);
        badge.setBackground(circle);
        TextView ic = new TextView(this);
        ic.setText(icon);
        ic.setTextSize(22);
        ic.setGravity(Gravity.CENTER);
        android.widget.FrameLayout.LayoutParams icLp =
                new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER);
        ic.setLayoutParams(icLp);
        if (!installed) ic.setAlpha(0.35f);
        badge.addView(ic);
        t.addView(badge);
        t.addView(spacer(4));
        TextView n = label(name, 13, installed ? TEXT : DIM, true);
        n.setGravity(Gravity.CENTER);
        t.addView(n);
        t.addView(spacer(4));
        TextView s = label(installed ? "TAP TO PLAY" : "NOT INSTALLED", 9, installed ? CYAN : DIM, true);
        s.setGravity(Gravity.CENTER);
        t.addView(s);
        t.setClickable(installed);
        t.setFocusable(installed);
        return t;
    }

    /** Solid neon button (primary actions). */
    private TextView solidBtn(String text, int bg, int fg) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextSize(15);
        b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
        b.setTextColor(fg);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable d = new GradientDrawable();
        d.setColor(bg);
        d.setCornerRadius(dp(10));
        b.setBackground(d);
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    /** Ghost button: dark card with neon border. Callers set weight via LayoutParams. */
    private TextView ghostBtn(String text, int accent) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextSize(13);
        b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
        b.setTextColor(accent);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(8), dp(10), dp(8), dp(10));
        GradientDrawable d = new GradientDrawable();
        d.setColor(CARD);
        d.setCornerRadius(dp(10));
        d.setStroke(dp(1), accent);
        b.setBackground(d);
        b.setClickable(true);
        b.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        int m = dp(3);
        lp.leftMargin = m;
        lp.rightMargin = m;
        b.setLayoutParams(lp);
        return b;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private View spacer(int dpH) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(dpH)));
        return v;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
