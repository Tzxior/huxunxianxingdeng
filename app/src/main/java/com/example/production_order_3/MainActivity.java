package com.example.production_order_3;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private static final String TAG = "WebView4KFix";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);

        // ========== 第一步：获取真实物理分辨率 ==========
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        // 关键：必须用 getRealMetrics 获取物理像素，不能用 getMetrics（会被系统缩放）
        DisplayMetrics realMetrics = new DisplayMetrics();
        display.getRealMetrics(realMetrics);

        // 备用方式：如果 getRealMetrics 被厂商阉割，用 Point 方式
        Point realSize = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealSize(realSize);
        }

        // 打印关键调试信息（日志会自动保存，方便您后续提取）
        Log.d(TAG, "=== WebView 4K Fix Debug Info ===");
        Log.d(TAG, "RealMetrics: " + realMetrics.widthPixels + "x" + realMetrics.heightPixels);
        Log.d(TAG, "RealSize: " + realSize.x + "x" + realSize.y);
        Log.d(TAG, "Density: " + realMetrics.density + ", Dpi: " + realMetrics.densityDpi);
        Log.d(TAG, "ScaledDensity: " + realMetrics.scaledDensity);
        Log.d(TAG, "XDpi: " + realMetrics.xdpi + ", YDpi: " + realMetrics.ydpi);

        // ========== 第二步：强制WebView尺寸=真实物理像素 ==========
        // 重新设置WebView的布局参数，锁定物理分辨率
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                realMetrics.widthPixels,
                realMetrics.heightPixels
        );
        webView.setLayoutParams(layoutParams);

        // ========== 第三步：强制启用硬件加速（GPU渲染） ==========
        // 方法1：View层设置
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        // 方法2：确保系统层面也开启（双保险）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        // ========== 第四步：WebSettings 终极配置 ==========
        WebSettings webSettings = webView.getSettings();

        // 基础功能
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        // ===== 核心修复：Viewport强制1:1物理像素 =====
        // 启用视口，但禁止自动缩放
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(false);  // 关键：必须false

        // 强制初始缩放为100%（即1:1物理像素）
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setTextZoom(100);

        // ===== 新增：强制禁止系统字体缩放 =====
        webSettings.setTextZoom(100);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.setSafeBrowsingEnabled(false);  // 禁用安全浏览，避免额外渲染层
        }

        // ===== 缓存策略 =====
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // ========== 第五步：WebViewClient + WebChromeClient ==========
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page loaded: " + url);

                // ===== 页面加载完成后，注入JS强制修正CSS视口 =====
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    String jsFix =
                            "javascript:(function() {" +
                                    "  var meta = document.createElement('meta');" +
                                    "  meta.name = 'viewport';" +
                                    "  meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no, target-densitydpi=device-dpi';" +
                                    "  document.getElementsByTagName('head')[0].appendChild(meta);" +
                                    "  // 强制body宽高100%" +
                                    "  document.body.style.width = '100%';" +
                                    "  document.body.style.height = '100%';" +
                                    "  document.body.style.margin = '0';" +
                                    "  document.body.style.padding = '0';" +
                                    "  // 如果使用flex布局，强制100vw/100vh" +
                                    "  var allDivs = document.getElementsByTagName('*');" +
                                    "  for(var i=0; i<allDivs.length; i++) {" +
                                    "    if(allDivs[i].style.width === '100vw' || allDivs[i].style.width === '100%') {" +
                                    "      allDivs[i].style.width = window.innerWidth + 'px';" +
                                    "    }" +
                                    "    if(allDivs[i].style.height === '100vh' || allDivs[i].style.height === '100%') {" +
                                    "      allDivs[i].style.height = window.innerHeight + 'px';" +
                                    "    }" +
                                    "  }" +
                                    "})()";
                    webView.evaluateJavascript(jsFix, null);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress == 100) {
                    Log.d(TAG, "Page fully loaded, progress: 100%");
                }
            }
        });

        // ========== 第六步：加载页面 ==========
        webView.loadUrl(
                "http://192.168.1.243:37125/show?line=linear&incomplete=10&autoScroll=false"
        );

        Log.d(TAG, "=== WebView 4K Fix Applied Successfully ===");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // ========== 额外：在onResume中强制刷新尺寸（防止电视系统后台篡改） ==========
    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            // 重新获取真实分辨率（防止电视切换分辨率导致）
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);

            // 如果尺寸变化，重新设置
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) webView.getLayoutParams();
            if (params.width != realMetrics.widthPixels || params.height != realMetrics.heightPixels) {
                params.width = realMetrics.widthPixels;
                params.height = realMetrics.heightPixels;
                webView.setLayoutParams(params);
                Log.d(TAG, "onResume: WebView resized to " + realMetrics.widthPixels + "x" + realMetrics.heightPixels);
            }

            // 强制重绘
            webView.invalidate();
        }
    }
}