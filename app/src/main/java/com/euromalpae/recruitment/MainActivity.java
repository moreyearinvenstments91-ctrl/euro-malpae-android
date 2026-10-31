
package com.euromalpae.recruitment;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    private static final int FILE_CHOOSER_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient());

        // FILE UPLOAD SUPPORT
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = callback;

                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(
                            intent,
                            FILE_CHOOSER_REQUEST
                    );
                } catch (Exception e) {
                    fileCallback.onReceiveValue(null);
                    fileCallback = null;
                    Toast.makeText(
                            MainActivity.this,
                            "Unable to open file chooser",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                return true;
            }
        });

        // PDF AND ZIP DOWNLOAD SUPPORT
        webView.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(
                    String url,
                    String userAgent,
                    String contentDisposition,
                    String mimeType,
                    long contentLength) {

                downloadFile(
                        url,
                        userAgent,
                        contentDisposition,
                        mimeType
                );
            }
        });

        webView.loadUrl(
                "https://maple-ncubez-recruitment.onrender.com"
        );
    }

    // DOWNLOAD FILES TO THE PHONE
    private void downloadFile(
            String url,
            String userAgent,
            String contentDisposition,
            String mimeType) {

        try {

            DownloadManager.Request request =
                    new DownloadManager.Request(Uri.parse(url));

            String fileName = URLUtil.guessFileName(
                    url,
                    contentDisposition,
                    mimeType
            );

            request.setTitle(fileName);
            request.setDescription("Downloading EURO MALPAE document");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
            );

            if (mimeType != null && !mimeType.isEmpty()) {
                request.setMimeType(mimeType);
            }

            // Preserve the logged-in session
            String cookies = CookieManager.getInstance().getCookie(url);

            if (cookies != null && !cookies.isEmpty()) {
                request.addRequestHeader("Cookie", cookies);
            }

            if (userAgent != null && !userAgent.isEmpty()) {
                request.addRequestHeader("User-Agent", userAgent);
            }

            DownloadManager downloadManager =
                    (DownloadManager) getSystemService(
                            Context.DOWNLOAD_SERVICE
                    );

            if (downloadManager == null) {
                Toast.makeText(
                        this,
                        "Download service unavailable",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            downloadManager.enqueue(request);

            Toast.makeText(
                    this,
                    "Downloading: " + fileName,
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Download failed: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // FILE UPLOAD RESULT
    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST
                && fileCallback != null) {

            Uri[] results = null;

            if (resultCode == RESULT_OK && data != null) {

                Uri uri = data.getData();

                if (uri != null) {
                    results = new Uri[]{uri};
                }
            }

            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    // BACK BUTTON SUPPORT
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
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
