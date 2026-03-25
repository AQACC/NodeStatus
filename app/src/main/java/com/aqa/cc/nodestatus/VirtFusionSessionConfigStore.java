package com.aqa.cc.nodestatus;

import android.content.Context;
import android.content.SharedPreferences;

public final class VirtFusionSessionConfigStore {
    private static final String PREFS_NAME = "virtfusion_session_config";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_API_BASE_URL = "api_base_url";
    private static final String KEY_API_TOKEN = "api_token";
    private static final String KEY_COOKIE_HEADER = "cookie_header";
    private static final String KEY_XSRF_HEADER = "xsrf_header";
    private static final String KEY_USER_AGENT = "user_agent";
    private static final String KEY_ALLOW_INSECURE_TLS = "allow_insecure_tls";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_LOW_TRAFFIC_THRESHOLD_PERCENT = "low_traffic_threshold_percent";
    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0";

    private final SharedPreferences preferences;

    public VirtFusionSessionConfigStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public VirtFusionSessionConfig load() {
        return new VirtFusionSessionConfig(
                preferences.getString(KEY_BASE_URL, ""),
                preferences.getString(KEY_API_BASE_URL, ""),
                preferences.getString(KEY_API_TOKEN, ""),
                preferences.getString(KEY_COOKIE_HEADER, ""),
                preferences.getString(KEY_XSRF_HEADER, ""),
                preferences.getString(KEY_USER_AGENT, DEFAULT_USER_AGENT),
                preferences.getBoolean(KEY_ALLOW_INSECURE_TLS, false),
                preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, false),
                preferences.getInt(KEY_LOW_TRAFFIC_THRESHOLD_PERCENT, 20)
        );
    }

    public void save(VirtFusionSessionConfig config) {
        preferences.edit()
                .putString(KEY_BASE_URL, config.getBaseUrl())
                .putString(KEY_API_BASE_URL, config.getApiBaseUrl())
                .putString(KEY_API_TOKEN, config.getApiToken())
                .putString(KEY_COOKIE_HEADER, config.getCookieHeader())
                .putString(KEY_XSRF_HEADER, config.getXsrfHeader())
                .putString(KEY_USER_AGENT, config.getUserAgent())
                .putBoolean(KEY_ALLOW_INSECURE_TLS, config.isAllowInsecureTls())
                .putBoolean(KEY_NOTIFICATIONS_ENABLED, config.isNotificationsEnabled())
                .putInt(KEY_LOW_TRAFFIC_THRESHOLD_PERCENT, config.getLowTrafficThresholdPercent())
                .apply();
    }
}
