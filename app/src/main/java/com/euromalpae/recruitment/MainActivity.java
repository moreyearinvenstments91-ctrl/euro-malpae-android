
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

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                return handleUrl(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    String url) {

                return handleUrl(Uri.parse(url));
            }
        });

        // File upload support
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

        // PDF and ZIP downloads
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

        webView.loadUrl(WEBSITE_URL);
    }

    // Handle internal and external links
    private boolean handleUrl(Uri uri) {

        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (scheme == null) {
            return false;
        }

        // Keep EURO MALPAE website inside the APK
        if (("https".equalsIgnoreCase(scheme) ||
             "http".equalsIgnoreCase(scheme))
                && host != null
                && host.equalsIgnoreCase(
                    "maple-ncubez-recruitment.onrender.com")) {

            return false;
        }

        // WhatsApp ordinary and WhatsApp Business
        if ("https".equalsIgnoreCase(scheme)
                && host != null
                && (host.equalsIgnoreCase("wa.me")
                || host.equalsIgnoreCase("www.wa.me")
                || host.equalsIgnoreCase("api.whatsapp.com")
                || host.equalsIgnoreCase("www.whatsapp.com"))) {

            openWhatsApp(uri);
            return true;
        }

        // Other external links
        if ("https".equalsIgnoreCase(scheme)
                || "http".equalsIgnoreCase(scheme)
                || "mailto".equalsIgnoreCase(scheme)
                || "tel".equalsIgnoreCase(scheme)) {

            openExternal(uri);
            return true;
        }

        return false;
    }

    // Let Android choose WhatsApp or WhatsApp Business
    private void openWhatsApp(Uri uri) {

        try {

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    uri
            );

            startActivity(
                    Intent.createChooser(
                            intent,
                            "Open with WhatsApp"
                    )
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to open WhatsApp. Check that WhatsApp is installed.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void openExternal(Uri uri) {

        try {

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    uri
            );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "No application available to open this link.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // Download files
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
                    DownloadManager.Request
                    .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
            );

            if (mimeType != null && !mimeType.isEmpty()) {
                request.setMimeType(mimeType);
            }

            String cookie =
                    CookieManager.getInstance().getCookie(url);

            if (cookie != null) {
                request.addRequestHeader("Cookie", cookie);
            }

            if (userAgent != null) {
                request.addRequestHeader("User-Agent", userAgent);
            }

            DownloadManager manager =
                    (DownloadManager) getSystemService(
                            Context.DOWNLOAD_SERVICE
                    );

            if (manager == null) {
                throw new Exception("Download service unavailable");
            }

            manager.enqueue(request);

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

    // File upload result
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

        if (requestCode == FILE_CHOOSER_REQUEST
                && fileCallback != null) {

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

    // Back navigation
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
