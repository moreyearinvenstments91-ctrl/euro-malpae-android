
package com.euromalpae.recruitment;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.PermissionRequest;
import android.widget.Toast;
import android.graphics.Bitmap;

public class MainActivity extends Activity {

    private static final String HOME_URL =
        "https://maple-ncubez-recruitment.onrender.com";

    private static final int FILE_CHOOSER_REQUEST = 7001;
    private static final int CAMERA_PERMISSION_REQUEST = 7002;

    private WebView webView;

    private ValueCallback<Uri[]> fileCallback;

    private PermissionRequest pendingPermissionRequest;

    private boolean activityDestroyed = false;

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

        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        settings.setMixedContentMode(
            WebSettings.MIXED_CONTENT_NEVER_ALLOW
        );

        CookieManager.getInstance().setAcceptCookie(true);

        CookieManager.getInstance().setAcceptThirdPartyCookies(
            webView, false
        );

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                WebView view,
                WebResourceRequest request
            ) {
                return handleUrl(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                WebView view,
                String url
            ) {
                return handleUrl(Uri.parse(url));
            }

            @Override
            public void onPageStarted(
                WebView view,
                String url,
                Bitmap favicon
            ) {
                super.onPageStarted(view, url, favicon);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            // HANDLE PDF AND DOCUMENT UPLOADS
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
                    Intent intent = params.createIntent();

                    startActivityForResult(
                        intent,
                        FILE_CHOOSER_REQUEST
                    );

                    return true;

                } catch (ActivityNotFoundException e) {

                    fileCallback = null;

                    Toast.makeText(
                        MainActivity.this,
                        "No file picker is available.",
                        Toast.LENGTH_LONG
                    ).show();

                    callback.onReceiveValue(null);

                    return true;

                } catch (Exception e) {

                    fileCallback = null;
                    callback.onReceiveValue(null);

                    return true;
                }
            }

            // HANDLE CAMERA PERMISSIONS INSIDE WEBVIEW
            @Override
            public void onPermissionRequest(
                PermissionRequest request
            ) {

                runOnUiThread(() -> {

                    if (activityDestroyed) {
                        request.deny();
                        return;
                    }

                    Uri origin = request.getOrigin();

                    if (!isTrustedOrigin(origin)) {
                        request.deny();
                        return;
                    }

                    boolean cameraRequested = false;

                    for (String resource : request.getResources()) {

                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE
                                .equals(resource)) {

                            cameraRequested = true;
                        }
                    }

                    if (!cameraRequested) {
                        request.deny();
                        return;
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            && checkSelfPermission(
                                Manifest.permission.CAMERA
                            ) != PackageManager.PERMISSION_GRANTED) {

                        if (pendingPermissionRequest != null) {
                            pendingPermissionRequest.deny();
                        }

                        pendingPermissionRequest = request;

                        requestPermissions(
                            new String[]{
                                Manifest.permission.CAMERA
                            },
                            CAMERA_PERMISSION_REQUEST
                        );

                    } else {

                        grantCameraPermission(request);
                    }
                });
            }

            // HANDLE CAMERA PERMISSION CANCELLATION
            @Override
            public void onPermissionRequestCanceled(
                PermissionRequest request
            ) {

                if (pendingPermissionRequest == request) {
                    pendingPermissionRequest = null;
                }
            }
        });

        // HANDLE DOWNLOADS
        webView.setDownloadListener(
            new DownloadListener() {

                @Override
                public void onDownloadStart(
                    String url,
                    String userAgent,
                    String contentDisposition,
                    String mimetype,
                    long contentLength
                ) {

                    try {

                        DownloadManager.Request request =
                            new DownloadManager.Request(
                                Uri.parse(url)
                            );

                        request.setMimeType(mimetype);

                        String cookie =
                            CookieManager.getInstance().getCookie(url);

                        if (cookie != null) {
                            request.addRequestHeader(
                                "Cookie",
                                cookie
                            );
                        }

                        request.setNotificationVisibility(
                            DownloadManager.Request
                                .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                        );

                        request.setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            DownloadManager.Request.guessFileName(
                                url,
                                contentDisposition,
                                mimetype
                            )
                        );

                        request.setTitle("EURO MALPAE download");

                        DownloadManager manager =
                            (DownloadManager) getSystemService(
                                DOWNLOAD_SERVICE
                            );

                        manager.enqueue(request);

                        Toast.makeText(
                            MainActivity.this,
                            "Download started.",
                            Toast.LENGTH_SHORT
                        ).show();

                    } catch (Exception e) {

                        Toast.makeText(
                            MainActivity.this,
                            "Unable to download this file.",
                            Toast.LENGTH_LONG
                        ).show();
                    }
                }
            }
        );

        if (savedInstanceState != null) {

            webView.restoreState(savedInstanceState);

        } else {

            webView.loadUrl(HOME_URL);
        }
    }

    // ONLY TRUST THE EURO MALPAE WEBSITE
    private boolean isTrustedOrigin(Uri uri) {

        if (uri == null) {
            return false;
        }

        String scheme = uri.getScheme() == null
            ? ""
            : uri.getScheme().toLowerCase();

        String host = uri.getHost() == null
            ? ""
            : uri.getHost().toLowerCase();

        if (!"https".equals(scheme)) {
            return false;
        }

        return host.equals("maple-ncubez-recruitment.onrender.com")
            || host.equals("www.euromalpae.com")
            || host.equals("euromalpae.com");
    }

    // GRANT CAMERA ACCESS TO THE WEBVIEW
    private void grantCameraPermission(
        PermissionRequest request
    ) {

        if (!isTrustedOrigin(request.getOrigin())) {
            request.deny();
            return;
        }

        request.grant(
            new String[]{
                PermissionRequest.RESOURCE_VIDEO_CAPTURE
            }
        );
    }

    // HANDLE ANDROID CAMERA PERMISSION RESULT
    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        );

        if (requestCode == CAMERA_PERMISSION_REQUEST) {

            PermissionRequest request = pendingPermissionRequest;

            pendingPermissionRequest = null;

            if (request == null) {
                return;
            }

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                grantCameraPermission(request);

                Toast.makeText(
                    this,
                    "Camera permission granted.",
                    Toast.LENGTH_SHORT
                ).show();

            } else {

                request.deny();

                Toast.makeText(
                    this,
                    "Camera permission denied. Allow camera access in app settings.",
                    Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // HANDLE WEBSITE LINKS
    private boolean handleUrl(Uri uri) {

        String scheme = uri.getScheme() == null
            ? ""
            : uri.getScheme().toLowerCase();

        String host = uri.getHost() == null
            ? ""
            : uri.getHost().toLowerCase();

        if (scheme.equals("https") &&
                (host.equals("maple-ncubez-recruitment.onrender.com")
                || host.equals("www.euromalpae.com")
                || host.equals("euromalpae.com"))) {

            return false;
        }

        if (scheme.equals("http") || scheme.equals("https")) {

            try {

                startActivity(
                    new Intent(Intent.ACTION_VIEW, uri)
                );

            } catch (ActivityNotFoundException ignored) {}

            return true;
        }

        if (scheme.equals("mailto")
                || scheme.equals("tel")
                || scheme.equals("sms")
                || scheme.equals("whatsapp")
                || scheme.equals("intent")) {

            try {

                Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    uri
                );

                startActivity(intent);

            } catch (Exception e) {

                Toast.makeText(
                    this,
                    "No compatible app was found.",
                    Toast.LENGTH_LONG
                ).show();
            }

            return true;
        }

        return true;
    }

    // HANDLE FILE PICKER RESULT
    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        );

        if (requestCode == FILE_CHOOSER_REQUEST
                && fileCallback != null) {

            Uri[] result =
                WebChromeClient.FileChooserParams.parseResult(
                    resultCode,
                    data
                );

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

        activityDestroyed = true;

        if (pendingPermissionRequest != null) {
            pendingPermissionRequest.deny();
            pendingPermissionRequest = null;
        }

        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }

        if (webView != null) {

            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
