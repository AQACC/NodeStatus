package com.aqa.cc.nodestatus;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public final class NodeStatusRefreshStatusStore {
    private static final String PREFS_NAME = "nodestatus_refresh_status";
    private static final String KEY_LAST_MESSAGE = "last_message";
    private static final String KEY_LAST_UPDATED_AT = "last_updated_at";
    private static final String KEY_LAST_SOURCE = "last_source";
    private static final String KEY_LAST_SUCCESS = "last_success";
    private static final String KEY_LAST_PENDING = "last_pending";
    private static final String KEY_LAST_USED_TOKEN = "last_used_token";
    private static final String KEY_LAST_USED_SESSION = "last_used_session";

    private final SharedPreferences preferences;

    public NodeStatusRefreshStatusStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void savePending(String source, String message, String updatedAt) {
        preferences.edit()
                .putString(KEY_LAST_SOURCE, source)
                .putString(KEY_LAST_MESSAGE, message)
                .putString(KEY_LAST_UPDATED_AT, updatedAt)
                .putBoolean(KEY_LAST_PENDING, true)
                .apply();
    }

    public void saveSuccess(
            String source,
            String message,
            String updatedAt,
            boolean usedToken,
            boolean usedSession
    ) {
        preferences.edit()
                .putString(KEY_LAST_SOURCE, source)
                .putString(KEY_LAST_MESSAGE, message)
                .putString(KEY_LAST_UPDATED_AT, updatedAt)
                .putBoolean(KEY_LAST_SUCCESS, true)
                .putBoolean(KEY_LAST_PENDING, false)
                .putBoolean(KEY_LAST_USED_TOKEN, usedToken)
                .putBoolean(KEY_LAST_USED_SESSION, usedSession)
                .apply();
    }

    public void saveFailure(String source, String message, String updatedAt) {
        preferences.edit()
                .putString(KEY_LAST_SOURCE, source)
                .putString(KEY_LAST_MESSAGE, message)
                .putString(KEY_LAST_UPDATED_AT, updatedAt)
                .putBoolean(KEY_LAST_SUCCESS, false)
                .putBoolean(KEY_LAST_PENDING, false)
                .apply();
    }

    @Nullable
    public RefreshStatus load() {
        String source = preferences.getString(KEY_LAST_SOURCE, null);
        String message = preferences.getString(KEY_LAST_MESSAGE, null);
        String updatedAt = preferences.getString(KEY_LAST_UPDATED_AT, null);
        if (source == null || message == null || updatedAt == null) {
            return null;
        }
        return new RefreshStatus(
                source,
                message,
                updatedAt,
                preferences.getBoolean(KEY_LAST_SUCCESS, false),
                preferences.getBoolean(KEY_LAST_PENDING, false),
                preferences.getBoolean(KEY_LAST_USED_TOKEN, false),
                preferences.getBoolean(KEY_LAST_USED_SESSION, false)
        );
    }

    public static final class RefreshStatus {
        private final String source;
        private final String message;
        private final String updatedAt;
        private final boolean success;
        private final boolean pending;
        private final boolean usedToken;
        private final boolean usedCompatibilitySession;

        public RefreshStatus(
                String source,
                String message,
                String updatedAt,
                boolean success,
                boolean pending,
                boolean usedToken,
                boolean usedCompatibilitySession
        ) {
            this.source = source;
            this.message = message;
            this.updatedAt = updatedAt;
            this.success = success;
            this.pending = pending;
            this.usedToken = usedToken;
            this.usedCompatibilitySession = usedCompatibilitySession;
        }

        public String getSource() {
            return source;
        }

        public String getMessage() {
            return message;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isPending() {
            return pending;
        }

        public boolean isUsedToken() {
            return usedToken;
        }

        public boolean isUsedCompatibilitySession() {
            return usedCompatibilitySession;
        }
    }
}
