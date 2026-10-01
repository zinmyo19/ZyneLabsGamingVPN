package com.dzinlabs.gvpn;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

/** Reaction-time trainer: wait for green, tap fast. 5 rounds, shows avg + best. */
public class ReflexActivity extends Activity {
    private static final int ROUNDS = 5;

    private TextView screen;
    private TextView stats;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();

    private int state = 0; // 0 idle, 1 waiting(red), 2 ready(green)
    private long greenAt = 0;
    private int round = 0;
    private long totalMs = 0;
    private long bestMs = Long.MAX_VALUE;
    private Runnable goGreen;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF151918);

        screen = new TextView(this);
        screen.setGravity(Gravity.CENTER);
        screen.setTextSize(26);
        screen.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        screen.setLayoutParams(lp);
        screen.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onTap(); }
        });
        root.addView(screen);

        stats = new TextView(this);
        stats.setGravity(Gravity.CENTER);
        stats.setTextSize(14);
        stats.setTypeface(Typeface.MONOSPACE);
        stats.setTextColor(0xFFADB5AF);
        stats.setPadding(0, dp(14), 0, dp(18));
        root.addView(stats);

        setContentView(root);
        showIdle();
    }

    private void showIdle() {
        state = 0;
        screen.setBackgroundColor(0xFF202624);
        screen.setTextColor(0xFFF2EEE4);
        screen.setText("\u26A1 REFLEX TRAINER\n\nTAP TO START");
        stats.setText(round > 0
                ? ("Rounds: " + round + "/" + ROUNDS + "  \u2022  avg " + (totalMs / round) + " ms  \u2022  best " + bestMs + " ms")
                : "Tap as fast as you can when it turns GREEN.");
    }

    private void onTap() {
        if (state == 0) {
            if (round >= ROUNDS) { round = 0; totalMs = 0; bestMs = Long.MAX_VALUE; }
            state = 1;
            screen.setBackgroundColor(0xFF7F1D1D);
            screen.setTextColor(0xFFFFFFFF);
            screen.setText("WAIT FOR GREEN...");
            stats.setText("Don't tap early!");
            goGreen = new Runnable() {
                @Override public void run() {
                    state = 2;
                    greenAt = System.currentTimeMillis();
                    screen.setBackgroundColor(0xFF16A34A);
                    screen.setText("TAP NOW!");
                }
            };
            h.postDelayed(goGreen, 1200 + rnd.nextInt(2800));
        } else if (state == 1) {
            h.removeCallbacks(goGreen);
            screen.setBackgroundColor(0xFF92400E);
            screen.setText("\u26A0 TOO SOON!\n\nTAP TO RETRY");
            state = 0;
            stats.setText("Wait for green before tapping.");
        } else if (state == 2) {
            long ms = System.currentTimeMillis() - greenAt;
            round++;
            totalMs += ms;
            if (ms < bestMs) bestMs = ms;
            state = 0;
            if (round >= ROUNDS) {
                screen.setBackgroundColor(0xFF202624);
                screen.setText("\uD83C\uDFC1 DONE!\n\navg " + (totalMs / round) + " ms\nbest " + bestMs + " ms\n\nTAP TO PLAY AGAIN");
                stats.setText("Pro gamers: < 200 ms \u2022 Casual: 200\u2013300 ms");
            } else {
                showIdle();
                screen.setText(String.valueOf(ms) + " ms\n\nTAP FOR ROUND " + (round + 1));
            }
        }
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
