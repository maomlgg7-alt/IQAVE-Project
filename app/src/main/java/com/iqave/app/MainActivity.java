package com.iqave.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.*;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int STORAGE_PERMISSION_CODE = 101;

    // مستقبل نتيجة فتح الملفات
    private final ActivityResultLauncher<Intent> fileChooserLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                String dataString = result.getData().getDataString();
                if (dataString != null) {
                    results = new Uri[]{ Uri.parse(dataString) };
                } else if (result.getData().getClipData() != null) {
                    int count = result.getData().getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = result.getData().getClipData().getItemAt(i).getUri();
                    }
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── شاشة كاملة بدون شريط العنوان ──
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        // Hardware Acceleration للـ Custom Cursor والأنيميشن
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        setupWebView();
        requestStoragePermission();

        // تحميل الصفحة الرئيسية
        webView.loadUrl("file:///android_asset/www/index.html");
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        // ── الإعدادات الأساسية ──
        settings.setJavaScriptEnabled(true);          // لعمل الأنيميشن والـ JS
        settings.setDomStorageEnabled(true);           // لحفظ الإعدادات (localStorage)
        settings.setAllowFileAccess(true);             // لرفع GIF/MP4 من التخزين
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(false); // تشغيل الصوت تلقائياً

        // ── IndexedDB (لحفظ GIF/MP4 كبيرة) ──
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        // ── Hardware Acceleration على مستوى WebView ──
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // ── إخفاء Scrollbar ──
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);

        // ── WebViewClient: فتح كل الروابط داخل التطبيق ──
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // الروابط الخارجية تفتح داخل التطبيق
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url);
                    return true;
                }
                // روابط الملفات المحلية
                if (url.startsWith("file://")) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // إخفاء شريط التحميل إذا موجود
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // صفحة offline محلية عند انقطاع النت
                if (request.isForMainFrame()) {
                    view.loadUrl("file:///android_asset/www/offline.html");
                }
            }
        });

        // ── WebChromeClient: رفع الملفات + الجيولوكيشن ──
        webView.setWebChromeClient(new WebChromeClient() {

            // رفع الملفات (GIF, MP4, صور, صوت)
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                              FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                // دعم ملفات متعددة
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                fileChooserLauncher.launch(intent);
                return true;
            }

            // إذن الموقع — يسأل مرة واحدة فقط
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                    GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, true); // remember=true لمرة واحدة
            }

            // Console.log للـ debugging
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                return true;
            }
        });

        // JavaScript Bridge — للتواصل بين JS والـ Native
        webView.addJavascriptInterface(new IQAVEBridge(), "AndroidBridge");
    }

    // ── JavaScript Bridge ──
    public class IQAVEBridge {
        @android.webkit.JavascriptInterface
        public String getDeviceInfo() {
            return "Android " + Build.VERSION.RELEASE + " | " + Build.MODEL;
        }

        @android.webkit.JavascriptInterface
        public boolean isAndroid() { return true; }

        @android.webkit.JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }
    }

    // ── طلب صلاحية التخزين ──
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ — READ_MEDIA_IMAGES و READ_MEDIA_VIDEO
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                }, STORAGE_PERMISSION_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, STORAGE_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // لا حاجة لعمل شيء — WebView يتولى الباقي
    }

    // ── زر الرجوع ──
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // ── إيقاف/استئناف الصوت ──
    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        webView.pauseTimers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.resumeTimers();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }
}
