package com.example.production_order_3;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
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
    private TextView diagnosticView;
    private String displayInfo = "屏幕参数：读取中";
    private String webViewInfo = "WebView：等待页面加载";
    private String pageInfo = "网页视口：等待页面加载";
    private boolean diagnosticMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        settings.setLoadWithOverviewMode(true); // Fit wide desktop pages to the visible WebView width
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // The app window is already hardware-accelerated in AndroidManifest.xml.
        // Avoid forcing an extra hardware layer, which may be composited as a scaled texture
        // on TVs whose app surface is rendered below the panel resolution.
        webView.setLayerType(View.LAYER_TYPE_NONE, null);
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

        diagnosticView = new TextView(this);
        diagnosticView.setTextColor(Color.WHITE);
        diagnosticView.setTextSize(24);
        diagnosticView.setGravity(android.view.Gravity.CENTER_VERTICAL);
        diagnosticView.setPadding(60, 40, 60, 40);
        diagnosticView.setBackgroundColor(Color.BLACK);
        diagnosticView.setElevation(100f);
        diagnosticView.setFocusable(false);
        FrameLayout.LayoutParams diagnosticParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        root.addView(diagnosticView, diagnosticParams);
        updateDiagnosticView();

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

    @SuppressWarnings("deprecation")
    private void logDisplayInfo() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        StringBuilder info = new StringBuilder();
        info.append("设备：").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
                .append("\n应用逻辑尺寸：").append(metrics.widthPixels).append(" × ").append(metrics.heightPixels)
                .append("\ndensity：").append(metrics.density)
                .append("\ndensityDpi：").append(metrics.densityDpi);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode current = display.getMode();
            info.append("\n\n当前显示模式：").append(current.getPhysicalWidth()).append(" × ")
                    .append(current.getPhysicalHeight()).append(" @ ").append(current.getRefreshRate()).append(" Hz");
            info.append("\n支持的显示模式：");
            for (Display.Mode mode : display.getSupportedModes()) {
                info.append("\n").append(mode.getPhysicalWidth()).append(" × ")
                        .append(mode.getPhysicalHeight()).append(" @ ").append(mode.getRefreshRate()).append(" Hz");
            }
        }
        displayInfo = info.toString();
        Log.d(TAG, displayInfo);
        updateDiagnosticView();
    }

    private void logWebViewInfo() {
        webView.post(() -> {
            webViewInfo = "WebView 实际尺寸：" + webView.getWidth() + " × " + webView.getHeight()
                    + "\nWebView scale：" + webView.getScale()
                    + "\n网页内容高度：" + webView.getContentHeight();
            updateDiagnosticView();
            Log.d(TAG, "WebView size=" + webView.getWidth() + "x" + webView.getHeight()
                    + ", scale=" + webView.getScale()
                    + ", contentHeight=" + webView.getContentHeight());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(
                        "JSON.stringify({innerWidth:innerWidth,innerHeight:innerHeight,"
                                + "devicePixelRatio:devicePixelRatio,"
                                + "screenWidth:screen.width,screenHeight:screen.height})",
                        value -> {
                            pageInfo = "网页视口（JSON）：\n" + value;
                            updateDiagnosticView();
                            Log.d(TAG, "Page viewport=" + value);
                        });
            }
        });
    }

    private void updateDiagnosticView() {
        if (diagnosticView != null) {
            diagnosticView.setText("显示诊断\n\n" + displayInfo + "\n\n"
                    + webViewInfo + "\n\n" + pageInfo
                    + "\n按遥控器【确认/OK】键进入看板。");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (diagnosticMode && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER)) {
            diagnosticMode = false;
            diagnosticView.setVisibility(View.GONE);
            return true;
        }
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