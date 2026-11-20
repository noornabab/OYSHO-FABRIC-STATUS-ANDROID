
package com.oysho.fabricstatus;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String HOME =
            "https://oysho.update-3d2.workers.dev/";

    private static final int FILE_CHOOSER_REQUEST = 501;

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setBackgroundColor(0xFFFFFFFF);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
        );

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance()
                .setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
            ) {
                Uri uri = request.getUrl();

                String scheme = uri.getScheme() == null
                        ? ""
                        : uri.getScheme();

                if ("https".equalsIgnoreCase(scheme)
                        || "http".equalsIgnoreCase(scheme)) {
                    return false;
                }

                try {
                    startActivity(
                            new Intent(Intent.ACTION_VIEW, uri)
                    );
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(
                            MainActivity.this,
                            "এই লিংক খোলার অ্যাপ পাওয়া যায়নি",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params
            ) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = callback;

                try {
                    startActivityForResult(
                            params.createIntent(),
                            FILE_CHOOSER_REQUEST
                    );
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;

                    Toast.makeText(
                            MainActivity.this,
                            "ফাইল নির্বাচন করা যাচ্ছে না",
                            Toast.LENGTH_SHORT
                    ).show();

                    return false;
                }

                return true;
            }
        });

        webView.setDownloadListener(
                (url, userAgent, contentDisposition,
                 mimeType, contentLength) -> {

                    try {
                        DownloadManager.Request request =
                                new DownloadManager.Request(
                                        Uri.parse(url)
                                );

                        String cookies =
                                CookieManager.getInstance()
                                        .getCookie(url);

                        if (cookies != null) {
                            request.addRequestHeader(
                                    "Cookie", cookies
                            );
                        }

                        if (userAgent != null) {
                            request.addRequestHeader(
                                    "User-Agent", userAgent
                            );
                        }

                        request.setNotificationVisibility(
                                DownloadManager.Request
                                        .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                        );

                        request.setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                "OYSHO_" + System.currentTimeMillis()
                                        + getExtension(mimeType, url)
                        );

                        request.setTitle(
                                "OYSHO Fabric Status download"
                        );

                        DownloadManager manager =
                                (DownloadManager)
                                        getSystemService(DOWNLOAD_SERVICE);

                        manager.enqueue(request);

                        Toast.makeText(
                                this,
                                "ডাউনলোড শুরু হয়েছে",
                                Toast.LENGTH_SHORT
                        ).show();

                    } catch (Exception e) {
                        Toast.makeText(
                                this,
                                "ডাউনলোড শুরু করা যায়নি",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );

        if (savedInstanceState == null) {
            webView.loadUrl(HOME);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private String getExtension(String mime, String url) {

        String lower = url.toLowerCase();

        if (lower.contains(".pdf")) return ".pdf";
        if (lower.contains(".xlsx")) return ".xlsx";
        if (lower.contains(".xls")) return ".xls";
        if (lower.contains(".csv")) return ".csv";

        if (mime != null && mime.contains("pdf")) {
            return ".pdf";
        }

        return ".bin";
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST
                && fileCallback != null) {

            Uri[] result =
                    WebChromeClient.FileChooserParams
                            .parseResult(resultCode, data);

            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) {
            webView.saveState(outState);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }

        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
        }

        super.onDestroy();
    }
}
