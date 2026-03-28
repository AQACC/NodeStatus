package com.aqa.cc.nodestatus;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ProviderSessionConfigStore implements SiteConfigStore {
    static final String PREFS_NAME = "virtfusion_session_config";
    static final String KEY_BASE_URL = "base_url";
    static final String KEY_LOGIN_ENTRY_URL = "login_entry_url";
    static final String LEGACY_KEY_API_BASE_URL = "api_base_url";
    static final String LEGACY_KEY_API_TOKEN = "api_token";
    static final String KEY_COOKIE_HEADER = "cookie_header";
    static final String KEY_XSRF_HEADER = "xsrf_header";
    static final String KEY_USER_AGENT = "user_agent";
    static final String KEY_ALLOW_INSECURE_TLS = "allow_insecure_tls";
    static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    static final String KEY_LOW_TRAFFIC_THRESHOLD_PERCENT = "low_traffic_threshold_percent";
    static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0";

    private final SharedPreferences preferences;
    private final SecureStringPreferencesCodec codec = new SecureStringPreferencesCodec();

    public ProviderSessionConfigStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public ProviderSessionConfig load() {
        MigrationState migrationState = new MigrationState();
        ProviderSessionConfig config = loadConfig(null, migrationState);
        if (migrationState.shouldRewrite()) {
            save(config);
        }
        return config;
    }

    @NonNull
    @Override
    public ProviderSessionConfig load(@NonNull String siteId) {
        MigrationState migrationState = new MigrationState();
        ProviderSessionConfig config = loadConfig(siteId, migrationState);
        if (migrationState.shouldRewrite()) {
            save(siteId, config);
        }
        return config;
    }

    @Override
    public void save(@NonNull ProviderSessionConfig config) {
        persistConfig(null, config);
    }

    @Override
    public void save(@NonNull String siteId, @NonNull ProviderSessionConfig config) {
        persistConfig(siteId, config);
    }

    @Override
    public boolean hasStoredConfig(@Nullable String siteId) {
        return preferences.contains(resolveKey(siteId, KEY_BASE_URL))
                || preferences.contains(resolveKey(siteId, KEY_LOGIN_ENTRY_URL))
                || preferences.contains(resolveKey(siteId, KEY_COOKIE_HEADER))
                || preferences.contains(resolveKey(siteId, KEY_XSRF_HEADER))
                || preferences.contains(resolveKey(siteId, KEY_USER_AGENT))
                || preferences.contains(resolveKey(siteId, KEY_ALLOW_INSECURE_TLS))
                || preferences.contains(resolveKey(siteId, KEY_NOTIFICATIONS_ENABLED))
                || preferences.contains(resolveKey(siteId, KEY_LOW_TRAFFIC_THRESHOLD_PERCENT));
    }

    @NonNull
    private ProviderSessionConfig loadConfig(@Nullable String siteId, @NonNull MigrationState migrationState) {
        return new ProviderSessionConfig(
                loadProtectedString(resolveKey(siteId, KEY_BASE_URL), "", migrationState),
                loadProtectedString(resolveKey(siteId, KEY_LOGIN_ENTRY_URL), "", migrationState),
                loadProtectedString(resolveKey(siteId, KEY_COOKIE_HEADER), "", migrationState),
                loadProtectedString(resolveKey(siteId, KEY_XSRF_HEADER), "", migrationState),
                loadProtectedString(resolveKey(siteId, KEY_USER_AGENT), DEFAULT_USER_AGENT, migrationState),
                preferences.getBoolean(resolveKey(siteId, KEY_ALLOW_INSECURE_TLS), false),
                preferences.getBoolean(resolveKey(siteId, KEY_NOTIFICATIONS_ENABLED), false),
                preferences.getInt(resolveKey(siteId, KEY_LOW_TRAFFIC_THRESHOLD_PERCENT), 20)
        );
    }

    private void persistConfig(@Nullable String siteId, @NonNull ProviderSessionConfig config) {
        preferences.edit()
                .putString(resolveKey(siteId, KEY_BASE_URL), codec.encrypt(config.getBaseUrl()))
                .putString(resolveKey(siteId, KEY_LOGIN_ENTRY_URL), codec.encrypt(config.getLoginEntryUrl()))
                .putString(resolveKey(siteId, KEY_COOKIE_HEADER), codec.encrypt(config.getCookieHeader()))
                .putString(resolveKey(siteId, KEY_XSRF_HEADER), codec.encrypt(config.getXsrfHeader()))
                .putString(resolveKey(siteId, KEY_USER_AGENT), codec.encrypt(config.getUserAgent()))
                .putBoolean(resolveKey(siteId, KEY_ALLOW_INSECURE_TLS), config.isAllowInsecureTls())
                .putBoolean(resolveKey(siteId, KEY_NOTIFICATIONS_ENABLED), config.isNotificationsEnabled())
                .putInt(resolveKey(siteId, KEY_LOW_TRAFFIC_THRESHOLD_PERCENT), config.getLowTrafficThresholdPercent())
                .remove(resolveKey(siteId, LEGACY_KEY_API_BASE_URL))
                .remove(resolveKey(siteId, LEGACY_KEY_API_TOKEN))
                .apply();
    }

    private String loadProtectedString(String key, String fallback, MigrationState migrationState) {
        String storedValue = preferences.getString(key, null);
        if (storedValue == null || storedValue.isBlank()) {
            return fallback;
        }
        if (!codec.isEncryptedValue(storedValue)) {
            migrationState.markForRewrite();
            return storedValue;
        }
        try {
            return codec.decrypt(storedValue);
        } catch (IllegalStateException exception) {
            preferences.edit().remove(key).apply();
            return fallback;
        }
    }

    private String resolveKey(@Nullable String siteId, @NonNull String key) {
        if (siteId == null || siteId.isBlank()) {
            return key;
        }
        return "site." + siteId + "." + key;
    }

    private static final class MigrationState {
        private boolean rewrite;

        void markForRewrite() {
            rewrite = true;
        }

        boolean shouldRewrite() {
            return rewrite;
        }
    }
}
