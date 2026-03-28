package com.aqa.cc.nodestatus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aqa.cc.nodestatus.adapter.virtfusion.VirtFusionLocalSessionAuth;
import com.aqa.cc.nodestatus.adapter.virtfusion.VirtFusionSessionClient;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtFusionWebLoginActivity extends AppCompatActivity {
    public static final String EXTRA_BASE_URL = "base_url";
    public static final String EXTRA_LOGIN_ENTRY_URL = "login_entry_url";
    public static final String EXTRA_USER_AGENT = "user_agent";
    public static final String EXTRA_ALLOW_INSECURE_TLS = "allow_insecure_tls";
    public static final String EXTRA_COOKIE_HEADER = "cookie_header";
    public static final String EXTRA_XSRF_HEADER = "xsrf_header";
    public static final String EXTRA_STATUS_MESSAGE = "status_message";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private TextView statusValue;
    private LinearProgressIndicator progressIndicator;
    private WebView webView;

    private String baseUrl;
    private String loginEntryUrl;
    private String userAgent;
    private boolean allowInsecureTls;
    private boolean importCompleted;
    private boolean verifyingSession;
    private String lastVerificationSignature = "";

    public static Intent createIntent(
            @NonNull Context context,
            @NonNull ProviderSessionConfig config
    ) {
        Intent intent = new Intent(context, VirtFusionWebLoginActivity.class);
        intent.putExtra(EXTRA_BASE_URL, config.getBaseUrl());
        intent.putExtra(EXTRA_LOGIN_ENTRY_URL, config.getLoginEntryUrl());
        intent.putExtra(EXTRA_USER_AGENT, config.getUserAgent());
        intent.putExtra(EXTRA_ALLOW_INSECURE_TLS, config.isAllowInsecureTls());
        return intent;
    }

    @Nullable
    public static String extractCookieHeader(@Nullable Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getStringExtra(EXTRA_COOKIE_HEADER);
    }

    @NonNull
    public static String extractXsrfHeader(@Nullable Intent intent) {
        if (intent == null) {
            return "";
        }
        String xsrfHeader = intent.getStringExtra(EXTRA_XSRF_HEADER);
        return xsrfHeader == null ? "" : xsrfHeader;
    }

    @Nullable
    public static String extractStatusMessage(@Nullable Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getStringExtra(EXTRA_STATUS_MESSAGE);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_virtfusion_web_login);

        baseUrl = readRequiredExtra(EXTRA_BASE_URL);
        loginEntryUrl = readRequiredExtra(EXTRA_LOGIN_ENTRY_URL);
        userAgent = readRequiredExtra(EXTRA_USER_AGENT);
        allowInsecureTls = getIntent().getBooleanExtra(EXTRA_ALLOW_INSECURE_TLS, false);

        if (baseUrl == null || baseUrl.isBlank()) {
            finishWithFailure(getString(R.string.session_import_requires_base_url));
            return;
        }
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = ProviderSessionConfigStore.DEFAULT_USER_AGENT;
        }

        bindViews();
        configureWebView();

        if (savedInstanceState == null) {
            renderStatus(R.string.web_login_status_ready);
            try {
                webView.loadUrl(resolveLaunchUrl());
            } catch (IllegalArgumentException exception) {
                finishWithFailure(getString(R.string.session_import_requires_valid_base_url));
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        executorService.shutdownNow();
        super.onDestroy();
    }

    private void bindViews() {
        findViewById(R.id.webLoginCloseButton).setOnClickListener(view -> finish());
        statusValue = findViewById(R.id.webLoginStatusValue);
        progressIndicator = findViewById(R.id.webLoginProgressIndicator);
        webView = findViewById(R.id.webLoginWebView);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUserAgentString(userAgent);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (importCompleted) {
                    return;
                }
                progressIndicator.setIndeterminate(newProgress < 100 || verifyingSession);
                progressIndicator.setProgressCompat(Math.max(newProgress, 5), true);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (importCompleted) {
                    return;
                }
                progressIndicator.show();
                progressIndicator.setIndeterminate(true);
                renderStatus(R.string.web_login_status_loading);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (importCompleted) {
                    return;
                }
                maybeImportSession(url);
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    @NonNull WebResourceRequest request,
                    @NonNull WebResourceError error
            ) {
                if (!request.isForMainFrame() || importCompleted) {
                    return;
                }
                progressIndicator.hide();
                renderStatus(getString(R.string.web_login_status_load_failed, error.getDescription()));
            }

            @Override
            public void onReceivedSslError(
                    WebView view,
                    @NonNull SslErrorHandler handler,
                    @NonNull SslError error
            ) {
                if (BuildConfig.DEBUG && allowInsecureTls) {
                    handler.proceed();
                    renderStatus(R.string.web_login_status_tls_bypassed);
                    return;
                }
                handler.cancel();
                progressIndicator.hide();
                renderStatus(R.string.web_login_status_tls_failed);
            }
        });
    }

    private void maybeImportSession(@NonNull String currentUrl) {
        if (verifyingSession || importCompleted) {
            return;
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.flush();
        String cookieHeader = firstNonBlank(
                cookieManager.getCookie(currentUrl),
                cookieManager.getCookie(baseUrl),
                cookieManager.getCookie(loginEntryUrl)
        );
        if (cookieHeader == null || !SessionCookieParser.containsCookie(cookieHeader, "virtfusion_session")) {
            progressIndicator.hide();
            renderStatus(R.string.web_login_status_waiting);
            return;
        }

        String signature = currentUrl + "\n" + cookieHeader;
        if (signature.equals(lastVerificationSignature)) {
            progressIndicator.hide();
            return;
        }
        lastVerificationSignature = signature;

        verifyImportedSession(cookieHeader);
    }

    private void verifyImportedSession(@NonNull String cookieHeader) {
        verifyingSession = true;
        progressIndicator.show();
        progressIndicator.setIndeterminate(true);
        renderStatus(R.string.web_login_status_verifying);
        executorService.execute(() -> {
            String xsrfHeader = SessionCookieParser.decodeCookieValue(
                    SessionCookieParser.findCookieValue(cookieHeader, "XSRF-TOKEN")
            );
            Throwable failure = null;
            try {
                VirtFusionLocalSessionAuth auth = new VirtFusionLocalSessionAuth(
                        baseUrl,
                        cookieHeader,
                        xsrfHeader,
                        userAgent,
                        allowInsecureTls
                );
                VirtFusionSessionClient.create(auth).fetchServerList(1);
            } catch (Throwable throwable) {
                failure = throwable;
            }

            String finalXsrfHeader = xsrfHeader;
            Throwable finalFailure = failure;
            mainHandler.post(() -> onVerificationFinished(cookieHeader, finalXsrfHeader, finalFailure));
        });
    }

    private void onVerificationFinished(
            @NonNull String cookieHeader,
            @NonNull String xsrfHeader,
            @Nullable Throwable failure
    ) {
        verifyingSession = false;
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (failure == null) {
            Intent result = new Intent()
                    .putExtra(EXTRA_COOKIE_HEADER, cookieHeader)
                    .putExtra(EXTRA_XSRF_HEADER, xsrfHeader);
            setResult(Activity.RESULT_OK, result);
            importCompleted = true;
            finish();
            return;
        }

        progressIndicator.hide();
        renderStatus(R.string.web_login_status_waiting);
    }

    private void finishWithFailure(@NonNull String statusMessage) {
        setResult(Activity.RESULT_CANCELED, new Intent().putExtra(EXTRA_STATUS_MESSAGE, statusMessage));
        finish();
    }

    @Nullable
    private String readRequiredExtra(@NonNull String key) {
        return getIntent().getStringExtra(key);
    }

    @NonNull
    private String resolveLaunchUrl() {
        if (loginEntryUrl != null && !loginEntryUrl.isBlank()) {
            return loginEntryUrl;
        }
        return resolveLoginUrl(baseUrl);
    }

    @NonNull
    private String resolveLoginUrl(@NonNull String inputBaseUrl) {
        URI baseUri = URI.create(inputBaseUrl.endsWith("/") ? inputBaseUrl : inputBaseUrl + "/");
        return baseUri.resolve("login").toString();
    }

    private void renderStatus(int messageResId) {
        renderStatus(getString(messageResId));
    }

    private void renderStatus(@NonNull String message) {
        statusValue.setText(message);
    }

    @Nullable
    private String firstNonBlank(@Nullable String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
