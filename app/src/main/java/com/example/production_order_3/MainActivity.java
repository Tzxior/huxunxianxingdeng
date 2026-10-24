package com.example.production_order_3;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ProductionBoard";
    private static final String PAGE_URL =
            "http://192.168.1.243:37125/show?line=linear&incomplete=10&autoScroll=false";

    private WebView webView;
    private TextView statusView;
    private boolean loadFailed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 仅调整本应用的显示密度，使 1920×1080 的应用窗口对应 1920×1080 CSS 视口。
        // 不修改电视系统的全局分辨率或密度。
        DisplayMetrics appMetrics = getResources().getDisplayMetrics();
        appMetrics.density = 1.0f;
        appMetrics.densityDpi = DisplayMetrics.DENSITY_MEDIUM;
        appMetrics.scaledDensity = 1.0f;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webView);

        logDisplayInfo();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setBackgroundColor(Color.BLACK);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                loadFailed = false;
                showStatus("正在加载生产看板…");
                Log.d(TAG, "Loading: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                loadFailed = false;
                hideStatus();
                logWebViewInfo();
                Log.d(TAG, "Loaded: " + url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    loadFailed = true;
                    showStatus("看板加载失败。请检查电视与服务器是否在同一网络，然后按遥控器确认键重试。");
                    Log.e(TAG, "Main-frame load error: " + error.getDescription());
                }
            }
        });

        statusView = new TextView(this);
        statusView.setTextColor(Color.WHITE);
        statusView.setTextSize(22);
        statusView.setGravity(android.view.Gravity.CENTER);
        statusView.setPadding(48, 32, 48, 32);
        statusView.setBackgroundColor(0xDD000000);
        statusView.setVisibility(View.GONE);
        FrameLayout root = (FrameLayout) webView.getParent();
        root.addView(statusView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        webView.loadUrl(PAGE_URL);
    }

    private void showStatus(String message) {
        if (statusView != null) {
            statusView.setText(message);
            statusView.setVisibility(View.VISIBLE);
        }
    }

    private void hideStatus() {
        if (statusView != null) {
            statusView.setVisibility(View.GONE);
        }
    }

    private void logDisplayInfo() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        Log.d(TAG, "Display pixels=" + metrics.widthPixels + "x" + metrics.heightPixels
                + ", density=" + metrics.density + ", densityDpi=" + metrics.densityDpi);
    }

    private void logWebViewInfo() {
        webView.post(() -> {
            Log.d(TAG, "WebView size=" + webView.getWidth() + "x" + webView.getHeight()
                    + ", scale=" + webView.getScale()
                    + ", contentHeight=" + webView.getContentHeight());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(
                        "JSON.stringify({innerWidth:innerWidth,innerHeight:innerHeight,"
                                + "devicePixelRatio:devicePixelRatio,"
                                + "screenWidth:screen.width,screenHeight:screen.height})",
                        value -> Log.d(TAG, "Page viewport=" + value));
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (loadFailed && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER)) {
            webView.reload();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}