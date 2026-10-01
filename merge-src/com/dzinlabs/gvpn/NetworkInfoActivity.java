package com.dzinlabs.gvpn;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Static info screen: domain, Zero Trust, WARP notes. */
public class NetworkInfoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(GameHubActivity.BG);
        scroll.addView(root);
        setContentView(scroll);

        root.addView(title("NETWORK & DOMAIN"));

        root.addView(card("\uD83C\uDF10 dzinlabs.dpdns.org",
                "Your Cloudflare-managed domain.\n\n" +
                "DNS is hosted on Cloudflare. When you run your own VPN server " +
                "later, point a grey-cloud (DNS only) A record such as " +
                "vpn.dzinlabs.dpdns.org at it \u2014 Cloudflare proxy does not " +
                "forward raw WireGuard UDP, so keep it DNS-only."));

        root.addView(card("\uD83D\uDEE1 Cloudflare Zero Trust",
                "Zero Trust free tier covers up to 50 users \u2014 plenty for family.\n\n" +
                "It can enroll family devices with identity-aware access and " +
                "private DNS filtering. Device enrollment from inside this app " +
                "is a future step; for now enroll via the Cloudflare One dashboard."));

        root.addView(card("\u26A1 WARP tunnel",
                "This app connects through Cloudflare WARP over WireGuard:\n" +
                "\u2022 Register a fresh WARP account from the VPN screen\n" +
                "\u2022 Default endpoint engage.cloudflareclient.com:2408\n" +
                "\u2022 Private DNS is 1.1.1.1 inside the tunnel\n\n" +
                "WARP gives a stable, clean route to game servers. It does not " +
                "choose exit regions on the free plan."));

        root.addView(card("\uD83C\uDFAE Gaming tips",
                "\u2022 Connect the VPN before matchmaking\n" +
                "\u2022 Use the Game Booster bubble for one-tap boost in-match\n" +
                "\u2022 If a game lags, switch WARP endpoint port " +
                "(2408 / 500 / 4500 / 1701)"));

        View back = backButton();
        root.addView(back);
    }

    private TextView title(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(22);
        v.setTextColor(GameHubActivity.ACCENT);
        v.setTypeface(v.getTypeface(), android.graphics.Typeface.BOLD);
        v.setPadding(0, 0, 0, dp(14));
        return v;
    }

    private LinearLayout card(String head, String body) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable d = new GradientDrawable();
        d.setColor(GameHubActivity.CARD);
        d.setCornerRadius(dp(14));
        d.setStroke(dp(1), 0xFF2F7569);
        c.setBackground(d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        c.setLayoutParams(lp);

        TextView h = new TextView(this);
        h.setText(head);
        h.setTextSize(16);
        h.setTextColor(GameHubActivity.TEXT);
        h.setTypeface(h.getTypeface(), android.graphics.Typeface.BOLD);
        h.setPadding(0, 0, 0, dp(8));
        c.addView(h);

        TextView b = new TextView(this);
        b.setText(body);
        b.setTextSize(13);
        b.setTextColor(GameHubActivity.DIM);
        b.setLineSpacing(dp(2), 1f);
        c.addView(b);
        return c;
    }

    private View backButton() {
        TextView b = new TextView(this);
        b.setText("\u2190 BACK");
        b.setTextSize(15);
        b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
        b.setTextColor(GameHubActivity.TEXT);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable d = new GradientDrawable();
        d.setColor(GameHubActivity.CARD);
        d.setCornerRadius(dp(14));
        d.setStroke(dp(1), 0xFF2F7569);
        b.setBackground(d);
        b.setClickable(true);
        b.setFocusable(true);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
