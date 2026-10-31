
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
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    private static final int FILE_CHOOSER_REQUEST = 100;

    private static final String WEBSITE_URL =
            "https://maple-ncubez-recruitment.onrender.com";

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
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // Keep EURO MALPAE website inside the application.
        // Open WhatsApp and other external links in their respective apps.
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                return handleUrl(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    String url) {

                return handleUrl(url);
            }
        });

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

                    if (params.getMode() ==
                            FileChooserParams.MODE_OPEN_MULTIPLE) {

                        intent.putExtra(
                                Intent.EXTRA_ALLOW_MULTIPLE,
                                true
                        );
                    }

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

        // Always load the live Render website.
        webView.loadUrl(WEBSITE_URL);
    }

    // HANDLE WHATSAPP AND EXTERNAL LINKS
    private boolean handleUrl(String url) {

        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();

        if (scheme == null) {
            return false;
        }

        if ("https".equalsIgnoreCase(scheme) ||
                "http".equalsIgnoreCase(scheme)) {

            String host = uri.getHost();

            if (host != null &&
                    (host.equalsIgnoreCase("maple-ncubez-recruitment.onrender.com") ||
                     host.equalsIgnoreCase("www.maple-ncubez-recruitment.onrender.com"))) {

                return false;
            }

            openExternalLink(uri);
            return true;
        }

        if ("mailto".equalsIgnoreCase(scheme) ||
                "tel".equalsIgnoreCase(scheme) ||
                "intent".equalsIgnoreCase(scheme)) {

            openExternalLink(uri);
            return true;
        }

        return false;
    }

    private void openExternalLink(Uri uri) {

        try {

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    uri
            );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "No application available to open this link",
                    Toast.LENGTH_SHORT
            ).show();
        }
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
            request.setDescription(
                    "Downloading EURO MALPAE document"
            );

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

            String cookies =
                    CookieManager.getInstance().getCookie(url);

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

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == FILE_CHOOSER_REQUEST &&
                fileCallback != null) {

            Uri[] results = null;

            if (resultCode == RESULT_OK && data != null) {

                if (data.getClipData() != null) {

                    int count = data.getClipData().getItemCount();

                    results = new Uri[count];

                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData()
                                .getItemAt(i).getUri();
                    }

                } else if (data.getData() != null) {

                    results = new Uri[]{
                            data.getData()
                    };
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
