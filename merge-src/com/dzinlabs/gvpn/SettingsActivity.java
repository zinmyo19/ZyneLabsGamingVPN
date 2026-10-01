package com.dzinlabs.gvpn;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.booster.shizuku.Prefs;

/** Boost options + app links. Cyberpunk theme. */
public class SettingsActivity extends Activity {

    static final int BG = 0xFF151918;
    static final int CARD = 0xFF202624;
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
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("\u2699 SETTINGS");
        title.setTextSize(22);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        title.setTextColor(YELLOW);
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(lp);
        root.addView(scroll);

        body.addView(spacer(8));
        body.addView(section("BOOST OPTIONS"));
        body.addView(spacer(6));
        toggleRow(body, "Kill Facebook on boost", Prefs.KEY_KILL_FB, true);
        toggleRow(body, "Kill Messenger on boost", Prefs.KEY_KILL_MSG, true);
        toggleRow(body, "Kill Instagram on boost", Prefs.KEY_KILL_IG, true);
        toggleRow(body, "Kill TikTok on boost", Prefs.KEY_KILL_TT, true);
        toggleRow(body, "Kill Chrome on boost", Prefs.KEY_KILL_CHROME, true);
        toggleRow(body, "Disable system animations", Prefs.KEY_DISABLE_ANIM, true);
        toggleRow(body, "Do Not Disturb while boosting", Prefs.KEY_DND, true);
        toggleRow(body, "Touch response tweaks", Prefs.KEY_TOUCH_BOOST, true);
        toggleRow(body, "Network stabilization", Prefs.KEY_NET_STABLE, true);
        toggleRow(body, "POCO F5 Pro beast tuning", Prefs.KEY_POCO_BEAST, true);

        body.addView(spacer(14));
        body.addView(section("APP"));
        body.addView(spacer(6));
        TextView aboutBtn = new TextView(this);
        aboutBtn.setText("\u24D8 ABOUT THIS APP");
        aboutBtn.setTextSize(14);
        aboutBtn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        aboutBtn.setTextColor(MAGENTA);
        aboutBtn.setGravity(Gravity.CENTER);
        aboutBtn.setPadding(0, dp(12), 0, dp(12));
        GradientDrawable d = new GradientDrawable();
        d.setColor(CARD);
        d.setCornerRadius(dp(10));
        d.setStroke(dp(1), MAGENTA);
        aboutBtn.setBackground(d);
        aboutBtn.setClickable(true);
        aboutBtn.setFocusable(true);
        aboutBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, AboutActivity.class));
            }
        });
        body.addView(aboutBtn);

        setContentView(root);
    }

    private TextView section(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        t.setTextColor(CYAN);
        return t;
    }

    private void toggleRow(LinearLayout parent, String labelText, final String key, boolean def) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextSize(14);
        label.setTypeface(Typeface.MONOSPACE);
        label.setTextColor(TEXT);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        label.setLayoutParams(llp);
        row.addView(label);

        Switch sw = new Switch(this);
        boolean on;
        try { on = Prefs.INSTANCE.getBoolean(this, key, def); }
        catch (Exception e) { on = def; }
        sw.setChecked(on);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try { Prefs.INSTANCE.setBoolean(SettingsActivity.this, key, isChecked); }
                catch (Exception ignored) {}
            }
        });
        row.addView(sw);
        parent.addView(row);

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
