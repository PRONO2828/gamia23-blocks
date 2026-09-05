package com.gamia23.blocks;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Button;

/**
 * A thin shell around the Gamia23 block puzzle.
 *
 * The game itself lives on the website, not in this APK. That is deliberate:
 * coins are awarded by the server, so the game has to be online anyway, and
 * keeping the code on the web means fixing a bug or changing the rules is a
 * deploy rather than a new APK that every player has to reinstall.
 *
 * What this shell adds over just opening a browser: it launches straight into
 * the game, keeps the login cookie between sessions, makes the Android back
 * button walk back through the game rather than closing the app, and shows a
 * real message when the phone is offline instead of a browser error page.
 */
public class MainActivity extends Activity {

    private static final String HOST = "gamia23.duckdns.org";
    private static final String FALLBACK_HOST = "gamia23.vercel.app";
    private static final String START_URL = "https://" + FALLBACK_HOST + "/game";

    private WebView web;
    private View offline;
    private boolean failed = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        // The game keeps a personal-best in localStorage; without this it
        // silently fails to save.
        s.setDomStorageEnabled(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Persist the login cookie across launches, so players sign in once.
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                String host = u.getHost() == null ? "" : u.getHost();
                // Gamia23 pages stay in the app. Anything else — a support link,
                // the chat widget's pop-outs — goes to the real browser, so
                // players never end up stuck on a third-party page with no way
                // back and no address bar.
                if (host.endsWith(HOST) || host.endsWith(FALLBACK_HOST)) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) {
                    return false;
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                failed = false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req,
                                        android.webkit.WebResourceError err) {
                // Only the main document failing means "we're offline" — a
                // failed image or the chat widget shouldn't blank the game.
                if (req.isForMainFrame()) {
                    failed = true;
                    showOffline(true);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!failed) showOffline(false);
            }
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(web, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        offline = buildOfflineView();
        offline.setVisibility(View.GONE);

        android.widget.FrameLayout stack = new android.widget.FrameLayout(this);
        stack.addView(root);
        stack.addView(offline);
        setContentView(stack);

        if (saved != null) web.restoreState(saved);
        else web.loadUrl(START_URL);
    }

    private View buildOfflineView() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(android.view.Gravity.CENTER);
        box.setBackgroundColor(0xFF0B0F1A);
        box.setPadding(64, 64, 64, 64);

        TextView t = new TextView(this);
        t.setText("You're offline");
        t.setTextColor(0xFFE8ECF6);
        t.setTextSize(22);
        t.setGravity(android.view.Gravity.CENTER);

        TextView p = new TextView(this);
        p.setText("Block Puzzle needs a connection so your coins can be saved to your account.");
        p.setTextColor(0xFF9AA6C4);
        p.setTextSize(15);
        p.setGravity(android.view.Gravity.CENTER);
        p.setPadding(0, 20, 0, 28);

        Button retry = new Button(this);
        retry.setText("Try again");
        retry.setOnClickListener(v -> {
            showOffline(false);
            web.loadUrl(START_URL);
        });

        box.addView(t);
        box.addView(p);
        box.addView(retry);
        return box;
    }

    private void showOffline(boolean show) {
        offline.setVisibility(show ? View.VISIBLE : View.GONE);
        web.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Back walks back through the game before it closes the app.
        if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack()) {
            web.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
