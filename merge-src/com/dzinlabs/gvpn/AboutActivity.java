package com.dzinlabs.gvpn;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Credits + app info. Cyberpunk theme. */
public class AboutActivity extends Activity {

    static final int BG = 0xFF151918;
    static final int YELLOW = 0xFFDDB461;
    static final int CYAN = 0xFF68B6A5;
    static final int MAGENTA = 0xFFE16A5E;
    static final int TEXT = 0xFFF2EEE4;
    static final int DIM = 0xFFADB5AF;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("DZINLABS");
        title.setTextSize(28);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        title.setTextColor(YELLOW);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("G A M I N G   V P N");
        sub.setTextSize(14);
        sub.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        sub.setTextColor(CYAN);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub);

        TextView ver = new TextView(this);
        try {
            ver.setText("v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (Exception e) {
            ver.setText("v4.3");
        }
        ver.setTextSize(12);
        ver.setTypeface(Typeface.MONOSPACE);
        ver.setTextColor(DIM);
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, dp(4), 0, 0);
        root.addView(ver);

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(lp);
        root.addView(scroll);

        body.addView(spacer(14));
        credit(body, "CREATED BY", "Dominic", MAGENTA);
        body.addView(spacer(10));
        credit(body, "ROLE", "Concept \u2022 Design \u2022 Testing", TEXT);
        body.addView(spacer(10));
        credit(body, "BUILT WITH", "Muse (AI build partner)", TEXT);

        body.addView(spacer(16));
        TextView fh = new TextView(this);
        fh.setText("WHAT'S INSIDE");
        fh.setTextSize(12);
        fh.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        fh.setTextColor(CYAN);
        body.addView(fh);
        body.addView(spacer(6));
        String[] feats = {
                "\u26A1 Cloudflare WARP gaming VPN",
                "\uD83C\uDFAF Floating game HUD (FPS / ping / temp)",
                "\uD83D\uDCE1 Network latency checker",
                "\u26A1 Reflex trainer + device readiness",
                "\uD83D\uDCDC 8 pro-tuned boost scripts",
                "\u25C9 Floating quick-boost bubble",
                "\uD83C\uDFAE 1-tap game launcher with auto-boost",
        };
        for (String f : feats) {
            TextView t = new TextView(this);
            t.setText(f);
            t.setTextSize(13);
            t.setTypeface(Typeface.MONOSPACE);
            t.setTextColor(TEXT);
            t.setPadding(0, dp(3), 0, dp(3));
            body.addView(t);
        }

        body.addView(spacer(16));
        TextView ch = new TextView(this);
        ch.setText("CONTACT");
        ch.setTextSize(12);
        ch.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        ch.setTextColor(CYAN);
        ch.setGravity(Gravity.CENTER);
        body.addView(ch);
        body.addView(spacer(6));
        contactRow(body, "Telegram Bot", "https://t.me/Dominic_aiBot");
        contactRow(body, "GitHub", "https://github.com/zinmyo19");
        contactRow(body, "Website", "https://dzinlabs-site.zynelabs.workers.dev/");

        body.addView(spacer(16));
        TextView note = new TextView(this);
        note.setText("WARP\u00AE is a trademark of Cloudflare, Inc.\nMade for family gaming \u2014 share freely.");
        note.setTextSize(11);
        note.setTypeface(Typeface.MONOSPACE);
        note.setTextColor(DIM);
        note.setGravity(Gravity.CENTER);
        body.addView(note);

        setContentView(root);
    }

    private void credit(LinearLayout parent, String k, String v, int color) {
        TextView kk = new TextView(this);
        kk.setText(k);
        kk.setTextSize(11);
        kk.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        kk.setTextColor(DIM);
        kk.setGravity(Gravity.CENTER);
        parent.addView(kk);
        TextView vv = new TextView(this);
        vv.setText(v);
        vv.setTextSize(16);
        vv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        vv.setTextColor(color);
        vv.setGravity(Gravity.CENTER);
        parent.addView(vv);
    }

    private void contactRow(LinearLayout parent, String name, final String url) {
        TextView t = new TextView(this);
        t.setText(name);
        t.setTextSize(14);
        t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        t.setTextColor(CYAN);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(6), 0, dp(6));
        t.setClickable(true);
        t.setFocusable(true);
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {}
            }
        });
        parent.addView(t);
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
