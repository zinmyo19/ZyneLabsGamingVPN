package com.dzinlabs.gvpn;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Enter / manage the PRO license key. Cyberpunk theme. */
public class LicenseActivity extends Activity {

    // Dominic: change to your sales bot / channel link before release.
    public static final String BUY_URL = "https://t.me/dzinlabs";

    static final int BG = 0xFF151918;
    static final int CARD = 0xFF202624;
    static final int YELLOW = 0xFFDDB461;
    static final int CYAN = 0xFF68B6A5;
    static final int MAGENTA = 0xFFE16A5E;
    static final int TEXT = 0xFFF2EEE4;
    static final int DIM = 0xFFADB5AF;

    private TextView statusView;
    private EditText keyInput;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("\uD83D\uDD11 LICENSE");
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

        body.addView(spacer(10));
        statusView = new TextView(this);
        statusView.setTextSize(15);
        statusView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, dp(10), 0, dp(10));
        body.addView(statusView);

        body.addView(spacer(8));
        TextView hint = new TextView(this);
        hint.setText("PRO unlocks: HUD overlay, boost scripts,\nper-game auto profiles.");
        hint.setTextSize(12);
        hint.setTypeface(Typeface.MONOSPACE);
        hint.setTextColor(DIM);
        hint.setGravity(Gravity.CENTER);
        body.addView(hint);

        body.addView(spacer(10));
        keyInput = new EditText(this);
        keyInput.setHint("DZIN-XXXX-XXXX-XXXX");
        keyInput.setTextSize(16);
        keyInput.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        keyInput.setTextColor(TEXT);
        keyInput.setHintTextColor(DIM);
        keyInput.setGravity(Gravity.CENTER);
        keyInput.setPadding(dp(10), dp(12), dp(10), dp(12));
        GradientDrawable id = new GradientDrawable();
        id.setColor(CARD);
        id.setCornerRadius(dp(10));
        id.setStroke(dp(1), CYAN);
        keyInput.setBackground(id);
        body.addView(keyInput);

        body.addView(spacer(10));
        TextView activate = neonBtn("ACTIVATE", CYAN);
        activate.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String k = keyInput.getText().toString().trim();
                if (k.isEmpty()) {
                    Toast.makeText(LicenseActivity.this, "Enter a key first",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                LicenseManager.setKey(LicenseActivity.this, k);
                statusView.setText("Verifying...");
                statusView.setTextColor(DIM);
                LicenseManager.verify(LicenseActivity.this, (pro, msg) -> {
                    refreshStatus();
                    Toast.makeText(LicenseActivity.this, msg, Toast.LENGTH_SHORT).show();
                });
            }
        });
        body.addView(activate);

        body.addView(spacer(8));
        TextView remove = neonBtn("REMOVE KEY", MAGENTA);
        remove.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                LicenseManager.clearKey(LicenseActivity.this);
                keyInput.setText("");
                refreshStatus();
                Toast.makeText(LicenseActivity.this, "Key removed", Toast.LENGTH_SHORT).show();
            }
        });
        body.addView(remove);

        body.addView(spacer(14));
        TextView buy = neonBtn("\uD83D\uDED2 BUY PRO VIA TELEGRAM", YELLOW);
        buy.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BUY_URL)));
                } catch (Exception e) {
                    Toast.makeText(LicenseActivity.this, "Cannot open link",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        body.addView(buy);

        String saved = LicenseManager.getKey(this);
        if (!saved.isEmpty()) keyInput.setText(saved);
        refreshStatus();
        setContentView(root);
    }

    private void refreshStatus() {
        boolean pro = LicenseManager.isProCached(this);
        if (pro) {
            statusView.setText("\u2605 PRO ACTIVE");
            statusView.setTextColor(YELLOW);
        } else if (!LicenseManager.getKey(this).isEmpty()) {
            statusView.setText("KEY SAVED \u2014 not verified");
            statusView.setTextColor(MAGENTA);
        } else {
            statusView.setText("FREE EDITION");
            statusView.setTextColor(DIM);
        }
    }

    private TextView neonBtn(String text, int accent) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(14);
        btn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        btn.setTextColor(accent);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, dp(12), 0, dp(12));
        GradientDrawable d = new GradientDrawable();
        d.setColor(CARD);
        d.setCornerRadius(dp(10));
        d.setStroke(dp(1), accent);
        btn.setBackground(d);
        btn.setClickable(true);
        btn.setFocusable(true);
        return btn;
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
